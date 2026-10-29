package com.marcoelizalde.pulse.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.darkColorScheme
import com.marcoelizalde.pulse.R

object PulseColors {
    val Ink = Color(0xFF080A10)
    val Surface = Color(0xFF11141D)
    val SurfaceRaised = Color(0xFF1B2030)
    val Hairline = Color(0x1AFFFFFF)
    val Grid = Color(0xFF232838)
    val Baseline = Color(0xFF3A4055)
    val TextPrimary = Color(0xFFF2F3F7)
    val TextSecondary = Color(0xFFA9AFC2)
    val TextMuted = Color(0xFF7A819C)

    /** Data marks; passes the dataviz palette checks against [Surface] in dark mode. */
    val Series = Color(0xFF19AB8C)

    /** Money, so sales never read as visitor counts. */
    val Sales = Color(0xFFF2C14E)

    /** Sales bars next to [Series]; passes the dataviz palette checks against [Surface] in dark mode. */
    val SalesSeries = Color(0xFFBB892A)

    val BrandTeal = Color(0xFF2FD8B4)
    val BrandViolet = Color(0xFF8C7CF0)
    val BrandBlue = Color(0xFF2B6CB0)
    val Warning = Color(0xFFFAB219)
}

/** Space Grotesk is a variable font; each weight sets its `wght` axis. */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

object PulseType {
    val hero = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 80.sp,
        lineHeight = 86.sp,
        letterSpacing = (-2).sp,
    )
    /** A second headline figure next to [hero]. */
    val figure = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1).sp,
    )
    val clock = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 30.sp)
    val title = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp)
    val overline = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 2.sp)
    val body = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val pill = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val rowName = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 17.sp)
    val rowValue = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    val caption = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = PulseColors.Series,
            onPrimary = PulseColors.Ink,
            background = PulseColors.Ink,
            onBackground = PulseColors.TextPrimary,
            surface = PulseColors.Surface,
            onSurface = PulseColors.TextPrimary,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides PulseColors.TextPrimary) {
            ProvideTextStyle(PulseType.body, content)
        }
    }
}
