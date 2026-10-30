package com.marcoelizalde.pulse.traffic.dashboard.components

import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.dashboard.RefreshInterval
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.Sales
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DashboardMenuTest {
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

    private fun time(instant: Instant) = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)

    @Test
    fun `the footer says when the data was updated and when the next refresh comes`() {
        val state = DashboardUiState(
            Period.TODAY,
            dashboard,
            isLoading = false,
            error = null,
            refreshInterval = RefreshInterval.TEN_MINUTES,
        )

        assertEquals("Updated ${time(updatedAt)} · next ${time(updatedAt.plusSeconds(600))}", menuStatus(state))
    }

    @Test
    fun `while loading, after a failure or before any data the footer says so instead`() {
        val loading = DashboardUiState(Period.TODAY, dashboard, isLoading = true, error = null)
        val failed = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = "DataFast error 500")

        assertEquals(
            listOf("Updated ${time(updatedAt)} · refreshing…", "Updated ${time(updatedAt)} · retrying soon", "Not loaded yet"),
            listOf(menuStatus(loading), menuStatus(failed), menuStatus(DashboardUiState())),
        )
    }

    @Test
    fun `the footer names the data source and the app version`() {
        assertEquals(
            listOf("DataFast · v0.1.1", "Demo data · v0.1.1"),
            listOf(menuSource(isDemo = false, version = "0.1.1"), menuSource(isDemo = true, version = "0.1.1")),
        )
    }
}
