package com.marcoelizalde.pulse.traffic.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Granularity {
    HOUR,
    DAY,
    WEEK;

    companion object {
        /** Keeps charts readable: hours for one day, days up to two months, weeks beyond. */
        fun forDays(days: Long): Granularity = when {
            days <= 1 -> HOUR
            days <= 62 -> DAY
            else -> WEEK
        }
    }
}

/** Inclusive calendar-day range, interpreted in [zone]. */
data class ReportWindow(
    val start: LocalDate,
    val end: LocalDate,
    val zone: ZoneId,
    val granularity: Granularity,
)

/** Wall-clock time in [zone] from [start] up to [end]. */
data class TimeSpan(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val zone: ZoneId,
)

enum class Period {
    TODAY,
    LAST_7_DAYS,
    LAST_15_DAYS,
    LAST_30_DAYS,
    THIS_YEAR;

    fun windowEndingOn(today: LocalDate, zone: ZoneId): ReportWindow {
        val start = when (this) {
            TODAY -> today
            LAST_7_DAYS -> today.minusDays(6)
            LAST_15_DAYS -> today.minusDays(14)
            LAST_30_DAYS -> today.minusDays(29)
            THIS_YEAR -> today.withDayOfYear(1)
        }
        val days = ChronoUnit.DAYS.between(start, today) + 1
        return ReportWindow(start, today, zone, Granularity.forDays(days))
    }

    /**
     * [window] one period earlier, cut where [now] falls one period earlier, so a period still in
     * progress is compared with an equally long stretch.
     */
    fun previousSpan(window: ReportWindow, now: LocalDateTime): TimeSpan =
        TimeSpan(earlier(window.start.atStartOfDay()), earlier(now), window.zone)

    private fun earlier(time: LocalDateTime): LocalDateTime = when (this) {
        TODAY -> time.minusDays(1)
        LAST_7_DAYS -> time.minusDays(7)
        LAST_15_DAYS -> time.minusDays(15)
        LAST_30_DAYS -> time.minusDays(30)
        THIS_YEAR -> time.minusYears(1)
    }
}
