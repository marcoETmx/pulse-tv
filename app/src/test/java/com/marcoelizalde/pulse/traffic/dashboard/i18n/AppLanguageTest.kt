package com.marcoelizalde.pulse.traffic.dashboard.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun `the system default follows the TV when Pulse speaks its language, else English`() {
        assertSame(SpanishStrings, AppLanguage.SYSTEM.strings(Locale.forLanguageTag("es-ES")))
        assertSame(EnglishStrings, AppLanguage.SYSTEM.strings(Locale.forLanguageTag("en-GB")))
        assertSame(EnglishStrings, AppLanguage.SYSTEM.strings(Locale.forLanguageTag("pt-BR")))
    }

    @Test
    fun `a chosen language wins over the TV's`() {
        assertSame(SpanishStrings, AppLanguage.SPANISH.strings(Locale.US))
        assertSame(EnglishStrings, AppLanguage.ENGLISH.strings(Locale.forLanguageTag("es-MX")))
    }

    @Test
    fun `each language is named in itself, and the system default in the language on screen`() {
        assertEquals(
            listOf("System default", "English", "Español"),
            AppLanguage.entries.map { it.label(EnglishStrings) },
        )
        assertEquals(
            listOf("Idioma del sistema", "English", "Español"),
            AppLanguage.entries.map { it.label(SpanishStrings) },
        )
    }
}
