package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/** Italian, with numbers and money written as in Italy ("1.234,50 $", "42%"). */
object ItalianStrings : PulseStrings {
    override val locale: Locale = Locale.forLanguageTag("it-IT")
    override val groupingSeparator = '.'
    override val decimalSeparator = ','
    override val thousandsSuffix = "k"
    override val millionsSuffix = "M"
    override val percentSuffix = "%"
    override val currencyAfterAmount = true
    override val monthDayPattern = "d MMM"
    override val monthDayYearPattern = "d MMM yyyy"
    override val weekdayMonthDayPattern = "EEE d MMM"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "Oggi"
        Period.LAST_7_DAYS -> "7 giorni"
        Period.LAST_15_DAYS -> "15 giorni"
        Period.LAST_30_DAYS -> "30 giorni"
        Period.THIS_YEAR -> "Quest’anno"
    }

    override val connecting = "Connessione a DataFast…"
    override val loadFailedTitle = "Impossibile caricare DataFast"
    override val tryAgain = "Riprova"
    override val noData = "Impossibile caricare i dati"
    override fun updated(time: String) = "Aggiornato $time"
    override val refreshing = "Aggiornamento…"
    override fun loadingPeriod(period: Period) = "Caricamento dati di ${inSentence(period)}…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "Impossibile caricare ${inSentence(selected)}: $error · dati di ${inSentence(shown)} delle $time"
    override fun refreshFailed(error: String, time: String) = "Impossibile aggiornare: $error · dati delle $time"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "Impossibile raggiungere DataFast. Controlla la connessione Internet della TV."
        AnalyticsProblem.TokenRejected -> "DataFast ha rifiutato il token (401). Controlla DATAFAST_TOKEN in local.properties."
        AnalyticsProblem.MissingPermissions -> "Il token DataFast richiede i permessi analytics:read e websites:read (403)."
        is AnalyticsProblem.RateLimited -> "Limite di DataFast raggiunto (${problem.requestsPerMinute} richieste al minuto)."
        is AnalyticsProblem.ProviderError -> "Errore DataFast ${problem.status}" + problem.detail?.let { ": $it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFast ha inviato una risposta inattesa."
        AnalyticsProblem.Unknown -> "Si è verificato un errore durante il caricamento della dashboard."
    }
    override val demoBadge = "DATI DEMO"

    override val visitors = "Visitatori"
    override val sales = "Vendite"
    override val mixedCurrencies = "Valute diverse"
    override fun payments(count: Long, formattedCount: String) = "$formattedCount ${if (count == 1L) "pagamento" else "pagamenti"}"
    override val chartHint = "Premi ▼ per esplorare"
    override val chartHintExploring = "◀ ▶ per muoverti nel tempo"
    override val chartEmpty = "Nessuna visita in questo periodo"
    override fun visitorCount(count: Long, formattedCount: String) = "$formattedCount ${if (count == 1L) "visitatore" else "visitatori"}"
    override fun salesAmount(money: String) = "$money di vendite"
    override fun weekOf(date: String) = "Settimana del $date"

    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Top prodotti"
        RankingOrder.SALES -> "Top per vendite"
    }
    override fun rankingAll(total: Int) = "$total ${if (total == 1) "prodotto" else "prodotti"}"
    override fun rankingRows(first: Int, last: Int, total: Int) = "$first–$last di $total"
    override val rankingScrollHint = "▲▼ per scorrere"
    override val rankingBrowseHint = "▶ per sfogliare"
    override val noWebsites = "Ancora nessun sito in questo account DataFast."

    override val menu = "Menu"
    override val autoRefresh = "Aggiornamento automatico"
    override fun refreshInterval(minutes: Int) = "Ogni $minutes min"
    override val topProducts = "Top prodotti"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Per visitatori"
        RankingOrder.SALES -> "Per vendite"
    }
    override val refreshNow = "Aggiorna ora"
    override val notLoadedYet = "Non ancora caricato"
    override val statusRefreshing = "aggiornamento…"
    override val statusRetrying = "nuovo tentativo a breve"
    override fun statusNext(time: String) = "prossimo $time"
    override val demoData = "Dati demo"
    override val languageSection = "Lingua"
    override val systemLanguage = "Lingua di sistema"
    override val languageName = "Italiano"

    /** A period's name inside a sentence: "Caricamento dati di 30 giorni…", "dati di oggi". */
    private fun inSentence(period: Period) = period(period).lowercase(locale)
}
