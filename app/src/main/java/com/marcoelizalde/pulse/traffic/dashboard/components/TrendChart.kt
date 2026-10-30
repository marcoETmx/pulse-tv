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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
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
import java.time.LocalDateTime

/**
 * Visitors as an area chart, with sales as bars on a money axis on the right when the period has any.
 * When focused, D-pad left/right moves a crosshair across the buckets; the TV equivalent of a hover tooltip.
 */
@Composable
fun TrendChart(points: List<TrafficPoint>, granularity: Granularity, salesCurrency: String?, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    var focused by remember { mutableStateOf(false) }
    // The cursor follows its bucket, not its position, so a refresh leaves it in place; a new period starts at the end.
    var cursorStart by remember(granularity, points.firstOrNull()?.start) { mutableStateOf<LocalDateTime?>(null) }
    val cursor = cursorIndex(points, cursorStart)
    val reveal = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val ticks = remember(points) { Formats.axisTicks(points.maxOfOrNull { it.visitors } ?: 0) }
    // The money axis reuses the visitor gridlines, so the chart keeps a single set of lines.
    val salesTicks = remember(points, ticks) {
        val maxCents = points.maxOfOrNull { it.salesCents } ?: 0
        if (maxCents > 0) Formats.alignedTicks((maxCents + 99) / 100, ticks.size - 1) else emptyList()
    }
    val sales = salesTicks.takeIf { it.isNotEmpty() }?.let { SalesAxis(it, salesCurrency) }

    Column(modifier) {
        // The legend names the measures; color alone must not tell them apart.
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendItem("Visitors") { drawLine(PulseColors.Series, Offset(0f, center.y), Offset(size.width, center.y), 2.dp.toPx(), StrokeCap.Round) }
            if (sales != null) {
                Spacer(Modifier.width(16.dp))
                LegendItem("Sales") {
                    val barWidth = 8.dp.toPx()
                    drawRoundRect(
                        PulseColors.SalesSeries,
                        topLeft = Offset((size.width - barWidth) / 2, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
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
                        Key.DirectionLeft -> (cursor > 0).also { if (it) cursorStart = points[cursor - 1].start }
                        Key.DirectionRight -> (cursor < points.lastIndex).also { if (it) cursorStart = points[cursor + 1].start }
                        else -> false
                    }
                }
                .focusable(enabled = points.isNotEmpty()),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawTrend(points, granularity, ticks, sales, if (focused) cursor else null, reveal.value, textMeasurer)
            }
        }
    }
}

/** Where the cursor sits: on the bucket starting at [cursorStart], or on the last one when it is unset or gone. */
internal fun cursorIndex(points: List<TrafficPoint>, cursorStart: LocalDateTime?): Int =
    points.indexOfFirst { it.start == cursorStart }.takeIf { it >= 0 } ?: points.lastIndex

/** Right-hand money axis: tick values in whole currency units, aligned with the visitor gridlines. */
private class SalesAxis(val ticks: List<Long>, val currency: String?)

@Composable
private fun LegendItem(label: String, swatch: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 14.dp, height = 12.dp), onDraw = swatch)
        Spacer(Modifier.width(6.dp))
        Text(label, style = PulseType.caption, color = PulseColors.TextSecondary)
    }
}

