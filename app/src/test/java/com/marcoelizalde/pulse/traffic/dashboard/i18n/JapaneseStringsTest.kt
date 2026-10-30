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

class JapaneseStringsTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private val formats = Formats(JapaneseStrings, zone)
    private val updatedAt = Instant.parse("2026-10-23T07:14:00Z")
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
    fun `labels are in Japanese`() {
        assertEquals(listOf("今日", "7日間", "15日間", "30日間", "今年"), Period.entries.map(JapaneseStrings::period))
        assertEquals(listOf("訪問者数順", "売上順"), RankingOrder.entries.map(JapaneseStrings::rankingOrder))
        assertEquals(listOf("人気ランキング", "売上ランキング"), RankingOrder.entries.map(JapaneseStrings::rankingTitle))
        assertEquals("10分ごと", JapaneseStrings.refreshInterval(10))
        assertEquals(listOf("日本語", "システムの言語"), listOf(JapaneseStrings.languageName, JapaneseStrings.systemLanguage))
    }

    @Test
    fun `counts take a counter word and never a plural`() {
        assertEquals(listOf("決済 0件", "決済 1件", "決済 1,240件"), listOf(0L, 1L, 1_240L).map(formats::payments))
        assertEquals(listOf("訪問者 1人", "訪問者 2人"), listOf(1L, 2L).map { JapaneseStrings.visitorCount(it, it.toString()) })
        assertEquals(listOf("全1件", "全5件", "1–6 / 全10件"), listOf(
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 1),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 5),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 10),
        ))
    }

    @Test
    fun `large numbers are shortened by 万 and 億`() {
        assertEquals(listOf("1,234", "1.2万", "100万", "1.2億"), listOf(1_234L, 12_345L, 999_950L, 123_456_789L).map(formats::compact))
        assertEquals(listOf("12,450", "42%"), listOf(formats.grouped(12_450), formats.share(0.4231)))
    }

    @Test
    fun `money puts the currency before the amount`() {
        assertEquals(
            listOf("$139", "$1,234.50", "MXN 120", "$1.5万"),
            listOf(formats.money(13_900, "$"), formats.money(123_450, "$"), formats.money(12_000, "MXN"), formats.moneyCompact(15_000, "$")),
        )
    }

    @Test
    fun `dates put the month first, with kanji and the weekday in brackets`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("10月23日(金)", formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("10月17日 – 10月23日", formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals("2025年12月20日 – 2026年1月5日", formats.range(window(LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 5))))
        assertEquals(listOf("10月23日(金)", "09:05"), LocalDateTime.of(2026, 10, 23, 9, 5).let { listOf(formats.clockDate(it), formats.clockTime(it)) })
    }

    @Test
    fun `chart buckets are labelled in Japanese`() {
        val start = LocalDateTime.of(2026, 10, 21, 0, 0)

        assertEquals("10月21日", formats.bucketTick(start, Granularity.DAY))
        assertEquals("10月21日(水)", formats.bucketDetail(start, Granularity.DAY))
        assertEquals("10月21日の週", formats.bucketDetail(start, Granularity.WEEK))
        assertEquals("09:00 – 10:00", formats.bucketDetail(start.withHour(9), Granularity.HOUR))
    }

    @Test
    fun `the status line reads naturally in Japanese`() {
        val fresh = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null)
        val stale = DashboardUiState(Period.LAST_30_DAYS, dashboard, isLoading = false, error = AnalyticsProblem.ProviderError(500, detail = null))

        assertEquals("16:14に更新", statusLine(fresh, formats))
        assertEquals("30日間のデータを読み込み中…", statusLine(stale.copy(isLoading = true, error = null), formats))
        assertEquals("30日間を読み込めませんでした：DataFastエラー 500 · 今日（16:14時点）を表示中", statusLine(stale, formats))
        assertEquals("16:14に更新 · 次回 16:19", menuStatus(fresh, formats))
        assertEquals("16:14に更新 · 読み込み中…", menuStatus(stale.copy(isLoading = true, error = null), formats))
    }

    @Test
    fun `problems are explained politely, keeping names and codes as they are`() {
        assertEquals(
            "DataFastに接続できません。テレビのインターネット接続を確認してください。",
            JapaneseStrings.problem(AnalyticsProblem.Unreachable),
        )
        assertEquals(
            "DataFastがトークンを拒否しました（401）。local.propertiesのDATAFAST_TOKENを確認してください。",
            JapaneseStrings.problem(AnalyticsProblem.TokenRejected),
        )
        assertEquals("DataFastの利用制限に達しました（1分あたり60リクエスト）。", JapaneseStrings.problem(AnalyticsProblem.RateLimited(60)))
        assertEquals("DataFastエラー 400：Invalid date range", JapaneseStrings.problem(AnalyticsProblem.ProviderError(400, "Invalid date range")))
    }
}
