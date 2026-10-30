package com.marcoelizalde.pulse.traffic.dashboard.i18n

import com.marcoelizalde.pulse.traffic.dashboard.RankingOrder
import com.marcoelizalde.pulse.traffic.dashboard.RefreshInterval
import com.marcoelizalde.pulse.traffic.domain.AnalyticsProblem
import com.marcoelizalde.pulse.traffic.domain.Period
import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishStringsTest {
    @Test
    fun `periods have short selector labels`() {
        assertEquals(
            listOf("Today", "7 days", "15 days", "30 days", "This year"),
            Period.entries.map(EnglishStrings::period),
        )
    }

    @Test
    fun `refresh intervals have short menu labels`() {
        assertEquals(
            listOf("Every 5 min", "Every 10 min", "Every 15 min"),
            RefreshInterval.entries.map { EnglishStrings.refreshInterval(it.minutes) },
        )
    }

    @Test
    fun `ranking orders have menu labels and a section title`() {
        assertEquals(listOf("By visitors", "By sales"), RankingOrder.entries.map(EnglishStrings::rankingOrder))
        assertEquals(listOf("Top products", "Top by sales"), RankingOrder.entries.map(EnglishStrings::rankingTitle))
    }

    @Test
    fun `analytics problems explain what went wrong and how to fix it`() {
        assertEquals(
            listOf(
                "Can't reach DataFast. Check the TV's internet connection.",
                "DataFast rejected the token (401). Check DATAFAST_TOKEN in local.properties.",
                "The DataFast token needs the analytics:read and websites:read permissions (403).",
                "DataFast rate limit reached (60 requests per minute).",
                "DataFast error 400: Invalid date range",
                "DataFast error 500",
                "DataFast sent an unexpected response.",
                "Something went wrong loading the dashboard.",
            ),
            listOf(
                AnalyticsProblem.Unreachable,
                AnalyticsProblem.TokenRejected,
                AnalyticsProblem.MissingPermissions,
                AnalyticsProblem.RateLimited(requestsPerMinute = 60),
                AnalyticsProblem.ProviderError(400, "Invalid date range"),
                AnalyticsProblem.ProviderError(500, detail = null),
                AnalyticsProblem.UnexpectedResponse,
                AnalyticsProblem.Unknown,
            ).map(EnglishStrings::problem),
        )
    }
}
