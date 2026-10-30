package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.FileSettingStore
import com.marcoelizalde.pulse.traffic.dashboard.SettingStore
import java.io.File
import java.util.Locale

/** The language chosen in the menu. [key] is the language's code, also used to match the TV's language. */
enum class AppLanguage(val key: String, private val catalog: PulseStrings?) {
    SYSTEM("system", null),
    ENGLISH("en", EnglishStrings),
    SPANISH("es", SpanishStrings),
    FRENCH("fr", FrenchStrings),
    GERMAN("de", GermanStrings),
    ITALIAN("it", ItalianStrings),
    JAPANESE("ja", JapaneseStrings),

    /** Simplified Chinese, also for TVs set to Traditional Chinese. */
    CHINESE("zh", ChineseStrings),
    KOREAN("ko", KoreanStrings);

    /** The catalog to show. [SYSTEM] follows [systemLocale] when Pulse speaks its language, else English. */
    fun strings(systemLocale: Locale): PulseStrings =
        catalog ?: entries.firstOrNull { it.key == systemLocale.language }?.catalog ?: EnglishStrings

    /** The menu label: each language named in itself, the system default in the language on screen. */
    fun label(onScreen: PulseStrings): String = catalog?.languageName ?: onScreen.systemLanguage

    companion object {
        val DEFAULT = SYSTEM

        /** Kept in [file] as its key. */
        fun store(file: File): SettingStore<AppLanguage> =
            FileSettingStore(file, encode = { it.key }, decode = { key -> entries.firstOrNull { it.key == key } })
    }
}
