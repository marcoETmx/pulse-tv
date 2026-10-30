package com.marcoelizalde.pulse.traffic.dashboard

import java.io.File

/** Keeps one chosen setting between launches. */
interface SettingStore<T : Any> {
    /** The saved choice, or null when nothing was chosen yet. */
    fun load(): T?

    fun save(value: T)
}

/**
 * A few bytes in [file], written by [encode]. A missing or unreadable file, or text [decode] does not
 * recognize, counts as no choice.
 */
class FileSettingStore<T : Any>(
    private val file: File,
    private val encode: (T) -> String,
    private val decode: (String) -> T?,
) : SettingStore<T> {
    override fun load(): T? = runCatching { file.readText().trim() }.getOrNull()?.let(decode)

    override fun save(value: T) {
        file.parentFile?.mkdirs()
        file.writeText(encode(value))
    }
}
