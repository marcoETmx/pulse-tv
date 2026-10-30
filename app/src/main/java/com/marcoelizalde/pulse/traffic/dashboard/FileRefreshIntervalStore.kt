package com.marcoelizalde.pulse.traffic.dashboard

import java.io.File

/** A few bytes in [file]: the interval's minutes. A missing or unreadable file counts as no choice. */
class FileRefreshIntervalStore(private val file: File) : RefreshIntervalStore {
    override fun load(): RefreshInterval? =
        runCatching { file.readText().trim().toInt() }.getOrNull()?.let(RefreshInterval::ofMinutes)

    override fun save(interval: RefreshInterval) {
        file.parentFile?.mkdirs()
        file.writeText(interval.minutes.toString())
    }
}
