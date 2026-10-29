package com.marcoelizalde.pulse.traffic.demo

import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.TimeSpan
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DemoAnalyticsSourceTest {
    private val zone = ZoneId.of("America/Mexico_City")
    private val clock = Clock.fixed(Instant.parse("2026-10-23T20:30:00Z"), zone) // 14:30 local
    private val today = LocalDate.of(2026, 10, 23)
    private val source = DemoAnalyticsSource(clock)

    @Test
    fun `today has one bucket per elapsed hour`() = runTest {
        val product = source.products().first()

        val trend = source.traffic(product, Period.TODAY.windowEndingOn(today, zone)).trend

        assertEquals((0..14).map { today.atTime(it, 0) }, trend.map { it.start })
    }

    @Test
    fun `last 7 days has one bucket per day and plausible numbers`() = runTest {
        val product = source.products().first()

        val traffic = source.traffic(product, Period.LAST_7_DAYS.windowEndingOn(today, zone))

        assertEquals((17..23).map { LocalDateTime.of(2026, 10, it, 0, 0) }, traffic.trend.map { it.start })
        assertTrue(traffic.trend.all { it.visitors > 0 })
        assertTrue(traffic.visitors in 1..traffic.trend.sumOf { it.visitors })
    }

    @Test
    fun `demo products sell in dollars`() = runTest {
        val window = Period.LAST_30_DAYS.windowEndingOn(today, zone)

        val sales = source.products().map { source.traffic(it, window).sales }

        assertTrue(sales.any { it.cents > 0 && it.payments > 0 })
        assertTrue(sales.all { it.currency == "$" })
    }

    @Test
    fun `demo sales per bucket add up to the product's sales`() = runTest {
        val window = Period.LAST_30_DAYS.windowEndingOn(today, zone)

        source.products().forEach { product ->
            val traffic = source.traffic(product, window)
            assertEquals(traffic.sales.cents, traffic.trend.sumOf { it.salesCents })
        }
    }

    @Test
    fun `numbers are stable between refreshes`() = runTest {
        val product = source.products().first()
        val window = Period.LAST_30_DAYS.windowEndingOn(today, zone)

        assertEquals(source.traffic(product, window), source.traffic(product, window))
    }

    @Test
    fun `previous visitors are stable and grow with the span`() = runTest {
        val product = source.products().first()
        val start = LocalDateTime.of(2026, 10, 10, 0, 0)
        fun span(hours: Long) = TimeSpan(start, start.plusHours(hours), zone)

        val day = source.visitors(product, span(24))

        assertTrue(day > 0)
        assertEquals(day, source.visitors(product, span(24)))
        assertTrue(source.visitors(product, span(24 * 7)) > day)
    }
}
