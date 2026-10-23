package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class FormatsTest {
    private val zone = ZoneId.of("America/Mexico_City")

    @Test
    fun `grouped numbers use thousands separators`() {
        assertEquals(listOf("0", "999", "12,450", "1,234,567"), listOf(0L, 999L, 12_450L, 1_234_567L).map(Formats::grouped))
    }

    @Test
    fun `compact numbers shorten thousands and millions`() {
        assertEquals(
            listOf("0", "999", "1K", "1.3K", "12.9K", "1M", "4.2M"),
            listOf(0L, 999L, 1_000L, 1_284L, 12_900L, 999_960L, 4_200_000L).map(Formats::compact),
        )
    }

    @Test
    fun `axis ticks are clean steps from zero`() {
        assertEquals(listOf(0L, 500L, 1_000L, 1_500L), Formats.axisTicks(1_234))
        assertEquals(listOf(0L, 500L, 1_000L, 1_500L, 2_000L), Formats.axisTicks(1_840))
        assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), Formats.axisTicks(2_100))
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), Formats.axisTicks(0))
    }

    @Test
    fun `share is a whole percentage and tiny shares stay visible`() {
        assertEquals(listOf("0%", "<1%", "42%", "100%"), listOf(0.0, 0.004, 0.4231, 1.0).map(Formats::share))
    }

    @Test
    fun `periods have short selector labels`() {
        assertEquals(
            listOf("Today", "7 days", "15 days", "30 days", "This year"),
            Period.entries.map(Formats::period),
        )
    }

    @Test
    fun `range reads naturally for one day, a few days and across years`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("Fri, Oct 23", Formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("Oct 17 – Oct 23", Formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals(
            "Dec 25, 2025 – Jan 23, 2026",
            Formats.range(window(LocalDate.of(2025, 12, 25), LocalDate.of(2026, 1, 23))),
        )
    }

    @Test
    fun `buckets are labelled by granularity`() {
        val start = LocalDateTime.of(2026, 10, 21, 14, 0)

        assertEquals(
            listOf("14:00", "Oct 21", "Oct 21"),
            Granularity.entries.map { Formats.bucketTick(start, it) },
        )
        assertEquals(
            listOf("14:00 – 15:00", "Wed, Oct 21", "Week of Oct 21"),
            Granularity.entries.map { Formats.bucketDetail(start, it) },
        )
        assertEquals(
            listOf("Visitors per hour", "Visitors per day", "Visitors per week"),
            Granularity.entries.map(Formats::trendTitle),
        )
    }
}
