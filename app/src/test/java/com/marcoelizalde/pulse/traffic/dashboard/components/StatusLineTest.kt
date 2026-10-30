package com.marcoelizalde.pulse.traffic.dashboard.components

import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.Sales
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StatusLineTest {
    private val updatedAt = Instant.parse("2026-10-23T22:14:00Z")
    private val dashboard = TrafficDashboard(
        period = Period.TODAY,
        window = Period.TODAY.windowEndingOn(LocalDate.of(2026, 10, 23), ZoneId.of("UTC")),
        totalVisitors = 15,
        totalSales = Sales.NONE,
        ranking = emptyList(),
        trend = emptyList(),
        updatedAt = updatedAt,
    )
    private val time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(updatedAt)

    @Test
    fun `an up-to-date dashboard only says when it was updated`() {
        val state = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null)

        assertEquals("Updated $time", statusLine(state))
    }

    @Test
    fun `a failed refresh still says which data is on screen`() {
        val state = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = "DataFast error 500")

        assertEquals("Couldn't refresh: DataFast error 500 · showing data from $time", statusLine(state))
    }

    @Test
    fun `a period that failed to load names it and the period still on screen`() {
        val state = DashboardUiState(Period.LAST_30_DAYS, dashboard, isLoading = false, error = "DataFast error 500")

        assertEquals("Couldn't load 30 days: DataFast error 500 · showing today from $time", statusLine(state))
    }

    @Test
    fun `demo data shows the demo badge instead of the update time`() {
        val state = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null, isDemo = true)

        assertTrue(showsDemoBadge(state))
    }

    @Test
    fun `demo data still shows loading as text`() {
        val state = DashboardUiState(Period.TODAY, dashboard, isLoading = true, error = null, isDemo = true)

        assertFalse(showsDemoBadge(state))
        assertEquals("Refreshing…", statusLine(state))
    }

    @Test
    fun `demo data still shows a failed refresh as text`() {
        val state = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = "DataFast error 500", isDemo = true)

        assertFalse(showsDemoBadge(state))
        assertEquals("Couldn't refresh: DataFast error 500 · showing data from $time", statusLine(state))
    }

    @Test
    fun `demo data still shows the first load as text`() {
        val state = DashboardUiState(Period.TODAY, dashboard = null, isLoading = true, error = null, isDemo = true)

        assertFalse(showsDemoBadge(state))
        assertEquals("Connecting to DataFast…", statusLine(state))
    }

    @Test
    fun `real data never shows the demo badge`() {
        val state = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null, isDemo = false)

        assertFalse(showsDemoBadge(state))
        assertEquals("Updated $time", statusLine(state))
    }
}
