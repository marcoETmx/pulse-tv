package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.dashboard.Formats
import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.dashboard.components.menuStatus
import com.marcoelizalde.pulse.traffic.dashboard.components.statusLine
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.Period
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import com.marcoelizalde.pulse.traffic.domain.Sales
import com.marcoelizalde.pulse.traffic.domain.TrafficDashboard
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ItalianStringsTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val formats = Formats(ItalianStrings, zone)
    private val updatedAt = Instant.parse("2026-10-23T14:14:00Z")
    private val dashboard = TrafficDashboard(
        period = Period.TODAY,
        window = Period.TODAY.windowEndingOn(LocalDate.of(2026, 10, 23), zone),
        totalVisitors = 15,
        totalSales = Sales.NONE,
        ranking = emptyList(),
        trend = emptyList(),
        updatedAt = updatedAt,
    )

    @Test
    fun `labels are in Italian`() {
        assertEquals(listOf("Oggi", "7 giorni", "15 giorni", "30 giorni", "Quest’anno"), Period.entries.map(ItalianStrings::period))
        assertEquals(listOf("Per visitatori", "Per vendite"), RankingOrder.entries.map(ItalianStrings::rankingOrder))
        assertEquals("Ogni 10 min", ItalianStrings.refreshInterval(10))
    }

    @Test
    fun `counts agree with their number`() {
        assertEquals(listOf("0 pagamenti", "1 pagamento", "1.240 pagamenti"), listOf(0L, 1L, 1_240L).map(formats::payments))
        assertEquals(listOf("1 visitatore", "2 visitatori"), listOf(1L, 2L).map { ItalianStrings.visitorCount(it, it.toString()) })
        assertEquals(listOf("1 prodotto", "5 prodotti", "1–6 di 10"), listOf(
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 1),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 5),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 10),
        ))
    }

    @Test
    fun `numbers and money are written as in Italy`() {
        assertEquals(listOf("12.450", "1,3k", "42%"), listOf(formats.grouped(12_450), formats.compact(1_284), formats.share(0.4231)))
        assertEquals(listOf("1.234,50\u00A0$", "120\u00A0MXN"), listOf(formats.money(123_450, "$"), formats.money(12_000, "MXN")))
    }

    @Test
    fun `dates put the day first, with Italian names`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("ven 23 ott", formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("17 ott – 23 ott", formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals("Settimana del 21 ott", formats.bucketDetail(LocalDateTime.of(2026, 10, 21, 0, 0), Granularity.WEEK))
    }

    @Test
    fun `the status line reads naturally in Italian`() {
        val stale = DashboardUiState(Period.LAST_30_DAYS, dashboard, isLoading = false, error = AnalyticsProblem.ProviderError(500, detail = null))

        assertEquals("Aggiornato 16:14", statusLine(DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null), formats))
        assertEquals("Caricamento dati di 30 giorni…", statusLine(stale.copy(isLoading = true, error = null), formats))
        assertEquals("Impossibile caricare 30 giorni: Errore DataFast 500 · dati di oggi delle 16:14", statusLine(stale, formats))
        assertEquals("Aggiornato 16:14 · aggiornamento…", menuStatus(stale.copy(isLoading = true, error = null), formats))
    }

    @Test
    fun `problems are explained in Italian`() {
        assertEquals(
            "Impossibile raggiungere DataFast. Controlla la connessione Internet della TV.",
            ItalianStrings.problem(AnalyticsProblem.Unreachable),
        )
        assertEquals("Errore DataFast 400: Invalid date range", ItalianStrings.problem(AnalyticsProblem.ProviderError(400, "Invalid date range")))
    }
}
