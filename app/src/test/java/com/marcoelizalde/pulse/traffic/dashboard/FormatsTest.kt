package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.dashboard.i18n.EnglishStrings
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class FormatsTest {
    private val formats = Formats(EnglishStrings)
    private val zone = ZoneId.of("America/Mexico_City")

    @Test
    fun `grouped numbers use thousands separators`() {
        assertEquals(listOf("0", "999", "12,450", "1,234,567"), listOf(0L, 999L, 12_450L, 1_234_567L).map(formats::grouped))
    }

    @Test
    fun `money shows cents only when there are some`() {
        assertEquals(
            listOf("$0", "$139", "$8.62", "$6.40", "$1,234.50", "-$5", "MXN 120"),
            listOf(
                formats.money(0, "$"),
                formats.money(13_900, "$"),
                formats.money(862, "$"),
                formats.money(640, "$"),
                formats.money(123_450, "$"),
                formats.money(-500, "$"),
                formats.money(12_000, "MXN"),
            ),
        )
    }

    @Test
    fun `money without a known currency uses dollars`() {
        assertEquals("$25", formats.money(2_500, null))
    }

    @Test
    fun `money axis labels are compact`() {
        assertEquals(
            listOf("$0", "$15", "$1.5K", "MXN 2K"),
            listOf(formats.moneyCompact(0, "$"), formats.moneyCompact(15, "$"), formats.moneyCompact(1_500, "$"), formats.moneyCompact(2_000, "MXN")),
        )
    }

    @Test
    fun `aligned ticks share the other axis's gridlines and cover the maximum`() {
        assertEquals(listOf(0L, 5L, 10L, 15L), Formats.alignedTicks(max = 9, steps = 3))
        assertEquals(listOf(0L, 50L, 100L, 150L, 200L), Formats.alignedTicks(max = 139, steps = 4))
        assertEquals(listOf(0L, 1L, 2L, 3L), Formats.alignedTicks(max = 0, steps = 3))
    }

    @Test
    fun `payments are counted in words`() {
        assertEquals(listOf("0 payments", "1 payment", "1,240 payments"), listOf(0L, 1L, 1_240L).map(formats::payments))
    }

    @Test
    fun `compact numbers shorten thousands and millions`() {
        assertEquals(
            listOf("0", "999", "1K", "1.3K", "12.9K", "1M", "4.2M"),
            listOf(0L, 999L, 1_000L, 1_284L, 12_900L, 999_960L, 4_200_000L).map(formats::compact),
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
        assertEquals(listOf("0%", "<1%", "42%", "100%"), listOf(0.0, 0.004, 0.4231, 1.0).map(formats::share))
    }

    @Test
    fun `range reads naturally for one day, a few days and across years`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("Fri, Oct 23", formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("Oct 17 – Oct 23", formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals(
            "Dec 25, 2025 – Jan 23, 2026",
            formats.range(window(LocalDate.of(2025, 12, 25), LocalDate.of(2026, 1, 23))),
        )
    }

    @Test
    fun `buckets are labelled by granularity`() {
        val start = LocalDateTime.of(2026, 10, 21, 14, 0)

        assertEquals(
            listOf("14:00", "Oct 21", "Oct 21"),
            Granularity.entries.map { formats.bucketTick(start, it) },
        )
        assertEquals(
            listOf("14:00 – 15:00", "Wed, Oct 21", "Week of Oct 21"),
            Granularity.entries.map { formats.bucketDetail(start, it) },
        )
    }

    @Test
    fun `ranking header says which rows are on screen when not all fit`() {
        assertEquals("10 tracked", formats.rankingRange(firstVisible = 0, visibleRows = 10, total = 10))
        assertEquals("5 tracked", formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 5))
        assertEquals("1 tracked", formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 1))
        assertEquals("1–6 of 10", formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 10))
        assertEquals("5–10 of 10", formats.rankingRange(firstVisible = 4, visibleRows = 6, total = 10))
    }
}
