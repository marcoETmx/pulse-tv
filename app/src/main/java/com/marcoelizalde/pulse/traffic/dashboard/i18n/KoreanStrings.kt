package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import java.util.Locale

/** Korean, with large numbers shortened by 천, 만 and 억 ("1.2만") and dates such as "10월 23일 (금)". */
object KoreanStrings : PulseStrings {
    override val locale: Locale = Locale.forLanguageTag("ko-KR")
    override val groupingSeparator = ','
    override val decimalSeparator = '.'
    override val compactUnits = listOf(1_000L to "천", 10_000L to "만", 100_000_000L to "억")
    override val percentSuffix = "%"
    override val currencyAfterAmount = false
    override val monthDayPattern = "MMM d일"
    override val monthDayYearPattern = "y년 MMM d일"
    override val weekdayMonthDayPattern = "MMM d일 (E)"

    override fun period(period: Period): String = when (period) {
        Period.TODAY -> "오늘"
        Period.LAST_7_DAYS -> "7일"
        Period.LAST_15_DAYS -> "15일"
        Period.LAST_30_DAYS -> "30일"
        Period.THIS_YEAR -> "올해"
    }

    override val connecting = "DataFast에 연결 중…"
    override val loadFailedTitle = "DataFast를 불러오지 못했습니다"
    override val tryAgain = "다시 시도"
    override val noData = "데이터를 불러오지 못했습니다"
    override fun updated(time: String) = "업데이트 $time"
    override val refreshing = "새로고침 중…"
    override fun loadingPeriod(period: Period) = "${inSentence(period)} 데이터를 불러오는 중…"
    override fun periodFailed(selected: Period, error: String, shown: Period, time: String) =
        "${inSentence(selected)} 데이터를 불러오지 못했습니다: $error · $time 기준 ${inSentence(shown)} 데이터를 표시합니다"
    override fun refreshFailed(error: String, time: String) = "새로고침하지 못했습니다: $error · $time 기준 데이터를 표시합니다"
    override fun problem(problem: AnalyticsProblem) = when (problem) {
        AnalyticsProblem.Unreachable -> "DataFast에 연결할 수 없습니다. TV의 인터넷 연결을 확인하세요."
        AnalyticsProblem.TokenRejected -> "DataFast가 토큰을 거부했습니다(401). local.properties의 DATAFAST_TOKEN 값을 확인하세요."
        AnalyticsProblem.MissingPermissions -> "DataFast 토큰에 analytics:read 및 websites:read 권한이 필요합니다(403)."
        is AnalyticsProblem.RateLimited -> "DataFast 요청 한도에 도달했습니다(분당 ${problem.requestsPerMinute}회)."
        is AnalyticsProblem.ProviderError -> "DataFast 오류 ${problem.status}" + problem.detail?.let { ": $it" }.orEmpty()
        AnalyticsProblem.UnexpectedResponse -> "DataFast에서 예상치 못한 응답을 받았습니다."
        AnalyticsProblem.Unknown -> "대시보드를 불러오는 중 문제가 발생했습니다."
    }
    override val demoBadge = "데모"

    override val visitors = "방문자"
    override val sales = "매출"
    override val mixedCurrencies = "여러 통화"

    // Counts take a counter word (건, 명, 개); Korean has no plural, so the number never changes the wording.
    override fun payments(count: Long, formattedCount: String) = "결제 ${formattedCount}건"
    override val chartHint = "▼ 버튼으로 탐색"
    override val chartHintExploring = "◀ ▶ 버튼으로 시간 이동"
    override val chartEmpty = "이 기간에 기록된 방문이 없습니다"
    override fun visitorCount(count: Long, formattedCount: String) = "방문자 ${formattedCount}명"
    override fun salesAmount(money: String) = "매출 $money"
    override fun weekOf(date: String) = "$date 주"

    override fun rankingTitle(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "인기 제품"
        RankingOrder.SALES -> "매출 순위"
    }
    override fun rankingAll(total: Int) = "제품 ${total}개"
    override fun rankingRows(first: Int, last: Int, total: Int) = "${total}개 중 $first–$last"
    override val rankingScrollHint = "▲▼ 스크롤"
    override val rankingBrowseHint = "▶ 둘러보기"
    override val noWebsites = "이 DataFast 계정에 아직 웹사이트가 없습니다."

    override val menu = "메뉴"
    override val autoRefresh = "자동 새로고침"
    override fun refreshInterval(minutes: Int) = "${minutes}분마다"
    override val topProducts = "인기 제품"
    override fun rankingOrder(order: RankingOrder) = when (order) {
        RankingOrder.VISITORS -> "방문자순"
        RankingOrder.SALES -> "매출순"
    }
    override val refreshNow = "지금 새로고침"
    override val notLoadedYet = "아직 불러오지 않음"
    override val statusRefreshing = "새로고침 중…"
    override val statusRetrying = "곧 다시 시도"
    override fun statusNext(time: String) = "다음 $time"
    override val demoData = "데모 데이터"
    override val languageSection = "언어"
    override val systemLanguage = "시스템 언어"
    override val languageName = "한국어"

    /** A period's name inside a sentence; "최근" keeps "7일" from reading as the 7th of the month. */
    private fun inSentence(period: Period) = when (period) {
        Period.TODAY, Period.THIS_YEAR -> period(period)
        Period.LAST_7_DAYS, Period.LAST_15_DAYS, Period.LAST_30_DAYS -> "최근 ${period(period)}"
    }
}
