package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/** Japanese, with large numbers shortened by 万 and 億 ("1.2万") and dates such as "10月23日(金)". */
object JapaneseStrings : PulseStrings {
    override val locale: Locale = Locale.forLanguageTag("ja-JP")
    override val groupingSeparator = ','
    override val decimalSeparator = '.'
    override val compactUnits = listOf(10_000L to "万", 100_000_000L to "億")
    override val percentSuffix = "%"
    override val currencyAfterAmount = false
    override val monthDayPattern = "M月d日"
    override val monthDayYearPattern = "y年M月d日"
    override val weekdayMonthDayPattern = "M月d日(E)"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "今日"
        Period.LAST_7_DAYS -> "7日間"
        Period.LAST_15_DAYS -> "15日間"
        Period.LAST_30_DAYS -> "30日間"
        Period.THIS_YEAR -> "今年"
    }

    override val connecting = "DataFastに接続中…"
    override val loadFailedTitle = "DataFastを読み込めませんでした"
    override val tryAgain = "再試行"
    override val noData = "データを読み込めませんでした"
    override fun updated(time: String) = "${time}に更新"
    override val refreshing = "更新中…"
    override fun loadingPeriod(period: Period) = "${period(period)}のデータを読み込み中…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "${period(selected)}を読み込めませんでした：$error · ${period(shown)}（${time}時点）を表示中"
    override fun refreshFailed(error: String, time: String) = "更新できませんでした：$error · ${time}時点のデータを表示中"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "DataFastに接続できません。テレビのインターネット接続を確認してください。"
        AnalyticsProblem.TokenRejected -> "DataFastがトークンを拒否しました（401）。local.propertiesのDATAFAST_TOKENを確認してください。"
        AnalyticsProblem.MissingPermissions -> "DataFastのトークンにanalytics:readとwebsites:readの権限が必要です（403）。"
        is AnalyticsProblem.RateLimited -> "DataFastの利用制限に達しました（1分あたり${problem.requestsPerMinute}リクエスト）。"
        is AnalyticsProblem.ProviderError -> "DataFastエラー ${problem.status}" + problem.detail?.let { "：$it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFastから予期しない応答がありました。"
        AnalyticsProblem.Unknown -> "ダッシュボードの読み込み中に問題が発生しました。"
    }
    override val demoBadge = "デモデータ"

    override val visitors = "訪問者"
    override val sales = "売上"
    override val mixedCurrencies = "複数の通貨"
    // Japanese has no plural: counts take a counter word (件, 人) and never change the words around them.
    override fun payments(count: Long, formattedCount: String) = "決済 ${formattedCount}件"
    override val chartHint = "▼で詳しく見る"
    override val chartHintExploring = "◀ ▶で時間を移動"
    override val chartEmpty = "この期間の訪問はありません"
    override fun visitorCount(count: Long, formattedCount: String) = "訪問者 ${formattedCount}人"
    override fun salesAmount(money: String) = "売上 $money"
    override fun weekOf(date: String) = "${date}の週"

    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "人気ランキング"
        RankingOrder.SALES -> "売上ランキング"
    }
    override fun rankingAll(total: Int) = "全${total}件"
    override fun rankingRows(first: Int, last: Int, total: Int) = "$first–$last / 全${total}件"
    override val rankingScrollHint = "▲▼でスクロール"
    override val rankingBrowseHint = "▶で一覧を見る"
    override val noWebsites = "このDataFastアカウントにはまだサイトがありません。"

    override val menu = "メニュー"
    override val autoRefresh = "自動更新"
    override fun refreshInterval(minutes: Int) = "${minutes}分ごと"
    override val topProducts = "ランキング"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "訪問者数順"
        RankingOrder.SALES -> "売上順"
    }
    override val refreshNow = "今すぐ更新"
    override val notLoadedYet = "まだ読み込まれていません"
    override val statusRefreshing = "読み込み中…"
    override val statusRetrying = "まもなく再試行"
    override fun statusNext(time: String) = "次回 $time"
    override val demoData = "デモデータ"
    override val languageSection = "言語"
    override val systemLanguage = "システムの言語"
    override val languageName = "日本語"
}
