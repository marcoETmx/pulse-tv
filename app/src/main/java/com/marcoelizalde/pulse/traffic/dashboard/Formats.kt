package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.dashboard.i18n.PulseStrings
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Display text for the dashboard in one language: [strings] gives the words and how that language writes
 * numbers and dates. Times are in [zone], the TV's timezone.
 */
class Formats(val strings: PulseStrings, zone: ZoneId = ZoneId.systemDefault()) {
    private val hour = DateTimeFormatter.ofPattern("HH:mm", strings.locale)
    private val zonedHour = hour.withZone(zone)
    private val monthDay = DateTimeFormatter.ofPattern(strings.monthDayPattern, strings.locale)
    private val monthDayYear = DateTimeFormatter.ofPattern(strings.monthDayYearPattern, strings.locale)
    private val weekdayMonthDay = DateTimeFormatter.ofPattern(strings.weekdayMonthDayPattern, strings.locale)
    private val symbols = DecimalFormatSymbols(strings.locale).apply {
        groupingSeparator = strings.groupingSeparator
        decimalSeparator = strings.decimalSeparator
    }

    fun grouped(value: Long): String = DecimalFormat("#,##0", symbols).format(value)

    fun compact(value: Long): String = when {
        value < 1_000 -> value.toString()
        value / 1_000.0 < 999.95 -> oneDecimal(value / 1_000.0) + strings.thousandsSuffix
        else -> oneDecimal(value / 1_000_000.0) + strings.millionsSuffix
    }

    /** Hours and minutes in the TV's timezone, such as when the data was updated. */
    fun time(instant: Instant): String = zonedHour.format(instant)

    fun clockTime(now: LocalDateTime): String = hour.format(now)

    fun clockDate(now: LocalDateTime): String = weekdayMonthDay.format(now)

    /** Whole amounts without cents ("$139"); a currency code instead of a symbol is spaced ("MXN 120"). */
    fun money(cents: Long, currency: String?): String {
        if (cents < 0) return "-" + money(-cents, currency)
        val amount = if (cents % 100 == 0L) grouped(cents / 100) else DecimalFormat("#,##0.00", symbols).format(cents / 100.0)
        return withCurrency(amount, currency)
    }

    /** Axis labels in whole currency units: "$15", "$1.5K". */
    fun moneyCompact(units: Long, currency: String?): String = withCurrency(compact(units), currency)

    fun payments(count: Long): String = strings.payments(count, grouped(count))

    fun share(share: Double): String = when {
        share <= 0.0 -> "0"
        share < 0.005 -> "<1"
        else -> "${(share * 100).roundToInt()}"
    } + strings.percentSuffix

    fun range(window: ReportWindow): String = when {
        window.start == window.end -> weekdayMonthDay.format(window.start)
        window.start.year == window.end.year -> "${monthDay.format(window.start)} – ${monthDay.format(window.end)}"
        else -> "${monthDayYear.format(window.start)} – ${monthDayYear.format(window.end)}"
    }

    fun bucketTick(start: LocalDateTime, granularity: Granularity): String = when (granularity) {
        Granularity.HOUR -> hour.format(start)
        Granularity.DAY, Granularity.WEEK -> monthDay.format(start)
    }

    fun bucketDetail(start: LocalDateTime, granularity: Granularity): String = when (granularity) {
        Granularity.HOUR -> "${hour.format(start)} – ${hour.format(start.plusHours(1))}"
        Granularity.DAY -> weekdayMonthDay.format(start)
        Granularity.WEEK -> strings.weekOf(monthDay.format(start))
    }

    /** Header text for the ranking list: which rows are on screen, or the count when all fit. */
    fun rankingRange(firstVisible: Int, visibleRows: Int, total: Int): String {
        if (total <= visibleRows) return strings.rankingAll(total)
        val first = firstVisible.coerceIn(0, total - visibleRows)
        return strings.rankingRows(first + 1, first + visibleRows, total)
    }

    /** Before the amount ("$139", "MXN 120") or after it, kept on the same line ("139 $"), as the language writes it. */
    private fun withCurrency(amount: String, currency: String?): String {
        val symbol = currency?.takeIf { it.isNotBlank() } ?: "$"
        return when {
            strings.currencyAfterAmount -> "$amount\u00A0$symbol"
            symbol.all { it.isLetter() } -> "$symbol $amount"
            else -> symbol + amount
        }
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToLong()
        return if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}${strings.decimalSeparator}${tenths % 10}"
    }

    // Axis ticks are the same in every language.
    companion object {
        /** Clean axis ticks from zero that cover [max] in at most four steps. */
        fun axisTicks(max: Long): List<Long> {
            val target = max.coerceAtLeast(4)
            val step = generateSequence(1L) { it * 10 }
                .flatMap { sequenceOf(it, it * 2, it * 5) }
                .first { ceilDiv(target, it) <= 4 }
            return (0..ceilDiv(target, step)).map { it * step }
        }

        /** Ticks for a second axis that reuse the first axis's [steps] gridlines, with a clean step that covers [max]. */
        fun alignedTicks(max: Long, steps: Int): List<Long> {
            val step = generateSequence(1L) { it * 10 }
                .flatMap { sequenceOf(it, it * 2, it * 5) }
                .first { it * steps >= max }
            return (0..steps).map { it * step }
        }

        private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1) / divisor
    }
}
