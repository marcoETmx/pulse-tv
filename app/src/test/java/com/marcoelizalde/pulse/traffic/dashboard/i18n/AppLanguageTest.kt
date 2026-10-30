package com.marcoelizalde.pulse.traffic.dashboard.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun `the system default follows the TV when Pulse speaks its language, else English`() {
        assertSame(SpanishStrings, AppLanguage.SYSTEM.strings(Locale.forLanguageTag("es-ES")))
        assertSame(FrenchStrings, AppLanguage.SYSTEM.strings(Locale.forLanguageTag("fr-CA")))
        assertSame(GermanStrings, AppLanguage.SYSTEM.strings(Locale.forLanguageTag("de-AT")))
        assertSame(ItalianStrings, AppLanguage.SYSTEM.strings(Locale.forLanguageTag("it-IT")))
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
            listOf("System default", "English", "Español", "Français", "Deutsch", "Italiano"),
            AppLanguage.entries.map { it.label(EnglishStrings) },
        )
        assertEquals(
            listOf("Idioma del sistema", "English", "Español", "Français", "Deutsch", "Italiano"),
            AppLanguage.entries.map { it.label(SpanishStrings) },
        )
        assertEquals(
            listOf("Langue du système", "Systemsprache", "Lingua di sistema"),
            listOf(FrenchStrings, GermanStrings, ItalianStrings).map(AppLanguage.SYSTEM::label),
        )
    }
}
