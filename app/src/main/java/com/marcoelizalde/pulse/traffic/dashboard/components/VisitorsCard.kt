package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.traffic.dashboard.Formats
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType
import kotlin.math.roundToLong

/** The hero figure (total visitors) with the trend that produced it. */
@Composable
fun VisitorsCard(dashboard: TrafficDashboard, modifier: Modifier = Modifier) {
    PulseCard(modifier) {
        Column(Modifier.fillMaxSize().padding(start = 28.dp, end = 24.dp, top = 22.dp, bottom = 16.dp)) {
            Overline("Visitors · ${Formats.period(dashboard.period)}")
            AnimatedCount(dashboard.totalVisitors, PulseType.hero)
            val products = dashboard.ranking.size
            Text(
                "${Formats.range(dashboard.window)}  ·  across $products ${if (products == 1) "product" else "products"}",
                style = PulseType.body,
                color = PulseColors.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            TrendChart(dashboard.trend, dashboard.window.granularity, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** Counts up (or down) to [target] whenever it changes. */
@Composable
private fun AnimatedCount(target: Long, style: TextStyle) {
    var shown by remember { mutableLongStateOf(0L) }
    LaunchedEffect(target) {
        val from = shown
        animate(0f, 1f, animationSpec = tween(1_100, easing = FastOutSlowInEasing)) { fraction, _ ->
            shown = from + ((target - from) * fraction).roundToLong()
        }
    }
    Text(Formats.grouped(shown), style = style)
}