private fun DrawScope.drawTrend(
    points: List<TrafficPoint>,
    granularity: Granularity,
    ticks: List<Long>,
    sales: SalesAxis?,
    cursor: Int?,
    reveal: Float,
    textMeasurer: TextMeasurer,
) {
    val axisStyle = PulseType.caption.copy(color = PulseColors.TextMuted)
    val labelGap = 8.dp.toPx()
    val visitorLabels = ticks.map { textMeasurer.measure(Formats.compact(it), axisStyle) }
    val salesLabels = sales?.ticks?.map { textMeasurer.measure(Formats.moneyCompact(it, sales.currency), axisStyle) }
    // Gutters fit the labels, so the axis labels line up with the card's text edges.
    val plot = Rect(
        left = visitorLabels.maxOf { it.size.width } + labelGap,
        top = 10.dp.toPx(),
        // Room on the right for the money axis.
        right = size.width - labelGap - (salesLabels?.maxOf { it.size.width } ?: 0),
        bottom = size.height - 28.dp.toPx(),
    )
    val axisMax = ticks.last().toFloat()
    fun y(visitors: Long) = plot.bottom - visitors / axisMax * plot.height
    fun x(index: Int) = if (points.size == 1) plot.center.x else plot.left + index * plot.width / (points.size - 1)

    if (cursor != null) {
        // Inside the label gap, so the outline never touches the axis labels.
        val inset = 4.dp.toPx()
        drawRoundRect(
            color = PulseColors.Series,
            topLeft = Offset(plot.left - inset, plot.top - inset),
            size = Size(plot.width + inset * 2, plot.height + inset * 2),
            cornerRadius = CornerRadius(12.dp.toPx()),
            alpha = 0.55f,
            style = Stroke(1.5.dp.toPx()),
        )
    }

    ticks.forEachIndexed { index, tick ->
        val tickY = y(tick)
        drawLine(
            color = if (tick == 0L) PulseColors.Baseline else PulseColors.Grid,
            start = Offset(plot.left, tickY),
            end = Offset(plot.right, tickY),
            strokeWidth = 1f,
        )
        val label = visitorLabels[index]
        drawText(label, topLeft = Offset(plot.left - labelGap - label.size.width, tickY - label.size.height / 2f))
        salesLabels?.get(index)?.let { drawText(it, topLeft = Offset(plot.right + labelGap, tickY - it.size.height / 2f)) }
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
        if (sales != null) {
            drawSalesBars(points, sales, plot, ::x)
            // A surface ring keeps the line readable where it crosses a bar.
            drawPath(line, PulseColors.Surface, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
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

    // Values show only while exploring with the cursor.
    if (cursor != null) {
        drawTooltip(
            detail = Formats.bucketDetail(points[index].start, granularity),
            value = "${Formats.grouped(points[index].visitors)} visitors",
            extra = sales?.let { "${Formats.money(points[index].salesCents, it.currency)} in sales" },
            marker = marker,
            plot = plot,
            textMeasurer = textMeasurer,
        )
    }
}

/** One bar per bucket, rounded at the data end and anchored to the baseline. */
private fun DrawScope.drawSalesBars(points: List<TrafficPoint>, sales: SalesAxis, plot: Rect, x: (Int) -> Float) {
    val axisMaxCents = sales.ticks.last() * 100f
    val slot = if (points.size == 1) plot.width else plot.width / (points.size - 1)
    val barWidth = (slot * 0.6f).coerceIn(2.dp.toPx(), 16.dp.toPx())
    val radius = CornerRadius(minOf(4.dp.toPx(), barWidth / 2))
    points.forEachIndexed { index, point ->
        if (point.salesCents <= 0) return@forEachIndexed
        val height = maxOf(point.salesCents / axisMaxCents * plot.height, 2.dp.toPx())
        val left = x(index) - barWidth / 2
        val bar = Path().apply {
            addRoundRect(RoundRect(left, plot.bottom - height, left + barWidth, plot.bottom, topLeftCornerRadius = radius, topRightCornerRadius = radius))
        }
        drawPath(bar, PulseColors.SalesSeries)
    }
}

private fun DrawScope.drawTooltip(detail: String, value: String, extra: String?, marker: Offset, plot: Rect, textMeasurer: TextMeasurer) {
    val detailText = textMeasurer.measure(detail, PulseType.caption.copy(color = PulseColors.TextSecondary))
    val valueText = textMeasurer.measure(value, PulseType.title.copy(color = PulseColors.TextPrimary, fontWeight = FontWeight.SemiBold))
    val extraText = extra?.let { textMeasurer.measure(it, PulseType.title.copy(color = PulseColors.TextPrimary)) }
    val padding = 12.dp.toPx()
    val width = maxOf(detailText.size.width, valueText.size.width, extraText?.size?.width ?: 0) + padding * 2
    val height = detailText.size.height + valueText.size.height + (extraText?.size?.height ?: 0) + padding * 2
    val gap = 14.dp.toPx()
    // Beside the crosshair, so it never covers the bucket being read (its point or its bar).
    val before = marker.x - gap - width
    val left = if (before >= plot.left) before else minOf(marker.x + gap, plot.right - width)
    val top = (marker.y - height / 2).coerceIn(plot.top, plot.bottom - height)
    val corner = CornerRadius(10.dp.toPx())
    drawRoundRect(PulseColors.SurfaceRaised, Offset(left, top), Size(width, height), corner)
    drawRoundRect(PulseColors.Hairline, Offset(left, top), Size(width, height), corner, style = Stroke(1f))
    drawText(detailText, topLeft = Offset(left + padding, top + padding))
    drawText(valueText, topLeft = Offset(left + padding, top + padding + detailText.size.height))
    extraText?.let { drawText(it, topLeft = Offset(left + padding, top + padding + detailText.size.height + valueText.size.height)) }
}

/** First, last and two evenly spaced buckets in between. */
private fun xTickIndices(count: Int): List<Int> =
    if (count <= 1) listOf(0) else listOf(0, (count - 1) / 3, 2 * (count - 1) / 3, count - 1).distinct()
