package com.marcoelizalde.pulse.traffic.demo

import com.marcoelizalde.pulse.traffic.domain.Period
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
    fun `numbers are stable between refreshes`() = runTest {
        val product = source.products().first()
        val window = Period.LAST_30_DAYS.windowEndingOn(today, zone)

        assertEquals(source.traffic(product, window), source.traffic(product, window))
    }
}
