package com.marcoelizalde.pulse.traffic.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class ComparisonReuseTest {
    private val zone = ZoneId.of("America/Mexico_City")
    private val noon = Instant.parse("2026-10-23T18:00:00Z") // 12:00 in Mexico City
    private val shop = Product("1", "Shop", "shop.example")
    private val blog = Product("2", "Blog", "blog.example")

    /** A value unique to each product and compared stretch start. */
    private fun valueOf(product: Product, start: LocalDateTime): Long =
        product.id.toLong() * 1_000_000 + start.toLocalDate().toEpochDay()

    private inner class SpanSource(private val products: List<Product>) : AnalyticsSource {
        val visitorsCalls = AtomicInteger()
        @Volatile var failing = false

        override suspend fun products(): List<Product> = products

        override suspend fun traffic(product: Product, window: ReportWindow) = ProductTraffic(1, emptyList())

        override suspend fun visitors(product: Product, span: TimeSpan): Long {
            visitorsCalls.incrementAndGet()
            if (failing) error("boom")
            return valueOf(product, span.start)
        }
    }

    private class MutableClock(var now: Instant, private val zone: ZoneId) : Clock() {
        fun advance(duration: Duration) {
            now += duration
        }

        override fun instant(): Instant = now
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    }

    private fun expectedStart(period: Period, clock: Clock): LocalDateTime =
        period.previousSpan(period.windowEndingOn(LocalDate.now(clock), zone), LocalDateTime.now(clock)).start

    @Test
    fun `every period keeps its own comparison, and reloads reuse the right one`() = runTest {
        val clock = MutableClock(noon, zone)
        val source = SpanSource(listOf(shop, blog))
        val load = LoadTrafficDashboard(source, clock)

        val first = Period.entries.associateWith { period -> load(period).ranking.associate { it.product to it.previousVisitors } }
        clock.advance(Duration.ofMinutes(5))
        val second = Period.entries.associateWith { period -> load(period).ranking.associate { it.product to it.previousVisitors } }

        assertEquals(10, source.visitorsCalls.get())
        assertEquals(first, second)
        for (period in Period.entries) {
            for (product in listOf(shop, blog)) {
                assertEquals("$period $product", valueOf(product, expectedStart(period, clock)), second.getValue(period)[product])
            }
        }
    }

    @Test
    fun `today at noon reuses its comparison while younger than 1 percent of the current stretch`() = runTest {
        // Age a is reused while a < (12 h + a) / 100, so up to 436.36 s.
        val clock = MutableClock(noon, zone)
        val source = SpanSource(listOf(shop))
        val load = LoadTrafficDashboard(source, clock)

        load(Period.TODAY)
        clock.advance(Duration.ofSeconds(436))
        load(Period.TODAY)
        assertEquals(1, source.visitorsCalls.get())

        clock.advance(Duration.ofSeconds(1))
        load(Period.TODAY)
        assertEquals(2, source.visitorsCalls.get())
    }

    @Test
    fun `a new day asks again even within the reuse time`() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-24T05:50:00Z"), zone) // 23:50
        val source = SpanSource(listOf(shop))
        val load = LoadTrafficDashboard(source, clock)

        load(Period.LAST_7_DAYS)
        clock.advance(Duration.ofMinutes(20)) // 00:10 the next day
        val next = load(Period.LAST_7_DAYS)

        assertEquals(2, source.visitorsCalls.get())
        assertEquals(valueOf(shop, expectedStart(Period.LAST_7_DAYS, clock)), next.ranking.single().previousVisitors)
    }

    @Test
    fun `an expired comparison is not shown when asking again fails, and a failure is never reused`() = runTest {
        val clock = MutableClock(noon, zone)
        val source = SpanSource(listOf(shop))
        val load = LoadTrafficDashboard(source, clock)

        load(Period.LAST_7_DAYS)
        clock.advance(Duration.ofMinutes(31))
        source.failing = true
        assertNull(load(Period.LAST_7_DAYS).ranking.single().previousVisitors)
        clock.advance(Duration.ofMinutes(1))
        assertNull(load(Period.LAST_7_DAYS).ranking.single().previousVisitors)
        source.failing = false
        clock.advance(Duration.ofMinutes(1))
        load(Period.LAST_7_DAYS)
        clock.advance(Duration.ofMinutes(1))
        load(Period.LAST_7_DAYS)

        assertEquals(4, source.visitorsCalls.get())
    }

    @Test
    fun `pruning keeps comparisons that are still valid`() = runTest {
        val clock = MutableClock(noon, zone)
        val source = SpanSource(listOf(shop))
        val load = LoadTrafficDashboard(source, clock)

        load(Period.LAST_7_DAYS)
        clock.advance(Duration.ofMinutes(20))
        load(Period.LAST_30_DAYS) // stores a new comparison and prunes
        clock.advance(Duration.ofMinutes(9))
        load(Period.LAST_7_DAYS)

        assertEquals(2, source.visitorsCalls.get())
    }

    @Test
    fun `parallel products on real threads keep their own comparison while pruning runs`() = runBlocking(Dispatchers.Default) {
        val products = (1..300).map { Product("$it", "P$it", "p$it.example") }
        val clock = MutableClock(noon, zone)
        val random = Random(7)
        val source = object : AnalyticsSource {
            val calls = AtomicInteger()
            override suspend fun products() = products
            override suspend fun traffic(product: Product, window: ReportWindow) = ProductTraffic(1, emptyList())
            override suspend fun visitors(product: Product, span: TimeSpan): Long {
                calls.incrementAndGet()
                withContext(Dispatchers.IO) { Thread.sleep(random.nextLong(0, 3)) }
                return valueOf(product, span.start)
            }
        }
        val load = LoadTrafficDashboard(source, clock, maxConcurrentRequests = 64)

        load(Period.LAST_30_DAYS)
        clock.advance(Duration.ofMinutes(31)) // the LAST_30_DAYS entries are now prunable
        val first = load(Period.LAST_7_DAYS).ranking.associate { it.product to it.previousVisitors }
        clock.advance(Duration.ofMinutes(1))
        val second = load(Period.LAST_7_DAYS).ranking.associate { it.product to it.previousVisitors }

        assertEquals(600, source.calls.get())
        assertEquals(first, second)
        val start = expectedStart(Period.LAST_7_DAYS, clock)
        products.forEach { assertEquals(valueOf(it, start), second[it]) }
    }
}
