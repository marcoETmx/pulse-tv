package com.marcoelizalde.pulse.traffic.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcoelizalde.pulse.traffic.domain.AnalyticsException
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class DashboardUiState(
    val selectedPeriod: Period = Period.TODAY,
    val dashboard: TrafficDashboard? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDemo: Boolean = false,
) {
    /** The data on screen belongs to another period while the selected one loads. */
    val isShowingStalePeriod: Boolean get() = dashboard != null && dashboard.period != selectedPeriod
}

class DashboardViewModel(
    private val loadDashboard: suspend (Period) -> TrafficDashboard,
    isDemo: Boolean,
    private val refreshEvery: Duration = 5.minutes,
    private val retryAfterFailure: Duration = 30.seconds,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DashboardUiState(isDemo = isDemo))
    val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()

    private var refreshLoop: Job? = null

    init {
        restart()
    }

    fun select(period: Period) {
        mutableState.update { it.copy(selectedPeriod = period) }
        restart()
    }

    fun retry() = restart()

    /** Stops refreshing while the dashboard is not visible. */
    fun pause() {
        refreshLoop?.cancel()
    }

    /** Reloads at once when the dashboard becomes visible again; no-op while already refreshing. */
    fun resume() {
        if (refreshLoop?.isActive != true) restart()
    }

    /** Loads the selected period now, then keeps it fresh until the selection changes. */
    private fun restart() {
        refreshLoop?.cancel()
        refreshLoop = viewModelScope.launch {
            while (true) {
                val succeeded = load(mutableState.value.selectedPeriod)
                delay(if (succeeded) refreshEvery else retryAfterFailure)
            }
        }
    }

    private suspend fun load(period: Period): Boolean {
        mutableState.update { it.copy(isLoading = true) }
        return try {
            val dashboard = loadDashboard(period)
            mutableState.update { it.copy(dashboard = dashboard, isLoading = false, error = null) }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = (e as? AnalyticsException)?.message ?: "Something went wrong loading the dashboard."
            mutableState.update { it.copy(isLoading = false, error = message) }
            false
        }
    }
}
