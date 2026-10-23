package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType

private val CardShape = RoundedCornerShape(24.dp)

// Surface at 84% with a faint top highlight, pre-blended into one fill to keep overdraw low.
private val CardFill = Brush.verticalGradient(listOf(Color(0xD61D2028), Color(0xD611141D)))

/** Frosted card that floats over the aurora background. */
@Composable
fun PulseCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(CardFill)
            .border(1.dp, PulseColors.Hairline, CardShape),
        content = content,
    )
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, style = PulseType.overline, color = PulseColors.TextMuted)
}

/** The Pulse heartbeat mark, drawn with the brand gradient. */
@Composable
fun PulseGlyph(modifier: Modifier = Modifier, alpha: Float = 1f) {
    Canvas(modifier) {
        val path = Path()
        HeartbeatPoints.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x * size.width, y * size.height) else path.lineTo(x * size.width, y * size.height)
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(PulseColors.BrandTeal, PulseColors.BrandViolet)),
            alpha = alpha,
            style = Stroke(width = size.height * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private val HeartbeatPoints = listOf(
    0.04f to 0.56f, 0.30f to 0.56f, 0.40f to 0.26f, 0.52f to 0.90f, 0.64f to 0.10f, 0.73f to 0.56f, 0.96f to 0.56f,
)

/**
 * Live-data dot that ripples a few times whenever [pulseKey] changes (new data arrived), then rests,
 * so an idle dashboard renders no frames.
 */
@Composable
fun LiveDot(pulseKey: Any?, modifier: Modifier = Modifier, color: Color = PulseColors.Series) {
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(pulseKey) {
        repeat(3) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(1_200, easing = LinearEasing))
        }
        ripple.snapTo(0f)
    }
    Canvas(modifier.size(14.dp)) {
        val radius = size.minDimension / 2
        val progress = ripple.value
        if (progress > 0f) {
            drawCircle(color, radius = radius * (0.45f + 0.55f * progress), alpha = 0.5f * (1 - progress))
        }
        drawCircle(color, radius = radius * 0.45f)
    }
}

/** Status glyph for warnings: always paired with a text label, never color alone. */
@Composable
fun WarningGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val triangle = Path().apply {
            moveTo(w / 2, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(triangle, PulseColors.Warning)
        val stroke = w * 0.11f
        drawLine(PulseColors.Ink, Offset(w / 2, h * 0.36f), Offset(w / 2, h * 0.66f), stroke, StrokeCap.Round)
        drawCircle(PulseColors.Ink, radius = stroke * 0.6f, center = Offset(w / 2, h * 0.83f))
    }
}

@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(PulseColors.BrandViolet.copy(alpha = 0.16f))
            .border(1.dp, PulseColors.BrandViolet.copy(alpha = 0.5f), shape)
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        Text("DEMO DATA · add DATAFAST_TOKEN to local.properties", style = PulseType.caption)
    }
}
