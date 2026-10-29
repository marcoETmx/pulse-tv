package com.marcoelizalde.pulse.traffic.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class LoadTrafficDashboardTest {
    private val zone = ZoneId.of("America/Mexico_City")
    private val now = Instant.parse("2026-10-23T18:00:00Z")
    private val clock = Clock.fixed(now, zone)

    private val shop = Product("1", "Shop", "shop.example")
    private val blog = Product("2", "Blog", "blog.example")
    private val app = Product("3", "App", "app.example")

    private val monday = LocalDateTime.of(2026, 10, 19, 0, 0)
    private val tuesday = LocalDateTime.of(2026, 10, 20, 0, 0)

    @Test
    fun `total visitors is the sum of every product`() = runTest {
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(120, emptyList()),
            blog to ProductTraffic(30, emptyList()),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertEquals(150L, dashboard.totalVisitors)
    }

    @Test
    fun `total sales add up every product's revenue and payments`() = runTest {
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(120, emptyList(), Sales(cents = 13_900, payments = 1, currency = "$")),
            blog to ProductTraffic(30, emptyList(), Sales(cents = 862, payments = 2, currency = "$")),
            app to ProductTraffic(10, emptyList(), Sales(cents = 0, payments = 0, currency = "$")),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertEquals(Sales(cents = 14_762, payments = 3, currency = "$"), dashboard.totalSales)
    }

    @Test
    fun `sales in different currencies have no total`() = runTest {
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(120, emptyList(), Sales(cents = 13_900, payments = 1, currency = "$")),
            blog to ProductTraffic(30, emptyList(), Sales(cents = 500, payments = 1, currency = "€")),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertNull(dashboard.totalSales)
    }

    @Test
    fun `each ranked product keeps its own sales`() = runTest {
        val shopSales = Sales(cents = 13_900, payments = 1, currency = "$")
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(120, emptyList(), shopSales),
            blog to ProductTraffic(30, emptyList()),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertEquals(listOf(shopSales, Sales.NONE), dashboard.ranking.map { it.sales })
    }

    @Test
    fun `ranking orders products by visitors with their share of the total`() = runTest {
        val source = FakeAnalyticsSource(
            blog to ProductTraffic(25, emptyList()),
            shop to ProductTraffic(50, emptyList()),
            app to ProductTraffic(25, emptyList()),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertEquals(
            listOf(
                RankedProduct(shop, 50, 0.5),
                RankedProduct(app, 25, 0.25),
                RankedProduct(blog, 25, 0.25),
            ),
            dashboard.ranking,
        )
    }

    @Test
    fun `share is zero when nobody visited`() = runTest {
        val source = FakeAnalyticsSource(shop to ProductTraffic(0, emptyList()))

        val dashboard = LoadTrafficDashboard(source, clock)(Period.TODAY)

        assertEquals(listOf(RankedProduct(shop, 0, 0.0)), dashboard.ranking)
    }

    @Test
    fun `trend adds every product's visitors per bucket in time order`() = runTest {
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(15, listOf(TrafficPoint(tuesday, 10), TrafficPoint(monday, 5))),
            blog to ProductTraffic(3, listOf(TrafficPoint(monday, 1), TrafficPoint(tuesday, 2))),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertEquals(listOf(TrafficPoint(monday, 6), TrafficPoint(tuesday, 12)), dashboard.trend)
    }

    @Test
    fun `trend adds every product's sales per bucket`() = runTest {
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(15, listOf(TrafficPoint(monday, 5, salesCents = 900), TrafficPoint(tuesday, 10)), dollars(900)),
            blog to ProductTraffic(3, listOf(TrafficPoint(monday, 1, salesCents = 250), TrafficPoint(tuesday, 2, salesCents = 100)), dollars(350)),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertEquals(listOf(1_150L, 100L), dashboard.trend.map { it.salesCents })
    }

    @Test
    fun `sales in different currencies are left out of the trend`() = runTest {
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(5, listOf(TrafficPoint(monday, 5, salesCents = 900)), dollars(900)),
            blog to ProductTraffic(1, listOf(TrafficPoint(monday, 1, salesCents = 500)), Sales(500, 1, "€")),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_7_DAYS)

        assertEquals(listOf(TrafficPoint(monday, 6, salesCents = 0)), dashboard.trend)
    }

    @Test
    fun `trend leaves out buckets that have not started yet`() = runTest {
        // The clock reads 12:00 in Mexico City; DataFast returns every hour of the day.
        val hours = (10..14).map { TrafficPoint(LocalDateTime.of(2026, 10, 23, it, 0), 1) }
        val source = FakeAnalyticsSource(shop to ProductTraffic(3, hours))

        val dashboard = LoadTrafficDashboard(source, clock)(Period.TODAY)

        assertEquals((10..12).map { LocalDateTime.of(2026, 10, 23, it, 0) }, dashboard.trend.map { it.start })
    }

    @Test
    fun `every product is queried with the selected period's window`() = runTest {
        val source = FakeAnalyticsSource(
            shop to ProductTraffic(1, emptyList()),
            blog to ProductTraffic(1, emptyList()),
        )

        val dashboard = LoadTrafficDashboard(source, clock)(Period.LAST_30_DAYS)

        val expected = ReportWindow(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 10, 23), zone, Granularity.DAY)
        assertEquals(Period.LAST_30_DAYS, dashboard.period)
        assertEquals(expected, dashboard.window)
        assertEquals(mapOf(shop to expected, blog to expected), source.requestedWindows)
    }

    @Test
    fun `dashboard is stamped with the load time`() = runTest {
        val source = FakeAnalyticsSource(shop to ProductTraffic(1, emptyList()))

        val dashboard = LoadTrafficDashboard(source, clock)(Period.TODAY)

        assertEquals(now, dashboard.updatedAt)
    }

    @Test
    fun `a failing product fails the whole load`() = runTest {
        val source = FakeAnalyticsSource(shop to ProductTraffic(1, emptyList()), failing = setOf(blog))

        val result = runCatching { LoadTrafficDashboard(source, clock)(Period.TODAY) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    private fun dollars(cents: Long) = Sales(cents, payments = 1, currency = "$")

    private class FakeAnalyticsSource(
        vararg traffic: Pair<Product, ProductTraffic>,
        private val failing: Set<Product> = emptySet(),
    ) : AnalyticsSource {
        private val trafficByProduct = traffic.toMap()
        val requestedWindows = mutableMapOf<Product, ReportWindow>()

        override suspend fun products(): List<Product> = trafficByProduct.keys.toList() + failing

        override suspend fun traffic(product: Product, window: ReportWindow): ProductTraffic {
            if (product in failing) error("boom")
            requestedWindows[product] = window
            return trafficByProduct.getValue(product)
        }
    }
}
