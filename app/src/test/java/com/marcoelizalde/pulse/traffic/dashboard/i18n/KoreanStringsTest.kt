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

class KoreanStringsTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val formats = Formats(KoreanStrings, zone)
    private val updatedAt = Instant.parse("2026-10-23T05:14:00Z")
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
    fun `labels are in Korean`() {
        assertEquals(listOf("오늘", "7일", "15일", "30일", "올해"), Period.entries.map(KoreanStrings::period))
        assertEquals(listOf("방문자순", "매출순"), RankingOrder.entries.map(KoreanStrings::rankingOrder))
        assertEquals(listOf("인기 제품", "매출 순위"), RankingOrder.entries.map(KoreanStrings::rankingTitle))
        assertEquals("10분마다", KoreanStrings.refreshInterval(10))
        assertEquals(listOf("한국어", "시스템 언어"), listOf(KoreanStrings.languageName, KoreanStrings.systemLanguage))
    }

    @Test
    fun `counts use counter words, the same for any number`() {
        assertEquals(listOf("결제 0건", "결제 1건", "결제 1,240건"), listOf(0L, 1L, 1_240L).map(formats::payments))
        assertEquals(listOf("방문자 1명", "방문자 2명"), listOf(1L, 2L).map { KoreanStrings.visitorCount(it, it.toString()) })
        assertEquals(listOf("제품 1개", "제품 5개", "10개 중 1–6"), listOf(
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 1),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 5),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 10),
        ))
    }

    @Test
    fun `large numbers are shortened by thousands, ten thousands and hundred millions`() {
        assertEquals(
            listOf("999", "1.2천", "1만", "1.2만", "1.2억"),
            listOf(999L, 1_234L, 9_999L, 12_345L, 123_456_789L).map(formats::compact),
        )
        assertEquals("$1.5만", formats.moneyCompact(15_000, "$"))
    }

    @Test
    fun `numbers and money are written as in Korea`() {
        assertEquals(listOf("12,450", "42%"), listOf(formats.grouped(12_450), formats.share(0.4231)))
        assertEquals(
            listOf("$139", "$1,234.50", "KRW 120"),
            listOf(formats.money(13_900, "$"), formats.money(123_450, "$"), formats.money(12_000, "KRW")),
        )
        assertEquals("매출 $139", KoreanStrings.salesAmount(formats.money(13_900, "$")))
    }

    @Test
    fun `dates write the month and day with 월 and 일, and the weekday in parentheses`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("10월 23일 (금)", formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("10월 17일 – 10월 23일", formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals("2025년 12월 28일 – 2026년 1월 3일", formats.range(window(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 3))))
        assertEquals("10월 23일 (금)", formats.clockDate(LocalDateTime.of(2026, 10, 23, 9, 5)))
        assertEquals("09:05", formats.clockTime(LocalDateTime.of(2026, 10, 23, 9, 5)))
    }

    @Test
    fun `chart buckets are described in Korean`() {
        assertEquals("14:00 – 15:00", formats.bucketDetail(LocalDateTime.of(2026, 10, 23, 14, 0), Granularity.HOUR))
        assertEquals("10월 21일 (수)", formats.bucketDetail(LocalDateTime.of(2026, 10, 21, 0, 0), Granularity.DAY))
        assertEquals("10월 21일 주", formats.bucketDetail(LocalDateTime.of(2026, 10, 21, 0, 0), Granularity.WEEK))
    }

    @Test
    fun `the status line reads naturally in Korean`() {
        val stale = DashboardUiState(Period.LAST_30_DAYS, dashboard, isLoading = false, error = AnalyticsProblem.ProviderError(500, detail = null))

        assertEquals("업데이트 14:14", statusLine(DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null), formats))
        assertEquals("최근 30일 데이터를 불러오는 중…", statusLine(stale.copy(isLoading = true, error = null), formats))
        assertEquals("올해 데이터를 불러오는 중…", KoreanStrings.loadingPeriod(Period.THIS_YEAR))
        assertEquals(
            "최근 30일 데이터를 불러오지 못했습니다: DataFast 오류 500 · 14:14 기준 오늘 데이터를 표시합니다",
            statusLine(stale, formats),
        )
        assertEquals("업데이트 14:14 · 새로고침 중…", menuStatus(stale.copy(isLoading = true, error = null), formats))
        assertEquals("업데이트 14:14 · 다음 14:19", menuStatus(DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null), formats))
    }

    @Test
    fun `problems are explained in Korean`() {
        assertEquals("DataFast에 연결할 수 없습니다. TV의 인터넷 연결을 확인하세요.", KoreanStrings.problem(AnalyticsProblem.Unreachable))
        assertEquals(
            "DataFast가 토큰을 거부했습니다(401). local.properties의 DATAFAST_TOKEN 값을 확인하세요.",
            KoreanStrings.problem(AnalyticsProblem.TokenRejected),
        )
        assertEquals("DataFast 요청 한도에 도달했습니다(분당 60회).", KoreanStrings.problem(AnalyticsProblem.RateLimited(60)))
        assertEquals("DataFast 오류 400: Invalid date range", KoreanStrings.problem(AnalyticsProblem.ProviderError(400, "Invalid date range")))
    }
}
