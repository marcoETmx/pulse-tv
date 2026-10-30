package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/** German, with numbers and money written as in Germany ("1.234,50 $", "42 %"); instructions use the infinitive. */
object GermanStrings : PulseStrings {
    override val locale: Locale = Locale.forLanguageTag("de-DE")
    override val groupingSeparator = '.'
    override val decimalSeparator = ','
    override val compactUnits = listOf(1_000L to "k", 1_000_000L to "M")
    override val percentSuffix = "\u00A0%"
    override val currencyAfterAmount = true
    override val monthDayPattern = "d. MMM"
    override val monthDayYearPattern = "d. MMM yyyy"
    override val weekdayMonthDayPattern = "EEE, d. MMM"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "Heute"
        Period.LAST_7_DAYS -> "7 Tage"
        Period.LAST_15_DAYS -> "15 Tage"
        Period.LAST_30_DAYS -> "30 Tage"
        Period.THIS_YEAR -> "Dieses Jahr"
    }

    override val connecting = "Verbinde mit DataFast…"
    override val loadFailedTitle = "Fehler beim Laden von DataFast"
    override val tryAgain = "Erneut versuchen"
    override val noData = "Daten konnten nicht geladen werden"
    override fun updated(time: String) = "Aktualisiert $time"
    override val refreshing = "Wird aktualisiert…"
    override fun loadingPeriod(period: Period) = "Lade Daten für ${inSentence(period)}…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "Daten für ${inSentence(selected)} nicht geladen: $error · angezeigt: ${inSentence(shown)}, Stand $time"
    override fun refreshFailed(error: String, time: String) = "Aktualisierung fehlgeschlagen: $error · Stand $time"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "DataFast ist nicht erreichbar. Internetverbindung des Fernsehers prüfen."
        AnalyticsProblem.TokenRejected -> "DataFast hat das Token abgelehnt (401). DATAFAST_TOKEN in local.properties prüfen."
        AnalyticsProblem.MissingPermissions -> "Das DataFast-Token braucht die Berechtigungen analytics:read und websites:read (403)."
        is AnalyticsProblem.RateLimited -> "DataFast-Limit erreicht (${problem.requestsPerMinute} Anfragen pro Minute)."
        is AnalyticsProblem.ProviderError -> "DataFast-Fehler ${problem.status}" + problem.detail?.let { ": $it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFast hat eine unerwartete Antwort gesendet."
        AnalyticsProblem.Unknown -> "Beim Laden des Dashboards ist ein Fehler aufgetreten."
    }
    override val demoBadge = "DEMODATEN"

    override val visitors = "Besucher"
    override val sales = "Umsatz"
    override val mixedCurrencies = "Mehrere Währungen"
    override fun payments(count: Long, formattedCount: String) = "$formattedCount ${if (count == 1L) "Zahlung" else "Zahlungen"}"
    override val chartHint = "Mit ▼ erkunden"
    override val chartHintExploring = "Mit ◀ ▶ durch die Zeit"
    override val chartEmpty = "Keine Besuche in diesem Zeitraum"
    // "Besucher" is both singular and plural.
    override fun visitorCount(count: Long, formattedCount: String) = "$formattedCount Besucher"
    override fun salesAmount(money: String) = "$money Umsatz"
    override fun weekOf(date: String) = "Woche vom $date"

    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Top-Produkte"
        RankingOrder.SALES -> "Top nach Umsatz"
    }
    override fun rankingAll(total: Int) = "$total ${if (total == 1) "Produkt" else "Produkte"}"
    override fun rankingRows(first: Int, last: Int, total: Int) = "$first–$last von $total"
    override val rankingScrollHint = "▲▼ zum Scrollen"
    override val rankingBrowseHint = "▶ zum Blättern"
    override val noWebsites = "Noch keine Websites in diesem DataFast-Konto."

    override val menu = "Menü"
    override val autoRefresh = "Auto-Aktualisierung"
    override fun refreshInterval(minutes: Int) = "Alle $minutes Min."
    override val topProducts = "Top-Produkte"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Nach Besuchern"
        RankingOrder.SALES -> "Nach Umsatz"
    }
    override val refreshNow = "Jetzt aktualisieren"
    override val notLoadedYet = "Noch nicht geladen"
    override val statusRefreshing = "wird aktualisiert…"
    override val statusRetrying = "neuer Versuch gleich"
    override fun statusNext(time: String) = "nächste $time"
    override val demoData = "Demodaten"
    override val languageSection = "Sprache"
    override val systemLanguage = "Systemsprache"
    override val languageName = "Deutsch"

    /** A period's name inside a sentence, where nouns stay capitalized: "Lade Daten für 30 Tage…", "dieses Jahr". */
    private fun inSentence(period: Period) = when (period) {
        Period.TODAY -> "heute"
        Period.THIS_YEAR -> "dieses Jahr"
        else -> period(period)
    }
}
