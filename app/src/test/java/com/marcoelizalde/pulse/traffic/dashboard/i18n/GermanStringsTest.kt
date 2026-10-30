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

class GermanStringsTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val formats = Formats(GermanStrings, zone)
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
    fun `labels are in German`() {
        assertEquals(listOf("Heute", "7 Tage", "15 Tage", "30 Tage", "Dieses Jahr"), Period.entries.map(GermanStrings::period))
        assertEquals(listOf("Nach Besuchern", "Nach Umsatz"), RankingOrder.entries.map(GermanStrings::rankingOrder))
        assertEquals("Alle 10 Min.", GermanStrings.refreshInterval(10))
    }

    @Test
    fun `counts agree with their number`() {
        assertEquals(listOf("0 Zahlungen", "1 Zahlung", "1.240 Zahlungen"), listOf(0L, 1L, 1_240L).map(formats::payments))
        assertEquals(listOf("1 Besucher", "2 Besucher"), listOf(1L, 2L).map { GermanStrings.visitorCount(it, it.toString()) })
        assertEquals(listOf("1 Produkt", "5 Produkte", "1–6 von 10"), listOf(
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 1),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 5),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 10),
        ))
    }

    @Test
    fun `numbers and money are written as in Germany`() {
        assertEquals(listOf("12.450", "1,3k", "42\u00A0%"), listOf(formats.grouped(12_450), formats.compact(1_284), formats.share(0.4231)))
        assertEquals(listOf("1.234,50\u00A0$", "120\u00A0MXN"), listOf(formats.money(123_450, "$"), formats.money(12_000, "MXN")))
    }

    @Test
    fun `dates put the day first, with German names`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("Fr., 23. Okt.", formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("17. Okt. – 23. Okt.", formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals("Woche vom 21. Okt.", formats.bucketDetail(LocalDateTime.of(2026, 10, 21, 0, 0), Granularity.WEEK))
    }

    @Test
    fun `the status line reads naturally in German, keeping nouns capitalized`() {
        val stale = DashboardUiState(Period.LAST_30_DAYS, dashboard, isLoading = false, error = AnalyticsProblem.ProviderError(500, detail = null))

        assertEquals("Aktualisiert 16:14", statusLine(DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null), formats))
        assertEquals("Lade Daten für 30 Tage…", statusLine(stale.copy(isLoading = true, error = null), formats))
        assertEquals("Lade Daten für dieses Jahr…", GermanStrings.loadingPeriod(Period.THIS_YEAR))
        assertEquals(
            "Daten für 30 Tage nicht geladen: DataFast-Fehler 500 · angezeigt: heute, Stand 16:14",
            statusLine(stale, formats),
        )
        assertEquals("Aktualisiert 16:14 · wird aktualisiert…", menuStatus(stale.copy(isLoading = true, error = null), formats))
    }

    @Test
    fun `problems are explained in German`() {
        assertEquals(
            "DataFast ist nicht erreichbar. Internetverbindung des Fernsehers prüfen.",
            GermanStrings.problem(AnalyticsProblem.Unreachable),
        )
        assertEquals("DataFast-Fehler 400: Invalid date range", GermanStrings.problem(AnalyticsProblem.ProviderError(400, "Invalid date range")))
    }
}
