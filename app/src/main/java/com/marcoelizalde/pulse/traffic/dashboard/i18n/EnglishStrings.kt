package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

object EnglishStrings : PulseStrings {
    override val locale: Locale = Locale.US
    override val groupingSeparator = ','
    override val decimalSeparator = '.'
    override val thousandsSuffix = "K"
    override val millionsSuffix = "M"
    override val percentSuffix = "%"
    override val currencyAfterAmount = false
    override val monthDayPattern = "MMM d"
    override val monthDayYearPattern = "MMM d, yyyy"
    override val weekdayMonthDayPattern = "EEE, MMM d"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "Today"
        Period.LAST_7_DAYS -> "7 days"
        Period.LAST_15_DAYS -> "15 days"
        Period.LAST_30_DAYS -> "30 days"
        Period.THIS_YEAR -> "This year"
    }

    override val connecting = "Connecting to DataFast…"
    override val loadFailedTitle = "Couldn't load DataFast"
    override val tryAgain = "Try again"
    override val noData = "Couldn't load data"
    override fun updated(time: String) = "Updated $time"
    override val refreshing = "Refreshing…"
    override fun loadingPeriod(period: Period) = "Loading ${inSentence(period)}…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "Couldn't load ${inSentence(selected)}: $error · showing ${inSentence(shown)} from $time"
    override fun refreshFailed(error: String, time: String) = "Couldn't refresh: $error · showing data from $time"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "Can't reach DataFast. Check the TV's internet connection."
        AnalyticsProblem.TokenRejected -> "DataFast rejected the token (401). Check DATAFAST_TOKEN in local.properties."
        AnalyticsProblem.MissingPermissions -> "The DataFast token needs the analytics:read and websites:read permissions (403)."
        is AnalyticsProblem.RateLimited -> "DataFast rate limit reached (${problem.requestsPerMinute} requests per minute)."
        is AnalyticsProblem.ProviderError -> "DataFast error ${problem.status}" + problem.detail?.let { ": $it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFast sent an unexpected response."
        AnalyticsProblem.Unknown -> "Something went wrong loading the dashboard."
    }
    override val demoBadge = "DEMO DATA"

    override val visitors = "Visitors"
    override val sales = "Sales"
    override val mixedCurrencies = "Mixed currencies"
    override fun payments(count: Long, formattedCount: String) = "$formattedCount ${if (count == 1L) "payment" else "payments"}"
    override val chartHint = "Press ▼ to explore"
    override val chartHintExploring = "◀ ▶ move through time"
    override val chartEmpty = "No visits recorded in this period"
    override fun visitorCount(count: Long, formattedCount: String) = "$formattedCount visitors"
    override fun salesAmount(money: String) = "$money in sales"
    override fun weekOf(date: String) = "Week of $date"

    // Both titles fit beside the row range; the title names the order only when it is not the usual one.
    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "Top products"
        RankingOrder.SALES -> "Top by sales"
    }
    override fun rankingAll(total: Int) = "$total tracked"
    override fun rankingRows(first: Int, last: Int, total: Int) = "$first–$last of $total"
    override val rankingScrollHint = "▲▼ to scroll"
    override val rankingBrowseHint = "▶ to browse"
    override val noWebsites = "No websites in this DataFast account yet."

    override val menu = "Menu"
    override val autoRefresh = "Auto refresh"
    override fun refreshInterval(minutes: Int) = "Every $minutes min"
    override val topProducts = "Top products"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "By visitors"
        RankingOrder.SALES -> "By sales"
    }
    override val refreshNow = "Refresh now"
    override val notLoadedYet = "Not loaded yet"
    override val statusRefreshing = "refreshing…"
    override val statusRetrying = "retrying soon"
    override fun statusNext(time: String) = "next $time"
    override val demoData = "Demo data"

    /** A period's name inside a sentence: "Loading 30 days…", "showing today from 14:05". */
    private fun inSentence(period: Period) = period(period).lowercase()
}
