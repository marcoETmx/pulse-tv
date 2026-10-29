package com.marcoelizalde.pulse.traffic.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PeriodTest {
    private val zone = ZoneId.of("America/Mexico_City")
    private val today = LocalDate.of(2026, 10, 23)

    @Test
    fun `today covers only the current day in hourly buckets`() {
        assertEquals(
            ReportWindow(today, today, zone, Granularity.HOUR),
            Period.TODAY.windowEndingOn(today, zone),
        )
    }

    @Test
    fun `last 7 days includes today and the 6 previous days`() {
        assertEquals(
            ReportWindow(LocalDate.of(2026, 10, 17), today, zone, Granularity.DAY),
            Period.LAST_7_DAYS.windowEndingOn(today, zone),
        )
    }

    @Test
    fun `last 15 days includes today and the 14 previous days`() {
        assertEquals(
            ReportWindow(LocalDate.of(2026, 10, 9), today, zone, Granularity.DAY),
            Period.LAST_15_DAYS.windowEndingOn(today, zone),
        )
    }

    @Test
    fun `last 30 days includes today and the 29 previous days`() {
        assertEquals(
            ReportWindow(LocalDate.of(2026, 9, 24), today, zone, Granularity.DAY),
            Period.LAST_30_DAYS.windowEndingOn(today, zone),
        )
    }

    @Test
    fun `this year runs from January 1 to today in weekly buckets`() {
        assertEquals(
            ReportWindow(LocalDate.of(2026, 1, 1), today, zone, Granularity.WEEK),
            Period.THIS_YEAR.windowEndingOn(today, zone),
        )
    }

    @Test
    fun `this year early in January uses daily buckets`() {
        val january3 = LocalDate.of(2026, 1, 3)
        assertEquals(
            ReportWindow(LocalDate.of(2026, 1, 1), january3, zone, Granularity.DAY),
            Period.THIS_YEAR.windowEndingOn(january3, zone),
        )
    }

    @Test
    fun `the previous span is the same stretch one period earlier, up to the same time`() {
        val now = LocalDateTime.of(2026, 10, 23, 12, 30)
        fun previous(period: Period) = period.previousSpan(period.windowEndingOn(today, zone), now)
        fun span(start: LocalDate, end: LocalDate) = TimeSpan(start.atStartOfDay(), end.atTime(12, 30), zone)

        assertEquals(span(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 22)), previous(Period.TODAY))
        assertEquals(span(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16)), previous(Period.LAST_7_DAYS))
        assertEquals(span(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 10, 8)), previous(Period.LAST_15_DAYS))
        assertEquals(span(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 9, 23)), previous(Period.LAST_30_DAYS))
        assertEquals(span(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 10, 23)), previous(Period.THIS_YEAR))
    }
}
