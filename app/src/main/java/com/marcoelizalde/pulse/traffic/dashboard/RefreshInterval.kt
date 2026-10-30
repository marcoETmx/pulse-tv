package com.marcoelizalde.pulse.traffic.dashboard

import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How often the dashboard reloads on its own. None is shorter than five minutes, so automatic loads never
 * spend more of DataFast's rate limit than they did before the interval could be chosen.
 */
enum class RefreshInterval(val minutes: Int) {
    FIVE_MINUTES(5),
    TEN_MINUTES(10),
    FIFTEEN_MINUTES(15);

    val duration: Duration get() = minutes.minutes

    companion object {
        val DEFAULT = FIVE_MINUTES

        /** The choice lasting [minutes], or null when there is none. */
        fun ofMinutes(minutes: Int): RefreshInterval? = entries.firstOrNull { it.minutes == minutes }

        /** Kept in [file] as its minutes. */
        fun store(file: File): SettingStore<RefreshInterval> =
            FileSettingStore(file, encode = { it.minutes.toString() }, decode = { it.toIntOrNull()?.let(::ofMinutes) })
    }
}
