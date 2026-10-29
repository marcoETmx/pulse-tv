package com.marcoelizalde.pulse.traffic.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marcoelizalde.pulse.traffic.dashboard.components.AuroraBackground
import com.marcoelizalde.pulse.traffic.dashboard.components.DashboardHeader
import com.marcoelizalde.pulse.traffic.dashboard.components.ErrorPanel
import com.marcoelizalde.pulse.traffic.dashboard.components.LoadingPanel
import com.marcoelizalde.pulse.traffic.dashboard.components.ProductRanking
import com.marcoelizalde.pulse.traffic.dashboard.components.StatusBar
import com.marcoelizalde.pulse.traffic.dashboard.components.VisitorsCard
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard

/** Container: wires the view model to the screen and pauses refreshes while the app is not visible. */
@Composable
fun DashboardRoute(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.resume()
        onStopOrDispose { viewModel.pause() }
    }
    DashboardScreen(state, onSelectPeriod = viewModel::select, onRetry = viewModel::retry)
}

@Composable
fun DashboardScreen(state: DashboardUiState, onSelectPeriod: (Period) -> Unit, onRetry: () -> Unit) {
    val selectedPeriodFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { selectedPeriodFocus.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        // 5% horizontal and vertical overscan-safe margins for 960x540dp TV screens.
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
            DashboardHeader(state.selectedPeriod, onSelectPeriod, selectedPeriodFocus)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val dashboard = state.dashboard
                when {
                    dashboard != null -> DashboardContent(dashboard, state.logos, dimmed = state.isShowingStalePeriod)
                    state.error != null -> ErrorPanel(state.error, onRetry)
                    else -> LoadingPanel()
                }
            }
            Spacer(Modifier.height(14.dp))
            StatusBar(state)
        }
    }
}

@Composable
private fun DashboardContent(dashboard: TrafficDashboard, logos: Map<String, ByteArray>, dimmed: Boolean) {
    val alpha by animateFloatAsState(if (dimmed) 0.35f else 1f, label = "dim")
    Row(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        VisitorsCard(dashboard, Modifier.weight(0.6f).fillMaxHeight())
        Spacer(Modifier.width(20.dp))
        ProductRanking(dashboard.ranking, logos, Modifier.weight(0.4f).fillMaxHeight())
    }
}
