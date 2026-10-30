package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/** Simplified Chinese, with numbers and dates written as in mainland China ("1.2万", "10月23日周五"). */
object ChineseStrings : PulseStrings {
    override val locale: Locale = Locale.forLanguageTag("zh-CN")
    override val groupingSeparator = ','
    override val decimalSeparator = '.'
    override val compactUnits = listOf(10_000L to "万", 100_000_000L to "亿")
    override val percentSuffix = "%"
    override val currencyAfterAmount = false
    override val monthDayPattern = "M月d日"
    override val monthDayYearPattern = "y年M月d日"
    override val weekdayMonthDayPattern = "M月d日E"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "今天"
        Period.LAST_7_DAYS -> "7天"
        Period.LAST_15_DAYS -> "15天"
        Period.LAST_30_DAYS -> "30天"
        Period.THIS_YEAR -> "今年"
    }

    override val connecting = "正在连接 DataFast…"
    override val loadFailedTitle = "无法加载 DataFast"
    override val tryAgain = "重试"
    override val noData = "无法加载数据"
    override fun updated(time: String) = "更新于 $time"
    override val refreshing = "正在刷新…"
    override fun loadingPeriod(period: Period) = "正在加载${period(period)}的数据…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "无法加载${period(selected)}的数据：$error · 显示${period(shown)}的数据，截至 $time"
    override fun refreshFailed(error: String, time: String) = "无法刷新：$error · 显示截至 $time 的数据"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "无法连接 DataFast。请检查电视的网络连接。"
        AnalyticsProblem.TokenRejected -> "DataFast 拒绝了令牌（401）。请检查 local.properties 中的 DATAFAST_TOKEN。"
        AnalyticsProblem.MissingPermissions -> "DataFast 令牌需要 analytics:read 和 websites:read 权限（403）。"
        is AnalyticsProblem.RateLimited -> "已达到 DataFast 速率限制（每分钟${problem.requestsPerMinute}次请求）。"
        is AnalyticsProblem.ProviderError -> "DataFast 错误 ${problem.status}" + problem.detail?.let { "：$it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFast 返回了异常响应。"
        AnalyticsProblem.Unknown -> "加载仪表盘时出错。"
    }
    override val demoBadge = "演示数据"

    // Chinese has no plural forms, so the counts below never change their words.
    override val visitors = "访客"
    override val sales = "销售额"
    override val mixedCurrencies = "多种货币"
    override fun payments(count: Long, formattedCount: String) = "${formattedCount}笔付款"
    override val chartHint = "按 ▼ 探索"
    override val chartHintExploring = "◀ ▶ 切换时间"
    override val chartEmpty = "此期间没有访问记录"
    override fun visitorCount(count: Long, formattedCount: String) = "${formattedCount}位访客"
    override fun salesAmount(money: String) = "销售额 $money"
    override fun weekOf(date: String) = "${date}起的一周"

    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "热门产品"
        RankingOrder.SALES -> "销售额排行"
    }
    override fun rankingAll(total: Int) = "共${total}个产品"
    override fun rankingRows(first: Int, last: Int, total: Int) = "$first–$last，共${total}个"
    override val rankingScrollHint = "▲▼ 滚动"
    override val rankingBrowseHint = "▶ 浏览"
    override val noWebsites = "此 DataFast 账户中还没有网站。"

    override val menu = "菜单"
    override val autoRefresh = "自动刷新"
    override fun refreshInterval(minutes: Int) = "每${minutes}分钟"
    override val topProducts = "热门产品"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "按访客数"
        RankingOrder.SALES -> "按销售额"
    }
    override val refreshNow = "立即刷新"
    override val notLoadedYet = "尚未加载"
    override val statusRefreshing = "正在刷新…"
    override val statusRetrying = "即将重试"
    override fun statusNext(time: String) = "下次刷新 $time"
    override val demoData = "演示数据"
    override val languageSection = "语言"
    override val systemLanguage = "跟随系统"
    override val languageName = "简体中文"
}
