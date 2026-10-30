package com.marcoelizalde.pulse.traffic.dashboard

import androidx.lifecycle.viewModelScope
import com.marcoelizalde.pulse.traffic.domain.AnalyticsException
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.Product
import com.marcoelizalde.pulse.traffic.domain.RankedProduct
import com.marcoelizalde.pulse.traffic.domain.Sales
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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
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
    fun `opens on the last seven days`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }) { viewModel ->
            runCurrent()

            assertEquals(listOf(Period.LAST_7_DAYS), calls)
            assertEquals(
                DashboardUiState(Period.LAST_7_DAYS, dashboardFor(Period.LAST_7_DAYS), isLoading = false, error = null, isDemo = true),
                viewModel.state.value,
            )
        }
    }

    @Test
    fun `selecting a period keeps the previous data until the new one arrives`() = runTest(dispatcher) {
        val today = CompletableDeferred<Unit>()
        withViewModel({ period ->
            if (period == Period.TODAY) today.await()
            dashboardFor(period)
        }) { viewModel ->
            runCurrent()

            viewModel.select(Period.TODAY)
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(Period.TODAY, selectedPeriod)
                assertEquals(Period.LAST_7_DAYS, dashboard?.period)
                assertTrue(isLoading)
                assertTrue(isShowingStalePeriod)
            }

            today.complete(Unit)
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(dashboardFor(Period.TODAY), dashboard)
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
            assertEquals(listOf(Period.LAST_7_DAYS, Period.LAST_30_DAYS), calls)

            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(listOf(Period.LAST_7_DAYS, Period.LAST_30_DAYS, Period.LAST_30_DAYS), calls)
        }
    }

    @Test
    fun `a failed first load shows the error and refreshing now loads again`() = runTest(dispatcher) {
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
            viewModel.refreshNow()
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(dashboardFor(Period.LAST_7_DAYS), dashboard)
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
                assertEquals(dashboardFor(Period.LAST_7_DAYS), dashboard)
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
            assertEquals(dashboardFor(Period.LAST_7_DAYS), viewModel.state.value.dashboard)
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
    fun `a rate limit waits until DataFast accepts requests again`() = runTest(dispatcher) {
        withViewModel({ period ->
            if (calls.size == 1) throw AnalyticsException("DataFast rate limit reached (60 requests per minute).", retryAfter = 50.seconds)
            dashboardFor(period)
        }) { viewModel ->
            runCurrent()

            advanceTimeBy(49.seconds)
            runCurrent()
            assertEquals(1, calls.size)

            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(2, calls.size)
            assertNull(viewModel.state.value.error)
        }
    }

    @Test
    fun `returning to a period loaded minutes ago reuses it without calling DataFast`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }) { viewModel ->
            runCurrent()
            viewModel.select(Period.TODAY)
            runCurrent()
            advanceTimeBy(2.minutes)

            viewModel.select(Period.LAST_7_DAYS)
            runCurrent()

            assertEquals(listOf(Period.LAST_7_DAYS, Period.TODAY), calls)
            with(viewModel.state.value) {
                assertEquals(dashboardFor(Period.LAST_7_DAYS), dashboard)
                assertFalse(isLoading)
                assertFalse(isShowingStalePeriod)
            }

            // It still refreshes once the reused data turns five minutes old.
            advanceTimeBy(3.minutes)
            runCurrent()
            assertEquals(listOf(Period.LAST_7_DAYS, Period.TODAY, Period.LAST_7_DAYS), calls)
        }
    }

    @Test
    fun `a period older than five minutes is reloaded when selected again`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }) { viewModel ->
            runCurrent()
            viewModel.select(Period.TODAY)
            runCurrent()
            advanceTimeBy(6.minutes)

            viewModel.select(Period.LAST_7_DAYS)
            runCurrent()

            assertEquals(Period.LAST_7_DAYS, calls.last())
            assertEquals(4, calls.size) // today, 7 days, 7 days refreshed at 5 min, today again
        }
    }

    @Test
    fun `refreshing now always asks DataFast again`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }) { viewModel ->
            runCurrent()

            viewModel.refreshNow()
            runCurrent()

            assertEquals(listOf(Period.LAST_7_DAYS, Period.LAST_7_DAYS), calls)
        }
    }

    @Test
    fun `logos load once per product after the dashboard`() = runTest(dispatcher) {
        val logoCalls = mutableListOf<String>()
        withViewModel(
            load = { dashboardFor(it, ranking = listOf(ranked(shop), ranked(blog))) },
            loadLogo = { product ->
                logoCalls += product.id
                if (product == shop) byteArrayOf(7) else null
            },
        ) { viewModel ->
            runCurrent()

            assertEquals(listOf(shop.id, blog.id), logoCalls)
            assertArrayEquals(byteArrayOf(7), viewModel.state.value.logos[shop.id])
            assertFalse(blog.id in viewModel.state.value.logos)

            advanceTimeBy(5.minutes)
            runCurrent()
            assertEquals(listOf(shop.id, blog.id), logoCalls)
        }
    }

    @Test
    fun `a logo that failed to load is asked again with the next refresh`() = runTest(dispatcher) {
        val logoCalls = mutableListOf<String>()
        withViewModel(
            load = { dashboardFor(it, ranking = listOf(ranked(shop))) },
            loadLogo = { product ->
                logoCalls += product.id
                if (logoCalls.size == 1) throw IOException("timeout") else byteArrayOf(7)
            },
        ) { viewModel ->
            runCurrent()
            assertTrue(viewModel.state.value.logos.isEmpty())

            advanceTimeBy(5.minutes)
            runCurrent()

            assertEquals(listOf(shop.id, shop.id), logoCalls)
            assertArrayEquals(byteArrayOf(7), viewModel.state.value.logos[shop.id])
        }
    }

    @Test
    fun `a failing logo never breaks the dashboard`() = runTest(dispatcher) {
        withViewModel(
            load = { dashboardFor(it, ranking = listOf(ranked(shop))) },
            loadLogo = { throw AnalyticsException("DataFast rate limit reached (60 requests per minute).") },
        ) { viewModel ->
            runCurrent()

            with(viewModel.state.value) {
                assertEquals(Period.LAST_7_DAYS, dashboard?.period)
                assertNull(error)
                assertTrue(logos.isEmpty())
            }
        }
    }

    @Test
    fun `unexpected failures show a generic message`() = runTest(dispatcher) {
        withViewModel({ error("NullPointer somewhere") }) { viewModel ->
            runCurrent()

            assertEquals("Something went wrong loading the dashboard.", viewModel.state.value.error)
        }
    }

    @Test
    fun `a saved refresh interval paces the refreshes`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }, refreshIntervals = SavedInterval(RefreshInterval.TEN_MINUTES)) { viewModel ->
            runCurrent()
            assertEquals(RefreshInterval.TEN_MINUTES, viewModel.state.value.refreshInterval)

            advanceTimeBy(10.minutes - 1.seconds)
            runCurrent()
            assertEquals(1, calls.size)

            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(2, calls.size)
        }
    }

    @Test
    fun `choosing a refresh interval saves it and paces the next refresh from the last load`() = runTest(dispatcher) {
        val store = SavedInterval()
        withViewModel({ dashboardFor(it) }, refreshIntervals = store) { viewModel ->
            runCurrent()
            advanceTimeBy(2.minutes)

            viewModel.setRefreshInterval(RefreshInterval.FIFTEEN_MINUTES)
            runCurrent()

            assertEquals(RefreshInterval.FIFTEEN_MINUTES, store.saved)
            assertEquals(RefreshInterval.FIFTEEN_MINUTES, viewModel.state.value.refreshInterval)
            assertEquals(1, calls.size)

            advanceTimeBy(13.minutes - 1.seconds)
            runCurrent()
            assertEquals(1, calls.size)

            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(2, calls.size)
        }
    }

    @Test
    fun `choosing an interval shorter than the data's age reloads right away`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }, refreshIntervals = SavedInterval(RefreshInterval.FIFTEEN_MINUTES)) { viewModel ->
            runCurrent()
            advanceTimeBy(7.minutes)
            runCurrent()
            assertEquals(1, calls.size)

            viewModel.setRefreshInterval(RefreshInterval.FIVE_MINUTES)
            runCurrent()

            assertEquals(2, calls.size)
        }
    }

    @Test
    fun `a period opened within the refresh interval is reused without calling DataFast`() = runTest(dispatcher) {
        withViewModel({ dashboardFor(it) }, refreshIntervals = SavedInterval(RefreshInterval.FIFTEEN_MINUTES)) { viewModel ->
            runCurrent()
            viewModel.select(Period.TODAY)
            runCurrent()
            advanceTimeBy(12.minutes)

            viewModel.select(Period.LAST_7_DAYS)
            runCurrent()

            assertEquals(listOf(Period.LAST_7_DAYS, Period.TODAY), calls)
            assertEquals(Period.LAST_7_DAYS, viewModel.state.value.dashboard?.period)
        }
    }

    @Test
    fun `refreshing now is ignored while a load is running`() = runTest(dispatcher) {
        val slowLoad = CompletableDeferred<Unit>()
        withViewModel({ period ->
            slowLoad.await()
            dashboardFor(period)
        }) { viewModel ->
            runCurrent()
            assertTrue(viewModel.state.value.isLoading)

            viewModel.refreshNow()
            runCurrent()
            slowLoad.complete(Unit)
            runCurrent()

            assertEquals(1, calls.size)
            assertEquals(dashboardFor(Period.LAST_7_DAYS), viewModel.state.value.dashboard)
        }
    }

    private val shop = Product("1", "Shop", "shop.example")
    private val blog = Product("2", "Blog", "blog.example")

    private fun ranked(product: Product) = RankedProduct(product, visitors = 10, share = 0.5)

    private class SavedInterval(var saved: RefreshInterval? = null) : RefreshIntervalStore {
        override fun load(): RefreshInterval? = saved

        override fun save(interval: RefreshInterval) {
            saved = interval
        }
    }

    private inline fun TestScope.withViewModel(
        noinline load: suspend (Period) -> TrafficDashboard,
        noinline loadLogo: suspend (Product) -> ByteArray? = { null },
        refreshIntervals: RefreshIntervalStore = SavedInterval(),
        block: TestScope.(DashboardViewModel) -> Unit,
    ) {
        val viewModel = DashboardViewModel(
            loadDashboard = { period ->
                calls += period
                load(period)
            },
            isDemo = true,
            loadLogo = loadLogo,
            refreshIntervals = refreshIntervals,
            timeSource = testScheduler.timeSource,
        )
        try {
            block(viewModel)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun dashboardFor(period: Period, ranking: List<RankedProduct> = emptyList()) = TrafficDashboard(
        period = period,
        window = period.windowEndingOn(LocalDate.of(2026, 10, 23), ZoneId.of("UTC")),
        totalVisitors = 100,
        totalSales = Sales.NONE,
        ranking = ranking,
        trend = emptyList(),
        updatedAt = Instant.EPOCH,
    )
}
