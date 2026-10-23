package com.marcoelizalde.pulse.traffic.domain

import java.time.LocalDate
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
}
