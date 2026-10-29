package com.marcoelizalde.pulse.traffic.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcoelizalde.pulse.traffic.domain.AnalyticsException
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.Product
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

data class DashboardUiState(
    val selectedPeriod: Period = Period.LAST_7_DAYS,
    val dashboard: TrafficDashboard? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDemo: Boolean = false,
    /** Encoded logo images by product id; products without one show a monogram. */
    val logos: Map<String, ByteArray> = emptyMap(),
) {
    /** The data on screen belongs to another period while the selected one loads. */
    val isShowingStalePeriod: Boolean get() = dashboard != null && dashboard.period != selectedPeriod
}

class DashboardViewModel(
    private val loadDashboard: suspend (Period) -> TrafficDashboard,
    isDemo: Boolean,
    private val loadLogo: suspend (Product) -> ByteArray? = { null },
    private val refreshEvery: Duration = 5.minutes,
    private val retryAfterFailure: Duration = 30.seconds,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DashboardUiState(isDemo = isDemo))
    val state: StateFlow<DashboardUiState> = mutableState.asStateFlow()

    private var refreshLoop: Job? = null

    private val requestedLogos = mutableSetOf<String>()

    /** Last dashboard per period, so flipping between periods does not spend DataFast's rate limit. */
    private val recent = mutableMapOf<Period, Pair<TrafficDashboard, ComparableTimeMark>>()

    init {
        restart(reuseRecent = true)
    }

    fun select(period: Period) {
        mutableState.update { it.copy(selectedPeriod = period) }
        restart(reuseRecent = true)
    }

    fun retry() = restart(reuseRecent = false)

    /** Stops refreshing while the dashboard is not visible. */
    fun pause() {
        refreshLoop?.cancel()
    }

    /** Reloads when the dashboard becomes visible again (unless its data is still fresh); no-op while refreshing. */
    fun resume() {
        if (refreshLoop?.isActive != true) restart(reuseRecent = true)
    }

    /**
     * Shows the selected period now (from [recent] when younger than [refreshEvery], otherwise loaded),
     * then keeps it fresh until the selection changes.
     */
    private fun restart(reuseRecent: Boolean) {
        refreshLoop?.cancel()
        val period = mutableState.value.selectedPeriod
        refreshLoop = viewModelScope.launch {
            val reusable = recent[period]?.takeIf { reuseRecent }
            val age = reusable?.second?.elapsedNow()
            if (reusable != null && age != null && age < refreshEvery) {
                mutableState.update { it.copy(dashboard = reusable.first, isLoading = false, error = null) }
                delay(refreshEvery - age)
            }
            while (true) {
                delay(load(period))
            }
        }
    }

    /** Loads [period] and returns how long to wait before the next attempt. */
    private suspend fun load(period: Period): Duration {
        mutableState.update { it.copy(isLoading = true) }
        return try {
            val dashboard = loadDashboard(period)
            recent[period] = dashboard to timeSource.markNow()
            mutableState.update { it.copy(dashboard = dashboard, isLoading = false, error = null) }
            loadLogos(dashboard)
            refreshEvery
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = (e as? AnalyticsException)?.message ?: "Something went wrong loading the dashboard."
            mutableState.update { it.copy(isLoading = false, error = message) }
            maxOf(retryAfterFailure, (e as? AnalyticsException)?.retryAfter ?: Duration.ZERO)
        }
    }

    /** Fetches each product's logo once, one at a time, since logos share DataFast's rate limit. */
    private fun loadLogos(dashboard: TrafficDashboard) {
        val missing = dashboard.ranking.map { it.product }.filter { requestedLogos.add(it.id) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            for (product in missing) {
                val logo = try {
                    loadLogo(product)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                if (logo != null) mutableState.update { it.copy(logos = it.logos + (product.id to logo)) }
            }
        }
    }
}
