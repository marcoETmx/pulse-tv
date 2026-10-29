package com.marcoelizalde.pulse.traffic.dashboard.components

import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.Sales
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import org.junit.Assert.assertEquals
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
}
