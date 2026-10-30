package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.DashboardUiState
import com.marcoelizalde.pulse.traffic.dashboard.Formats
import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.dashboard.RefreshInterval
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

class ChineseStringsTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val formats = Formats(ChineseStrings, zone)
    private val updatedAt = Instant.parse("2026-10-23T08:14:00Z")
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
    fun `labels are in Simplified Chinese`() {
        assertEquals(listOf("今天", "7天", "15天", "30天", "今年"), Period.entries.map(ChineseStrings::period))
        assertEquals(listOf("按访客数", "按销售额"), RankingOrder.entries.map(ChineseStrings::rankingOrder))
        assertEquals("每10分钟", ChineseStrings.refreshInterval(10))
        assertEquals(listOf("语言", "跟随系统", "简体中文"), listOf(ChineseStrings.languageSection, ChineseStrings.systemLanguage, ChineseStrings.languageName))
    }

    @Test
    fun `counts read the same for any number`() {
        assertEquals(listOf("0笔付款", "1笔付款", "1,240笔付款"), listOf(0L, 1L, 1_240L).map(formats::payments))
        assertEquals(listOf("1位访客", "2位访客"), listOf(1L, 2L).map { ChineseStrings.visitorCount(it, it.toString()) })
    }

    @Test
    fun `the ranking header names the order and the rows`() {
        assertEquals(listOf("热门产品", "销售额排行"), RankingOrder.entries.map(ChineseStrings::rankingTitle))
        assertEquals(listOf("共1个产品", "共5个产品", "1–6，共10个"), listOf(
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 1),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 5),
            formats.rankingRange(firstVisible = 0, visibleRows = 6, total = 10),
        ))
    }

    @Test
    fun `numbers shorten by ten thousands and money is written as in mainland China`() {
        assertEquals(listOf("12,450", "42%"), listOf(formats.grouped(12_450), formats.share(0.4231)))
        assertEquals(
            listOf("1,234", "1.2万", "100万", "1.2亿"),
            listOf(1_234L, 12_345L, 999_950L, 123_456_789L).map(formats::compact),
        )
        assertEquals(
            listOf("$1,234.50", "$139", "CNY 120"),
            listOf(formats.money(123_450, "$"), formats.money(13_900, "$"), formats.money(12_000, "CNY")),
        )
        assertEquals("销售额 $139", ChineseStrings.salesAmount(formats.money(13_900, "$")))
    }

    @Test
    fun `dates are written month first, with Chinese weekdays`() {
        fun window(start: LocalDate, end: LocalDate) = ReportWindow(start, end, zone, Granularity.DAY)

        assertEquals("10月23日周五", formats.range(window(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23))))
        assertEquals("10月17日 – 10月23日", formats.range(window(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 23))))
        assertEquals("2025年12月29日 – 2026年1月4日", formats.range(window(LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4))))
        assertEquals("10月23日周五", formats.clockDate(LocalDateTime.of(2026, 10, 23, 16, 14)))
    }

    @Test
    fun `chart buckets are described in Chinese`() {
        val start = LocalDateTime.of(2026, 10, 21, 0, 0)

        assertEquals("10月21日", formats.bucketTick(start, Granularity.DAY))
        assertEquals("00:00 – 01:00", formats.bucketDetail(start, Granularity.HOUR))
        assertEquals("10月21日周三", formats.bucketDetail(start, Granularity.DAY))
        assertEquals("10月21日起的一周", formats.bucketDetail(start, Granularity.WEEK))
    }

    @Test
    fun `the status line reads naturally in Chinese`() {
        val stale = DashboardUiState(Period.LAST_30_DAYS, dashboard, isLoading = false, error = AnalyticsProblem.ProviderError(500, detail = null))
        val upToDate = DashboardUiState(Period.TODAY, dashboard, isLoading = false, error = null, refreshInterval = RefreshInterval.FIVE_MINUTES)

        assertEquals("更新于 16:14", statusLine(upToDate, formats))
        assertEquals("正在加载30天的数据…", statusLine(stale.copy(isLoading = true, error = null), formats))
        assertEquals("无法加载30天的数据：DataFast 错误 500 · 显示今天的数据，截至 16:14", statusLine(stale, formats))
        assertEquals("更新于 16:14 · 正在刷新…", menuStatus(stale.copy(isLoading = true, error = null), formats))
        assertEquals("更新于 16:14 · 下次刷新 16:19", menuStatus(upToDate, formats))
    }

    @Test
    fun `problems are explained in Chinese`() {
        assertEquals("无法连接 DataFast。请检查电视的网络连接。", ChineseStrings.problem(AnalyticsProblem.Unreachable))
        assertEquals(
            "DataFast 拒绝了令牌（401）。请检查 local.properties 中的 DATAFAST_TOKEN。",
            ChineseStrings.problem(AnalyticsProblem.TokenRejected),
        )
        assertEquals("已达到 DataFast 速率限制（每分钟60次请求）。", ChineseStrings.problem(AnalyticsProblem.RateLimited(60)))
        assertEquals("DataFast 错误 400：Invalid date range", ChineseStrings.problem(AnalyticsProblem.ProviderError(400, "Invalid date range")))
    }
}
