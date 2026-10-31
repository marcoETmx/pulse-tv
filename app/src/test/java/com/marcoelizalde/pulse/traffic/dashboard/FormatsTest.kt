package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.dashboard.i18n.EnglishStrings
import com.marcoelizalde.pulse.traffic.dashboard.i18n.PulseStrings
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

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

    @Test
    fun `ranking header adds how to move through the rows, and leaves it out when the line is too wide`() {
        fun header(total: Int, browsing: Boolean, maxLength: Int = 100) =
            formats.rankingHeader(firstVisible = 0, visibleRows = 7, total = total, browsing = browsing) { it.length <= maxLength }

        assertEquals("5 tracked", header(total = 5, browsing = false))
        assertEquals("1–7 of 11  ·  ▶ to browse", header(total = 11, browsing = false))
        assertEquals("1–7 of 11  ·  ▲▼ to scroll", header(total = 11, browsing = true))
        assertEquals("1–7 of 11", header(total = 11, browsing = true, maxLength = 25))
        assertEquals("1–7 of 11", header(total = 11, browsing = false, maxLength = 5))
    }

    /** English words with German conventions, to show that numbers and dates follow the language, not US rules. */
    private val germanStyle = Formats(
        object : PulseStrings by EnglishStrings {
            override val locale: Locale = Locale.GERMAN
            override val groupingSeparator = '.'
            override val decimalSeparator = ','
            override val compactUnits = listOf(1_000L to " Tsd.", 1_000_000L to " Mio.")
            override val percentSuffix = " %"
            override val currencyAfterAmount = true
            override val monthDayPattern = "d. MMM"
            override val monthDayYearPattern = "d. MMM yyyy"
            override val weekdayMonthDayPattern = "EEE, d. MMM"
        },
    )

    @Test
    fun `numbers use the language's separators and abbreviations`() {
        assertEquals(listOf("999", "12.450", "1.234.567"), listOf(999L, 12_450L, 1_234_567L).map(germanStyle::grouped))
        assertEquals(listOf("1 Tsd.", "1,3 Tsd.", "4,2 Mio."), listOf(1_000L, 1_284L, 4_200_000L).map(germanStyle::compact))
        assertEquals(listOf("0 %", "<1 %", "42 %"), listOf(0.0, 0.004, 0.4231).map(germanStyle::share))
    }

    @Test
    fun `compact numbers use the language's units, and smaller numbers are written in full`() {
        val japaneseUnits = Formats(
            object : PulseStrings by EnglishStrings {
                override val compactUnits = listOf(10_000L to "万", 100_000_000L to "億")
            },
        )
        val koreanUnits = Formats(
            object : PulseStrings by EnglishStrings {
                override val compactUnits = listOf(1_000L to "천", 10_000L to "만", 100_000_000L to "억")
            },
        )

        assertEquals(
            listOf("999", "1,234", "9,999", "1.2万", "100万", "9999.9万", "1億", "1.2億"),
            listOf(999L, 1_234L, 9_999L, 12_345L, 999_950L, 99_999_400L, 99_999_999L, 123_456_789L).map(japaneseUnits::compact),
        )
        assertEquals(
            listOf("999", "1.2천", "1만", "1.2만", "1.2억"),
            listOf(999L, 1_234L, 9_999L, 12_345L, 123_456_789L).map(koreanUnits::compact),
        )
    }

    @Test
    fun `money puts the currency where the language does`() {
        assertEquals(
            listOf("139\u00A0$", "1.234,50\u00A0$", "-5\u00A0$", "120\u00A0MXN", "1,5 Tsd.\u00A0$"),
            listOf(
                germanStyle.money(13_900, "$"),
                germanStyle.money(123_450, "$"),
                germanStyle.money(-500, "$"),
                germanStyle.money(12_000, "MXN"),
                germanStyle.moneyCompact(1_500, "$"),
            ),
        )
    }

    @Test
    fun `dates use the language's names and order`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)
        val start = LocalDateTime.of(2026, 10, 21, 14, 0)

        assertEquals("Fr., 23. Okt.", germanStyle.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("17. Okt. – 23. Okt.", germanStyle.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals("25. Dez. 2025 – 23. Jan. 2026", germanStyle.range(window(LocalDate.of(2025, 12, 25), LocalDate.of(2026, 1, 23))))
        assertEquals(listOf("14:00", "21. Okt.", "21. Okt."), Granularity.entries.map { germanStyle.bucketTick(start, it) })
        assertEquals("Mi., 21. Okt.", germanStyle.bucketDetail(start, Granularity.DAY))
        assertEquals("Mi., 21. Okt.", germanStyle.clockDate(start))
    }

    @Test
    fun `the clock and update times are hours and minutes in the TV's timezone`() {
        val formats = Formats(EnglishStrings, zone)

        assertEquals("14:05", formats.clockTime(LocalDateTime.of(2026, 10, 21, 14, 5)))
        assertEquals("Wed, Oct 21", formats.clockDate(LocalDateTime.of(2026, 10, 21, 14, 5)))
        assertEquals("16:14", formats.time(java.time.Instant.parse("2026-10-23T22:14:00Z")))
    }
}
