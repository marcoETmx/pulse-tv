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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.traffic.dashboard.Change
import com.marcoelizalde.pulse.traffic.dashboard.LocalFormats
import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.dashboard.RankingRow
import com.marcoelizalde.pulse.traffic.dashboard.rankingRows
import com.marcoelizalde.pulse.traffic.domain.RankedProduct
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Rows on screen on a 1080p TV; [rowsThatFit] gives the count for the room the card has. */
private const val ROWS_ON_1080P = 7
private val RowHeight = 44.dp
private val RowGap = 4.dp
private val RowShape = RoundedCornerShape(12.dp)

/** How many whole rows fit in [height]: seven on a 1080p TV, fewer when a taller font leaves the card less room. */
internal fun rowsThatFit(height: Dp): Int = ((height + RowGap) / (RowHeight + RowGap)).toInt().coerceAtLeast(1)

/**
 * Every product in its own row, in [order]. As many whole rows as fit are on screen, seven on a 1080p TV;
 * D-pad ▶ enters the list and ▲▼ moves one row at a time, scrolling the rest into view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductRanking(
    ranking: List<RankedProduct>,
    logos: Map<String, ByteArray>,
    order: RankingOrder,
    modifier: Modifier = Modifier,
) {
    val formats = LocalFormats.current
    val rows = remember(ranking, order) { rankingRows(ranking, order) }
    // The bars measure what the ranking is ordered by, in that measure's color, as in the chart.
    val barColor = if (order == RankingOrder.SALES) PulseColors.SalesSeries else PulseColors.Series
    val scroll = rememberScrollState()
    val pitch = with(LocalDensity.current) { (RowHeight + RowGap).toPx() }
    val firstVisible by remember { derivedStateOf { (scroll.value / pitch).roundToInt() } }
    var browsing by remember { mutableStateOf(false) }
    // The list sets how many rows fit once it is measured.
    var visibleRows by remember { mutableIntStateOf(ROWS_ON_1080P) }

    PulseCard(modifier) {
        // Less padding below than above, so the last row may come closer to the bottom edge than the title is to the top.
        Column(Modifier.fillMaxSize().padding(start = 14.dp, top = 20.dp, end = 14.dp, bottom = 8.dp)) {
            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Overline(formats.strings.rankingTitle(order))
                // The width the title leaves, measured so the header stays on one line in every language.
                BoxWithConstraints(Modifier.weight(1f).padding(start = 8.dp), contentAlignment = Alignment.CenterEnd) {
                    val measurer = rememberTextMeasurer()
                    val header = formats.rankingHeader(firstVisible, visibleRows, rows.size, browsing) {
                        measurer.measure(it, PulseType.caption, maxLines = 1).size.width <= constraints.maxWidth
                    }
                    Text(
                        header,
                        style = PulseType.caption,
                        color = PulseColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (rows.isEmpty()) {
                Text(
                    formats.strings.noWebsites,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style = PulseType.body,
                    color = PulseColors.TextSecondary,
                )
            }
            val firstRowFocus = remember { FocusRequester() }
            // The room the header leaves, so a taller font or larger text shows fewer rows instead of cutting the last one.
            BoxWithConstraints(Modifier.weight(1f)) {
                val fit = rowsThatFit(maxHeight)
                SideEffect { visibleRows = fit }
                CompositionLocalProvider(LocalBringIntoViewSpec provides ScrollOnlyAsNeeded) {
                    RankingList(rows, logos, barColor, fit, scroll, firstRowFocus, onBrowsingChange = { browsing = it })
                }
            }
        }
    }
}

@Composable
private fun RankingList(
    rows: List<RankingRow>,
    logos: Map<String, ByteArray>,
    barColor: Color,
    visibleRows: Int,
    scroll: ScrollState,
    firstRowFocus: FocusRequester,
    onBrowsingChange: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            // Exactly [visibleRows] rows tall, so each ▲▼ step scrolls by one whole row.
            .height(RowHeight * visibleRows + RowGap * (visibleRows - 1))
            .onFocusChanged { onBrowsingChange(it.hasFocus) }
            // ▶ from the chart lands on the first row (or the last one focused), not the nearest one.
            .focusRestorer(firstRowFocus)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        rows.forEachIndexed { index, row ->
            // Keyed by product, so focus and the bar animation follow a product when a refresh reorders the list.
            key(row.productId) {
                RankingLine(
                    row,
                    logo = logos[row.productId],
                    fraction = row.bar.toFloat(),
                    barColor = barColor,
                    order = index,
                    modifier = if (index == 0) Modifier.focusRequester(firstRowFocus) else Modifier,
                )
            }
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
private fun RankingLine(
    row: RankingRow,
    logo: ByteArray?,
    fraction: Float,
    barColor: Color,
    order: Int,
    modifier: Modifier = Modifier,
) {
    val formats = LocalFormats.current
    var focused by remember { mutableStateOf(false) }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        delay(order.coerceAtMost(ROWS_ON_1080P) * 70L)
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
            modifier = Modifier.width(30.dp),
            style = PulseType.rowName,
            color = if (focused) PulseColors.TextSecondary else PulseColors.TextMuted,
        )
        ProductLogo(row.name, logo)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // The sales are measured first so they keep their space; the name takes what is left.
            Row {
                Text(
                    row.name,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    style = PulseType.rowName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row.sales?.let { sales ->
                    Text(
                        formats.money(sales.cents, sales.currency),
                        modifier = Modifier.padding(start = 8.dp).alignByBaseline(),
                        style = PulseType.rowName,
                        color = PulseColors.Sales,
                        maxLines = 1,
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
                        val visible = if (row.bar > 0) maxOf(size.width * grow.value, size.height) else 0f
                        if (visible > 0f) {
                            // Square at the baseline, rounded at the data end.
                            val bar = Path().apply {
                                addRoundRect(RoundRect(0f, 0f, visible, size.height, topRightCornerRadius = radius, bottomRightCornerRadius = radius))
                            }
                            drawPath(bar, barColor)
                        }
                    },
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.widthIn(min = 48.dp), horizontalAlignment = Alignment.End) {
            Row {
                row.change?.let { change ->
                    Text(
                        if (change == Change.UP) "▲" else "▼",
                        modifier = Modifier.padding(end = 6.dp).alignByBaseline(),
                        style = PulseType.caption,
                        color = if (change == Change.UP) PulseColors.Up else PulseColors.Down,
                    )
                }
                Text(formats.grouped(row.visitors), modifier = Modifier.alignByBaseline(), style = PulseType.rowValue, maxLines = 1)
            }
            Text(formats.share(row.share), style = PulseType.caption, color = PulseColors.TextMuted)
        }
    }
}
