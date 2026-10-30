package com.marcoelizalde.pulse.traffic.dashboard

import androidx.compose.runtime.staticCompositionLocalOf
import com.marcoelizalde.pulse.traffic.dashboard.i18n.EnglishStrings

/** The dashboard's display text for everything below it, in the chosen language. */
val LocalFormats = staticCompositionLocalOf { Formats(EnglishStrings) }
