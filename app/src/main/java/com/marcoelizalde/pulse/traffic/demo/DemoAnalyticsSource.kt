package com.marcoelizalde.pulse.traffic.demo

import com.marcoelizalde.pulse.traffic.domain.AnalyticsSource
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.Product
import com.marcoelizalde.pulse.traffic.domain.ProductTraffic
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import com.marcoelizalde.pulse.traffic.domain.Sales
import com.marcoelizalde.pulse.traffic.domain.TrafficPoint
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.random.Random

/** Fictional products used when no DataFast token is configured. */
class DemoAnalyticsSource(private val clock: Clock) : AnalyticsSource {
    private val dailyVisitors = linkedMapOf(
        Product("demo-northwind", "Northwind", "northwind.app") to 1840,
        Product("demo-lumen", "Lumen Notes", "lumen.so") to 960,
        Product("demo-orbit", "Orbit CRM", "orbitcrm.io") to 610,
        Product("demo-pixel", "Pixel Forge", "pixelforge.dev") to 330,
        Product("demo-atlas", "Atlas Docs", "atlasdocs.com") to 140,
    )

    /** Price in cents and share of visitors who buy; Atlas Docs is free. */
    private val pricing = mapOf(
        "demo-northwind" to (2_900L to 0.004),
        "demo-lumen" to (900L to 0.008),
        "demo-orbit" to (4_900L to 0.003),
        "demo-pixel" to (1_900L to 0.002),
    )

    override suspend fun products(): List<Product> = dailyVisitors.keys.toList()

    override suspend fun traffic(product: Product, window: ReportWindow): ProductTraffic {
        val daily = dailyVisitors.getValue(product)
        val (priceCents, conversion) = pricing[product.id] ?: (0L to 0.0)
        val buckets = buckets(window).map { start ->
            val visitors = visitorsIn(product, start, window.granularity, daily)
            Triple(start, visitors, paymentsIn(product, start, visitors, conversion))
        }
        // Unique visitors over a window are fewer than the sum of its buckets.
        val uniqueRatio = if (window.granularity == Granularity.HOUR) 0.92 else 0.8
        val payments = buckets.sumOf { (_, _, payments) -> payments }
        return ProductTraffic(
            visitors = (buckets.sumOf { (_, visitors, _) -> visitors } * uniqueRatio).roundToLong(),
            trend = buckets.map { (start, visitors, payments) -> TrafficPoint(start, visitors, payments * priceCents) },
            sales = Sales(cents = payments * priceCents, payments = payments, currency = "$"),
        )
    }

    /** Whole payments; the fraction of one becomes a sale with that probability, stable per bucket. */
    private fun paymentsIn(product: Product, start: LocalDateTime, visitors: Long, conversion: Double): Long {
        val expected = visitors * conversion
        val extra = if (Random((product.id + start + "sale").hashCode()).nextDouble() < expected % 1) 1 else 0
        return expected.toLong() + extra
    }

    private fun buckets(window: ReportWindow): List<LocalDateTime> = when (window.granularity) {
        Granularity.HOUR -> {
            val now = LocalDateTime.now(clock.withZone(window.zone))
            val lastHour = if (window.end.isBefore(now.toLocalDate())) 23 else now.hour
            (0..lastHour).map { window.start.atTime(it, 0) }
        }
        Granularity.DAY -> days(window.start, window.end, step = 1)
        Granularity.WEEK -> days(window.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), window.end, step = 7)
    }

    private fun days(from: LocalDate, to: LocalDate, step: Long): List<LocalDateTime> =
        generateSequence(from) { it.plusDays(step) }
            .takeWhile { !it.isAfter(to) }
            .map { it.atStartOfDay() }
            .toList()

    private fun visitorsIn(product: Product, start: LocalDateTime, granularity: Granularity, daily: Int): Long {
        val noise = Random((product.id + start).hashCode()).nextDouble(0.8, 1.2)
        val weekday = if (start.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) 0.72 else 1.0
        val volume = when (granularity) {
            // Traffic peaks mid-afternoon and bottoms out before dawn.
            Granularity.HOUR -> daily / 24.0 * (1 - 0.75 * cos((start.hour - 2) * 2 * PI / 24))
            Granularity.DAY -> daily * weekday
            Granularity.WEEK -> daily * 6.4
        }
        return (volume * noise).roundToLong().coerceAtLeast(1)
    }
}
