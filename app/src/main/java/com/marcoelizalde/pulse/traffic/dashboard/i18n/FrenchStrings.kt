package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/**
 * French, with numbers and money written as in France ("1 234,50 $", "42 %"). Spaces before units and
 * colons are no-break spaces (U+00A0): the font has no narrow one, and they keep "42 %" on one line.
 */
object FrenchStrings : PulseStrings {
    override val locale: Locale = Locale.forLanguageTag("fr-FR")
    override val groupingSeparator = '\u00A0'
    override val decimalSeparator = ','
    // Unspaced, so the chart's axis labels stay clear of the first bar.
    override val compactUnits = listOf(1_000L to "k", 1_000_000L to "M")
    override val percentSuffix = "\u00A0%"
    override val currencyAfterAmount = true
    override val monthDayPattern = "d MMM"
    override val monthDayYearPattern = "d MMM yyyy"
    override val weekdayMonthDayPattern = "EEE d MMM"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "Aujourd’hui"
        Period.LAST_7_DAYS -> "7 jours"
        Period.LAST_15_DAYS -> "15 jours"
        Period.LAST_30_DAYS -> "30 jours"
        // "Cette année" crowds the header's date range off the screen.
        Period.THIS_YEAR -> "Année"
    }

    override val connecting = "Connexion à DataFast…"
    override val loadFailedTitle = "Impossible de charger DataFast"
    override val tryAgain = "Réessayer"
    override val noData = "Impossible de charger les données"
    override fun updated(time: String) = "Mis à jour à $time"
    override val refreshing = "Actualisation…"
    override fun loadingPeriod(period: Period) = "Chargement ${ofPeriod(period)}…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "Impossible de charger ${inSentence(selected)}\u00A0: $error · affichage ${ofPeriod(shown)}, données de $time"
    override fun refreshFailed(error: String, time: String) = "Impossible d’actualiser\u00A0: $error · données de $time"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "Impossible de joindre DataFast. Vérifiez la connexion Internet de la TV."
        AnalyticsProblem.TokenRejected -> "DataFast a refusé le jeton (401). Vérifiez DATAFAST_TOKEN dans local.properties."
        AnalyticsProblem.MissingPermissions -> "Le jeton DataFast doit avoir les autorisations analytics:read et websites:read (403)."
        is AnalyticsProblem.RateLimited -> "Limite de DataFast atteinte (${problem.requestsPerMinute} requêtes par minute)."
        is AnalyticsProblem.ProviderError -> "Erreur DataFast ${problem.status}" + problem.detail?.let { "\u00A0: $it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFast a envoyé une réponse inattendue."
        AnalyticsProblem.Unknown -> "Une erreur s’est produite lors du chargement du tableau de bord."
    }
    override val demoBadge = "DÉMO"

    override val visitors = "Visiteurs"
    override val sales = "Ventes"
    override val mixedCurrencies = "Plusieurs devises"
    override fun payments(count: Long, formattedCount: String) = "$formattedCount ${if (count <= 1L) "paiement" else "paiements"}"
    override val chartHint = "Appuyez sur ▼ pour explorer"
    override val chartHintExploring = "◀ ▶ pour se déplacer dans le temps"
    override val chartEmpty = "Aucune visite sur cette période"
    override fun visitorCount(count: Long, formattedCount: String) = "$formattedCount ${if (count <= 1L) "visiteur" else "visiteurs"}"
    override fun salesAmount(money: String) = "$money de ventes"
    override fun weekOf(date: String) = "Semaine du $date"

    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Top produits"
        RankingOrder.SALES -> "Top ventes"
    }
    override fun rankingAll(total: Int) = "$total ${if (total <= 1) "produit" else "produits"}"
    override fun rankingRows(first: Int, last: Int, total: Int) = "$first–$last sur $total"
    override val rankingScrollHint = "▲▼ pour défiler"
    override val rankingBrowseHint = "▶ pour parcourir"
    override val noWebsites = "Aucun site dans ce compte DataFast pour l’instant."

    override val menu = "Menu"
    override val autoRefresh = "Actualisation automatique"
    override fun refreshInterval(minutes: Int) = "Toutes les $minutes min"
    override val topProducts = "Top produits"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Par visiteurs"
        RankingOrder.SALES -> "Par ventes"
    }
    override val refreshNow = "Actualiser maintenant"
    override val notLoadedYet = "Pas encore chargé"
    override val statusRefreshing = "actualisation…"
    override val statusRetrying = "nouvel essai bientôt"
    override fun statusNext(time: String) = "prochaine à $time"
    override val demoData = "Données démo"
    override val languageSection = "Langue"
    override val systemLanguage = "Langue du système"
    override val languageName = "Français"

    /** A period's name inside a sentence: "Impossible de charger 30 jours", "cette année". */
    private fun inSentence(period: Period) = when (period) {
        Period.THIS_YEAR -> "cette année"
        else -> period(period).lowercase(locale)
    }

    /** "de" before a period, elided before a vowel: "Chargement de 30 jours…", "affichage d’aujourd’hui". */
    private fun ofPeriod(period: Period) = if (period == Period.TODAY) "d’${inSentence(period)}" else "de ${inSentence(period)}"
}
