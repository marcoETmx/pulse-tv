package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.traffic.dashboard.Formats
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.TrafficPoint
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType

/**
 * Single-series area chart. When focused, D-pad left/right moves a crosshair across the buckets;
 * the TV equivalent of a hover tooltip.
 */
@Composable
fun TrendChart(points: List<TrafficPoint>, granularity: Granularity, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    var focused by remember { mutableStateOf(false) }
    var cursor by remember(points) { mutableIntStateOf(points.lastIndex) }
    val reveal = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val ticks = remember(points) { Formats.axisTicks(points.maxOfOrNull { it.visitors } ?: 0) }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Formats.trendTitle(granularity), style = PulseType.title, color = PulseColors.TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                if (focused) "◀ ▶ move through time" else "Press ▼ to explore",
                style = PulseType.caption,
                color = PulseColors.TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> (cursor > 0).also { if (it) cursor-- }
                        Key.DirectionRight -> (cursor < points.lastIndex).also { if (it) cursor++ }
                        else -> false
                    }
                }
                .focusable(enabled = points.isNotEmpty()),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawTrend(points, granularity, ticks, if (focused) cursor else null, reveal.value, textMeasurer)
            }
        }
    }
}

private fun DrawScope.drawTrend(
    points: List<TrafficPoint>,
    granularity: Granularity,
    ticks: List<Long>,
    cursor: Int?,
    reveal: Float,
    textMeasurer: TextMeasurer,
) {
    val axisStyle = PulseType.caption.copy(color = PulseColors.TextMuted)
    val plot = Rect(
        left = 48.dp.toPx(),
        top = 10.dp.toPx(),
        // Room on the right for the end-of-line value label.
        right = size.width - 52.dp.toPx(),
        bottom = size.height - 28.dp.toPx(),
    )
    val axisMax = ticks.last().toFloat()
    fun y(visitors: Long) = plot.bottom - visitors / axisMax * plot.height
    fun x(index: Int) = if (points.size == 1) plot.center.x else plot.left + index * plot.width / (points.size - 1)

    if (cursor != null) {
        val inset = 8.dp.toPx()
        drawRoundRect(
            color = PulseColors.Series,
            topLeft = Offset(plot.left - inset, plot.top - inset),
            size = Size(plot.width + inset * 2, plot.height + inset * 2),
            cornerRadius = CornerRadius(12.dp.toPx()),
            alpha = 0.55f,
            style = Stroke(1.5.dp.toPx()),
        )
    }

    ticks.forEach { tick ->
        val tickY = y(tick)
        drawLine(
            color = if (tick == 0L) PulseColors.Baseline else PulseColors.Grid,
            start = Offset(plot.left, tickY),
            end = Offset(plot.right, tickY),
            strokeWidth = 1f,
        )
        val label = textMeasurer.measure(Formats.compact(tick), axisStyle)
        drawText(label, topLeft = Offset(plot.left - 12.dp.toPx() - label.size.width, tickY - label.size.height / 2f))
    }

    if (points.isEmpty()) {
        val empty = textMeasurer.measure("No visits recorded in this period", PulseType.body.copy(color = PulseColors.TextSecondary))
        drawText(empty, topLeft = plot.center - Offset(empty.size.width / 2f, empty.size.height / 2f))
        return
    }

    xTickIndices(points.size).forEach { index ->
        val label = textMeasurer.measure(Formats.bucketTick(points[index].start, granularity), axisStyle)
        val left = (x(index) - label.size.width / 2f).coerceIn(plot.left - 8.dp.toPx(), size.width - label.size.width)
        drawText(label, topLeft = Offset(left, plot.bottom + 8.dp.toPx()))
    }

    val line = Path()
    points.forEachIndexed { index, point ->
        if (index == 0) line.moveTo(x(index), y(point.visitors)) else line.lineTo(x(index), y(point.visitors))
    }
    val area = Path().apply {
        addPath(line)
        lineTo(x(points.lastIndex), plot.bottom)
        lineTo(x(0), plot.bottom)
        close()
    }
    clipRect(right = plot.left + (plot.width + 12.dp.toPx()) * reveal) {
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(PulseColors.Series.copy(alpha = 0.28f), PulseColors.Series.copy(alpha = 0f)),
                startY = plot.top,
                endY = plot.bottom,
            ),
        )
        drawPath(line, PulseColors.Series, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    if (reveal < 1f && cursor == null) return

    val index = cursor ?: points.lastIndex
    val marker = Offset(x(index), y(points[index].visitors))
    if (cursor != null) {
        drawLine(PulseColors.TextMuted, Offset(marker.x, plot.top), Offset(marker.x, plot.bottom), strokeWidth = 1f, alpha = 0.7f)
    }
    drawCircle(PulseColors.Surface, radius = 7.dp.toPx(), center = marker)
    drawCircle(PulseColors.Series, radius = 5.dp.toPx(), center = marker)

    if (cursor == null) {
        drawEndLabel(Formats.grouped(points[index].visitors), marker, plot, textMeasurer)
    } else {
        drawTooltip(
            detail = Formats.bucketDetail(points[index].start, granularity),
            value = "${Formats.grouped(points[index].visitors)} visitors",
            marker = marker,
            plot = plot,
            textMeasurer = textMeasurer,
        )
    }
}

private fun DrawScope.drawEndLabel(text: String, marker: Offset, plot: Rect, textMeasurer: TextMeasurer) {
    val label = textMeasurer.measure(text, PulseType.caption.copy(color = PulseColors.TextPrimary, fontWeight = FontWeight.SemiBold))
    val top = (marker.y - label.size.height / 2f).coerceIn(plot.top, plot.bottom - label.size.height)
    drawText(label, topLeft = Offset(marker.x + 12.dp.toPx(), top))
}

private fun DrawScope.drawTooltip(detail: String, value: String, marker: Offset, plot: Rect, textMeasurer: TextMeasurer) {
    val detailText = textMeasurer.measure(detail, PulseType.caption.copy(color = PulseColors.TextSecondary))
    val valueText = textMeasurer.measure(value, PulseType.title.copy(color = PulseColors.TextPrimary, fontWeight = FontWeight.SemiBold))
    val padding = 12.dp.toPx()
    val width = maxOf(detailText.size.width, valueText.size.width) + padding * 2
    val height = detailText.size.height + valueText.size.height + padding * 2
    val gap = 14.dp.toPx()
    val left = (marker.x - width / 2).coerceIn(plot.left, plot.right - width)
    val above = marker.y - gap - height
    val top = if (above >= plot.top) above else marker.y + gap
    val corner = CornerRadius(10.dp.toPx())
    drawRoundRect(PulseColors.SurfaceRaised, Offset(left, top), Size(width, height), corner)
    drawRoundRect(PulseColors.Hairline, Offset(left, top), Size(width, height), corner, style = Stroke(1f))
    drawText(detailText, topLeft = Offset(left + padding, top + padding))
    drawText(valueText, topLeft = Offset(left + padding, top + padding + detailText.size.height))
}

/** First, last and two evenly spaced buckets in between. */
private fun xTickIndices(count: Int): List<Int> =
    if (count <= 1) listOf(0) else listOf(0, (count - 1) / 3, 2 * (count - 1) / 3, count - 1).distinct()
