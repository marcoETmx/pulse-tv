package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/**
 * Every text the dashboard shows, in one language. Each language implements all of it, so a missing
 * translation fails the build instead of falling back to English. Numbers and dates arrive already
 * formatted; a language only decides the words around them and their order.
 */
interface PulseStrings {
    // How numbers and dates are written. Month and weekday names come from [locale].
    val locale: Locale
    val groupingSeparator: Char
    val decimalSeparator: Char
    val thousandsSuffix: String
    val millionsSuffix: String
    val percentSuffix: String
    val currencyAfterAmount: Boolean
    val monthDayPattern: String
    val monthDayYearPattern: String
    val weekdayMonthDayPattern: String

    // Header and status line
    fun period(period: Period): String
    val connecting: String
    val loadFailedTitle: String
    val tryAgain: String
    val noData: String
    fun updated(time: String): String
    val refreshing: String
    fun loadingPeriod(period: Period): String
    fun periodFailed(selected: Period, error: String, shown: Period, time: String): String
    fun refreshFailed(error: String, time: String): String
    fun problem(problem: AnalyticsProblem): String
    val demoBadge: String

    // Visitors card and chart
    val visitors: String
    val sales: String
    val mixedCurrencies: String
    fun payments(count: Long, formattedCount: String): String
    val chartHint: String
    val chartHintExploring: String
    val chartEmpty: String
    fun visitorCount(count: Long, formattedCount: String): String
    fun salesAmount(money: String): String
    fun weekOf(date: String): String

    // Product ranking
    fun rankingTitle(order: RankingOrder): String
    fun rankingAll(total: Int): String
    fun rankingRows(first: Int, last: Int, total: Int): String
    val rankingScrollHint: String
    val rankingBrowseHint: String
    val noWebsites: String

    // Menu
    val menu: String
    val autoRefresh: String
    fun refreshInterval(minutes: Int): String
    val topProducts: String
    fun rankingOrder(order: RankingOrder): String
    val refreshNow: String
    val notLoadedYet: String
    val statusRefreshing: String
    val statusRetrying: String
    fun statusNext(time: String): String
    val demoData: String
}
