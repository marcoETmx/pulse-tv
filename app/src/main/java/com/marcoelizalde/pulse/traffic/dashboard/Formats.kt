package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Display text for the dashboard; English copy with US number formatting. */
object Formats {
    private val hour = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val monthDayYear = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val weekdayMonthDay = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

    fun grouped(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)

    fun compact(value: Long): String = when {
        value < 1_000 -> value.toString()
        value / 1_000.0 < 999.95 -> oneDecimal(value / 1_000.0) + "K"
        else -> oneDecimal(value / 1_000_000.0) + "M"
    }

    /** Clean axis ticks from zero that cover [max] in at most four steps. */
    fun axisTicks(max: Long): List<Long> {
        val target = max.coerceAtLeast(4)
        val step = generateSequence(1L) { it * 10 }
            .flatMap { sequenceOf(it, it * 2, it * 5) }
            .first { ceilDiv(target, it) <= 4 }
        return (0..ceilDiv(target, step)).map { it * step }
    }

    /** Whole amounts without cents ("$139"); a currency code instead of a symbol is spaced ("MXN 120"). */
    fun money(cents: Long, currency: String?): String {
        if (cents < 0) return "-" + money(-cents, currency)
        val amount = if (cents % 100 == 0L) {
            grouped(cents / 100)
        } else {
            NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }.format(cents / 100.0)
        }
        return currencyPrefix(currency) + amount
    }

    /** Axis labels in whole currency units: "$15", "$1.5K". */
    fun moneyCompact(units: Long, currency: String?): String = currencyPrefix(currency) + compact(units)

    /** Ticks for a second axis that reuse the first axis's [steps] gridlines, with a clean step that covers [max]. */
    fun alignedTicks(max: Long, steps: Int): List<Long> {
        val step = generateSequence(1L) { it * 10 }
            .flatMap { sequenceOf(it, it * 2, it * 5) }
            .first { it * steps >= max }
        return (0..steps).map { it * step }
    }

    fun payments(count: Long): String = "${grouped(count)} ${if (count == 1L) "payment" else "payments"}"

    fun share(share: Double): String = when {
        share <= 0.0 -> "0%"
        share < 0.005 -> "<1%"
        else -> "${(share * 100).roundToInt()}%"
    }

    fun period(period: Period): String = when (period) {
        Period.TODAY -> "Today"
        Period.LAST_7_DAYS -> "7 days"
        Period.LAST_15_DAYS -> "15 days"
        Period.LAST_30_DAYS -> "30 days"
        Period.THIS_YEAR -> "This year"
    }

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
        Granularity.WEEK -> "Week of ${monthDay.format(start)}"
    }

    /** Header text for the ranking list: which rows are on screen, or the count when all fit. */
    fun rankingRange(firstVisible: Int, visibleRows: Int, total: Int): String {
        if (total <= visibleRows) return "$total tracked"
        val first = firstVisible.coerceIn(0, total - visibleRows)
        return "${first + 1}–${first + visibleRows} of $total"
    }

    private fun currencyPrefix(currency: String?): String {
        val symbol = currency?.takeIf { it.isNotBlank() } ?: "$"
        return if (symbol.all { it.isLetter() }) "$symbol " else symbol
    }

    private fun oneDecimal(value: Double): String {
        val tenths = (value * 10).roundToLong()
        return if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
    }

    private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1) / divisor
}
