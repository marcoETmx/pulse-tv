package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
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

private const val MAX_ROWS = 6

@Composable
fun ProductRanking(ranking: List<RankedProduct>, modifier: Modifier = Modifier) {
    val rows = remember(ranking) { rankingRows(ranking, MAX_ROWS) }
    val maxVisitors = (rows.maxOfOrNull { it.visitors } ?: 0).coerceAtLeast(1)

    PulseCard(modifier) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline("Top products")
                Spacer(Modifier.weight(1f))
                Text("${ranking.size} tracked", style = PulseType.caption, color = PulseColors.TextMuted)
            }
            Spacer(Modifier.height(20.dp))
            if (rows.isEmpty()) {
                Text("No websites in this DataFast account yet.", style = PulseType.body, color = PulseColors.TextSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEachIndexed { index, row ->
                    RankingLine(row, fraction = row.visitors.toFloat() / maxVisitors, order = index)
                }
            }
        }
    }
}

@Composable
private fun RankingLine(row: RankingRow, fraction: Float, order: Int) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        delay(order * 70L)
        grow.animateTo(fraction, tween(800, easing = FastOutSlowInEasing))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            row.rank?.let { "%02d".format(it) } ?: "··",
            modifier = Modifier.width(34.dp),
            style = PulseType.rowName,
            color = PulseColors.TextMuted,
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
