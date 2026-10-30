package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.dashboard.Formats
import com.marcoelizalde.pulse.ui.theme.PulseColors
import com.marcoelizalde.pulse.ui.theme.PulseType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LoadingPanel(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PulseGlyph(Modifier.size(width = 96.dp, height = 68.dp), alpha = alpha)
        Spacer(Modifier.height(18.dp))
        Text("Connecting to DataFast…", style = PulseType.title, color = PulseColors.TextSecondary)
    }
}

@Composable
fun ErrorPanel(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WarningGlyph(Modifier.size(44.dp))
        Spacer(Modifier.height(18.dp))
        Text("Couldn't load DataFast", style = PulseType.title.copy(fontSize = 28.sp, lineHeight = 34.sp))
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            modifier = Modifier.widthIn(max = 560.dp),
            style = PulseType.body,
            color = PulseColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(26.dp))
        Button(onClick = onRetry) { Text("Try again", style = PulseType.pill) }
    }
}

@Composable
fun StatusBar(state: DashboardUiState, modifier: Modifier = Modifier) {
    val refreshFailed = state.error != null && state.dashboard != null
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (refreshFailed) WarningGlyph(Modifier.size(14.dp)) else LiveDot(pulseKey = state.dashboard?.updatedAt)
        Spacer(Modifier.width(10.dp))
        if (showsDemoBadge(state)) {
            DemoBadge()
        } else {
            Text(
                statusLine(state),
                modifier = Modifier.weight(1f),
                style = PulseType.caption,
                color = if (refreshFailed) PulseColors.TextPrimary else PulseColors.TextSecondary,
                // Room for a refresh error next to the time of the data on screen.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Demo data's update time means nothing, so the demo badge takes its place; the header has no room
 * for both. Loading and errors still show as text.
 */
internal fun showsDemoBadge(state: DashboardUiState): Boolean = state.isDemo && isUpToDate(state)

internal fun statusLine(state: DashboardUiState): String {
    val dashboard = state.dashboard
    return when {
        dashboard == null && state.error != null -> "Couldn't load data"
        dashboard == null -> "Connecting to DataFast…"
        isUpToDate(state) -> "Updated ${UpdatedAt.format(dashboard.updatedAt)}"
        state.isLoading && state.isShowingStalePeriod -> "Loading ${Formats.period(state.selectedPeriod).lowercase()}…"
        state.error != null && state.isShowingStalePeriod ->
            "Couldn't load ${Formats.period(state.selectedPeriod).lowercase()}: ${state.error} · " +
                "showing ${Formats.period(dashboard.period).lowercase()} from ${UpdatedAt.format(dashboard.updatedAt)}"
        state.error != null -> "Couldn't refresh: ${state.error} · showing data from ${UpdatedAt.format(dashboard.updatedAt)}"
        // Neither up to date nor failed, so the period on screen is reloading.
        else -> "Refreshing…"
    }
}

/** Data on screen with nothing loading or failing: all there is to say is when it was updated. */
private fun isUpToDate(state: DashboardUiState): Boolean =
    state.dashboard != null && !state.isLoading && state.error == null

internal val UpdatedAt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US).withZone(ZoneId.systemDefault())
