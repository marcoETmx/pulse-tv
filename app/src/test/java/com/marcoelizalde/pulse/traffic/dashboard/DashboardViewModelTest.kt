package com.marcoelizalde.pulse.traffic.dashboard

import androidx.lifecycle.viewModelScope
import com.marcoelizalde.pulse.traffic.domain.AnalyticsException
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val calls = mutableListOf<Period>()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opens on today's dashboard`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }) { viewModel ->
            runCurrent()

            assertEquals(listOf(Period.TODAY), calls)
            assertEquals(
                DashboardUiState(Period.TODAY, dashboardFor(Period.TODAY), isLoading = false, error = null, isDemo = true),
                viewModel.state.value,
            )
        }
    }

    @Test
    fun `selecting a period keeps the previous data until the new one arrives`() = runTest(dispatcher) {
        val sevenDays = CompletableDeferred<Unit>()
        withViewModel({ period ->
            if (period == Period.LAST_7_DAYS) sevenDays.await()
            dashboardFor(period)
        }) { viewModel ->
            runCurrent()

            viewModel.select(Period.LAST_7_DAYS)
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(Period.LAST_7_DAYS, selectedPeriod)
                assertEquals(Period.TODAY, dashboard?.period)
                assertTrue(isLoading)
                assertTrue(isShowingStalePeriod)
            }

            sevenDays.complete(Unit)
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(dashboardFor(Period.LAST_7_DAYS), dashboard)
                assertFalse(isLoading)
                assertFalse(isShowingStalePeriod)
            }
        }
    }

    @Test
    fun `refreshes the selected period every five minutes`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }) { viewModel ->
            runCurrent()
            viewModel.select(Period.LAST_30_DAYS)
            runCurrent()

            advanceTimeBy(5.minutes - 1.seconds)
            runCurrent()
            assertEquals(listOf(Period.TODAY, Period.LAST_30_DAYS), calls)

            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(listOf(Period.TODAY, Period.LAST_30_DAYS, Period.LAST_30_DAYS), calls)
        }
    }

    @Test
    fun `a failed first load shows the error and retry loads again`() = runTest(dispatcher) {
        var fail = true
        withViewModel({ period ->
            if (fail) throw AnalyticsException("DataFast rejected the token (401).")
            dashboardFor(period)
        }) { viewModel ->
            runCurrent()

            with(viewModel.state.value) {
                assertNull(dashboard)
                assertFalse(isLoading)
                assertEquals("DataFast rejected the token (401).", error)
            }

            fail = false
            viewModel.retry()
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(dashboardFor(Period.TODAY), dashboard)
                assertNull(error)
            }
        }
    }

    @Test
    fun `a failed refresh keeps the last data on screen`() = runTest(dispatcher) {
        withViewModel({ period ->
            if (calls.size > 1) throw AnalyticsException("DataFast error 500")
            dashboardFor(period)
        }) { viewModel ->
            runCurrent()

            advanceTimeBy(5.minutes)
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(dashboardFor(Period.TODAY), dashboard)
                assertFalse(isLoading)
                assertEquals("DataFast error 500", error)
            }
        }
    }

    @Test
    fun `retries thirty seconds after a failure`() = runTest(dispatcher) {
        withViewModel({ period ->
            if (calls.size == 1) throw AnalyticsException("DataFast error 503")
            dashboardFor(period)
        }) { viewModel ->
            runCurrent()

            advanceTimeBy(30.seconds)
            runCurrent()

            assertEquals(2, calls.size)
            assertEquals(dashboardFor(Period.TODAY), viewModel.state.value.dashboard)
            assertNull(viewModel.state.value.error)
        }
    }

    @Test
    fun `pausing stops refreshes and resuming reloads right away`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }) { viewModel ->
            runCurrent()

            viewModel.pause()
            advanceTimeBy(20.minutes)
            runCurrent()
            assertEquals(1, calls.size)

            viewModel.resume()
            runCurrent()
            assertEquals(2, calls.size)

            viewModel.resume()
            runCurrent()
            assertEquals(2, calls.size)
        }
    }

    @Test
    fun `unexpected failures show a generic message`() = runTest(dispatcher) {
        withViewModel({ error("NullPointer somewhere") }) { viewModel ->
            runCurrent()

            assertEquals("Something went wrong loading the dashboard.", viewModel.state.value.error)
        }
    }

    private inline fun TestScope.withViewModel(
        noinline load: suspend (Period) -> TrafficDashboard,
        block: TestScope.(DashboardViewModel) -> Unit,
    ) {
        val viewModel = DashboardViewModel(
            loadDashboard = { period ->
                calls += period
                load(period)
            },
            isDemo = true,
        )
        try {
            block(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun dashboardFor(period: Period) = TrafficDashboard(
        period = period,
        window = period.windowEndingOn(LocalDate.of(2026, 10, 23), ZoneId.of("UTC")),
        totalVisitors = 100,
        ranking = emptyList(),
        trend = emptyList(),
        updatedAt = Instant.EPOCH,
    )
}
