package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.traffic.dashboard.Formats
import com.marcoelizalde.pulse.traffic.dashboard.RankingRow
import com.marcoelizalde.pulse.traffic.dashboard.rankingRows
import com.marcoelizalde.pulse.traffic.domain.RankedProduct
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val VISIBLE_ROWS = 6
private val RowHeight = 44.dp
private val RowGap = 6.dp
private val RowShape = RoundedCornerShape(12.dp)

/**
 * Every product in its own row. Six rows fit on screen; D-pad ▶ enters the list and ▲▼ moves
 * one row at a time, scrolling the rest into view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductRanking(ranking: List<RankedProduct>, modifier: Modifier = Modifier) {
    val rows = remember(ranking) { rankingRows(ranking) }
    val maxVisitors = (rows.maxOfOrNull { it.visitors } ?: 0).coerceAtLeast(1)
    val scroll = rememberScrollState()
    val pitch = with(LocalDensity.current) { (RowHeight + RowGap).toPx() }
    val firstVisible by remember { derivedStateOf { (scroll.value / pitch).roundToInt() } }
    var browsing by remember { mutableStateOf(false) }

    PulseCard(modifier) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 20.dp)) {
            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Overline("Top products")
                Spacer(Modifier.weight(1f))
                val range = Formats.rankingRange(firstVisible, VISIBLE_ROWS, rows.size)
                val hint = when {
                    rows.size <= VISIBLE_ROWS -> ""
                    browsing -> "  ·  ▲▼ to scroll"
                    else -> "  ·  ▶ to browse"
                }
                Text(range + hint, style = PulseType.caption, color = PulseColors.TextMuted)
            }
            Spacer(Modifier.height(16.dp))
            if (rows.isEmpty()) {
                Text(
                    "No websites in this DataFast account yet.",
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style = PulseType.body,
                    color = PulseColors.TextSecondary,
                )
            }
            val firstRowFocus = remember { FocusRequester() }
            CompositionLocalProvider(LocalBringIntoViewSpec provides ScrollOnlyAsNeeded) {
                RankingList(rows, maxVisitors, scroll, firstRowFocus, onBrowsingChange = { browsing = it })
            }
        }
    }
}

@Composable
private fun RankingList(
    rows: List<RankingRow>,
    maxVisitors: Long,
    scroll: ScrollState,
    firstRowFocus: FocusRequester,
    onBrowsingChange: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            // Exactly six rows tall, so each ▲▼ step scrolls by one whole row.
            .height(RowHeight * VISIBLE_ROWS + RowGap * (VISIBLE_ROWS - 1))
            .onFocusChanged { onBrowsingChange(it.hasFocus) }
            // ▶ from the chart lands on the first row (or the last one focused), not the nearest one.
            .focusRestorer(firstRowFocus)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        rows.forEachIndexed { index, row ->
            RankingLine(
                row,
                fraction = row.visitors.toFloat() / maxVisitors,
                order = index,
                modifier = if (index == 0) Modifier.focusRequester(firstRowFocus) else Modifier,
            )
        }
    }
}

/** TV's default pivots the focused row to a third of the list; this scrolls just enough, keeping rows aligned. */
@OptIn(ExperimentalFoundationApi::class)
private val ScrollOnlyAsNeeded = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = when {
        offset < 0f -> offset
        offset + size > containerSize -> offset + size - containerSize
        else -> 0f
    }
}

@Composable
private fun RankingLine(row: RankingRow, fraction: Float, order: Int, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        delay(order.coerceAtMost(VISIBLE_ROWS) * 70L)
        grow.animateTo(fraction, tween(800, easing = FastOutSlowInEasing))
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(RowHeight)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .then(
                if (focused) {
                    Modifier
                        .drawBehind { drawRoundRect(PulseColors.SurfaceRaised, cornerRadius = CornerRadius(12.dp.toPx())) }
                        .border(1.dp, PulseColors.Series.copy(alpha = 0.6f), RowShape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(row.rank),
            modifier = Modifier.width(34.dp),
            style = PulseType.rowName,
            color = if (focused) PulseColors.TextSecondary else PulseColors.TextMuted,
        )
        Column(Modifier.weight(1f)) {
            // The name is measured first so it keeps its space; the domain takes what is left.
            Row {
                Text(
                    row.name,
                    modifier = Modifier.alignByBaseline(),
                    style = PulseType.rowName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.detail.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        row.detail,
                        modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                        style = PulseType.caption,
                        color = PulseColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .drawBehind {
                        val radius = CornerRadius(size.height / 2)
                        drawRoundRect(PulseColors.Grid, cornerRadius = radius)
                        val visible = if (row.visitors > 0) maxOf(size.width * grow.value, size.height) else 0f
                        if (visible > 0f) {
                            // Square at the baseline, rounded at the data end.
                            val bar = Path().apply {
                                addRoundRect(RoundRect(0f, 0f, visible, size.height, topRightCornerRadius = radius, bottomRightCornerRadius = radius))
                            }
                            drawPath(bar, PulseColors.Series)
                        }
                    },
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.width(84.dp), horizontalAlignment = Alignment.End) {
            Text(Formats.grouped(row.visitors), style = PulseType.rowValue, maxLines = 1)
            Text(Formats.share(row.share), style = PulseType.caption, color = PulseColors.TextMuted)
        }
    }
}
