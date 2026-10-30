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

class FrenchStringsTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val formats = Formats(FrenchStrings, zone)
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
    fun `labels are in French`() {
        assertEquals(listOf("Aujourd’hui", "7 jours", "15 jours", "30 jours", "Année"), Period.entries.map(FrenchStrings::period))
        assertEquals(listOf("Par visiteurs", "Par ventes"), RankingOrder.entries.map(FrenchStrings::rankingOrder))
        assertEquals("Toutes les 10 min", FrenchStrings.refreshInterval(10))
    }

    @Test
    fun `counts agree with their number, singular for zero too`() {
        assertEquals(listOf("0 paiement", "1 paiement", "1\u00A0240 paiements"), listOf(0L, 1L, 1_240L).map(formats::payments))
        assertEquals(listOf("1 visiteur", "2 visiteurs"), listOf(1L, 2L).map { FrenchStrings.visitorCount(it, it.toString()) })
        assertEquals(listOf("1 produit", "5 produits", "1–6 sur 10"), listOf(
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 1),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 5),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 10),
        ))
    }

    @Test
    fun `numbers and money are written as in France`() {
        assertEquals(listOf("12\u00A0450", "1,3k", "42\u00A0%"), listOf(formats.grouped(12_450), formats.compact(1_284), formats.share(0.4231)))
        assertEquals(listOf("1\u00A0234,50\u00A0$", "120\u00A0MXN"), listOf(formats.money(123_450, "$"), formats.money(12_000, "MXN")))
    }

    @Test
    fun `dates put the day first, with French names`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("ven. 23 oct.", formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("17 oct. – 23 oct.", formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals("Semaine du 21 oct.", formats.bucketDetail(LocalDateTime.of(2026, 10, 21, 0, 0), Granularity.WEEK))
    }

    @Test
    fun `the status line reads naturally in French`() {
        val stale = DashboardUiState(Period.LAST_30_DAYS, dashboard, isLoading = false, error = AnalyticsProblem.ProviderError(500, detail = null))

        assertEquals("Mis à jour à 16:14", statusLine(DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null), formats))
        assertEquals("Chargement de 30 jours…", statusLine(stale.copy(isLoading = true, error = null), formats))
        assertEquals(
            listOf("Chargement d’aujourd’hui…", "Chargement de cette année…"),
            listOf(Period.TODAY, Period.THIS_YEAR).map(FrenchStrings::loadingPeriod),
        )
        assertEquals(
            "Impossible de charger 30 jours\u00A0: Erreur DataFast 500 · affichage d’aujourd’hui, données de 16:14",
            statusLine(stale, formats),
        )
        assertEquals("Mis à jour à 16:14 · actualisation…", menuStatus(stale.copy(isLoading = true, error = null), formats))
    }

    @Test
    fun `problems are explained in French`() {
        assertEquals(
            "Impossible de joindre DataFast. Vérifiez la connexion Internet de la TV.",
            FrenchStrings.problem(AnalyticsProblem.Unreachable),
        )
        assertEquals("Erreur DataFast 400\u00A0: Invalid date range", FrenchStrings.problem(AnalyticsProblem.ProviderError(400, "Invalid date range")))
    }
}
