package com.marcoelizalde.pulse.traffic.datafast

import com.marcoelizalde.pulse.traffic.domain.AnalyticsException
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.Product
import com.marcoelizalde.pulse.traffic.domain.ProductTraffic
import com.marcoelizalde.pulse.traffic.domain.Sales
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import com.marcoelizalde.pulse.traffic.domain.TimeSpan
import com.marcoelizalde.pulse.traffic.domain.TrafficPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

class DataFastAnalyticsSourceTest {
    private val shop = Product("665f0b3c4d2e1a0012345678", "Shop", "shop.example")
    private val window = ReportWindow(
        start = LocalDate.of(2026, 10, 17),
        end = LocalDate.of(2026, 10, 23),
        zone = ZoneId.of("America/Mexico_City"),
        granularity = Granularity.DAY,
    )

    @Test
    fun `products lists the account websites with the bearer token`() = runTest {
        val transport = FakeTransport(
            "/admin/websites" to HttpResponse(
                200,
                """
                {"status":"success","data":[
                  {"_id":"665f0b3c4d2e1a0012345678","domain":"shop.example","name":"Shop","trackingId":"dfid_1","timezone":"America/Mexico_City","currency":"USD","role":"owner","createdAt":"2026-01-01T00:00:00.000Z"},
                  {"_id":"665f0b3c4d2e1a0012345679","domain":"blog.example","name":null}
                ]}
                """,
            ),
        )

        val products = DataFastAnalyticsSource("dft_secret", transport).products()

        assertEquals(
            listOf(shop, Product("665f0b3c4d2e1a0012345679", "blog.example", "blog.example")),
            products,
        )
        val request = transport.requests.single()
        assertEquals("https://datafa.st/api/v1/admin/websites", request.url)
        assertEquals("Bearer dft_secret", request.headers["Authorization"])
    }

    @Test
    fun `traffic reads the product's unique visitors and trend in one request`() = runTest {
        val transport = FakeTransport(
            "/analytics/timeseries" to HttpResponse(
                200,
                """
                {"status":"success","fields":["visitors"],"interval":"day","timezone":"America/Mexico_City",
                 "totals":{"visitors":1100},
                 "data":[{"timestamp":"2026-10-17T00:00:00Z","visitors":528},{"timestamp":"2026-10-18T00:00:00Z","visitors":610}],
                 "pagination":{"limit":1000,"offset":0,"total":2}}
                """,
            ),
        )

        val traffic = DataFastAnalyticsSource("dft_secret", transport).traffic(shop, window)

        // totals.visitors are unique visitors for the window, so returning visitors are not counted twice.
        assertEquals(
            ProductTraffic(
                visitors = 1100,
                trend = listOf(
                    TrafficPoint(LocalDateTime.of(2026, 10, 17, 0, 0), 528),
                    TrafficPoint(LocalDateTime.of(2026, 10, 18, 0, 0), 610),
                ),
            ),
            traffic,
        )
        assertEquals(listOf("/analytics/timeseries"), transport.requests.map { it.path })
        val timeseries = transport.request("/analytics/timeseries")
        assertEquals(
            mapOf(
                "websiteId" to shop.id,
                "startAt" to "2026-10-17",
                "endAt" to "2026-10-23",
                "timezone" to "America/Mexico_City",
                "fields" to "visitors,revenue,payments",
                "interval" to "day",
                "limit" to "1000",
            ),
            timeseries.query,
        )
        assertTrue(transport.requests.all { it.headers["Authorization"] == "Bearer dft_secret" })
    }

    @Test
    fun `traffic reads the product's sales from the same request`() = runTest {
        val transport = FakeTransport(
            "/analytics/timeseries" to HttpResponse(
                200,
                """
                {"status":"success","fields":["visitors","revenue","payments"],"interval":"day","currency":"$",
                 "totals":{"visitors":43,"revenue":126.4,"revenueBreakdown":{"new":120,"renewal":6.4,"refund":0},"payments":24},
                 "data":[{"timestamp":"2026-10-17T00:00:00Z","visitors":43,"revenue":126.4,"payments":24}]}
                """,
            ),
        )

        val traffic = DataFastAnalyticsSource("dft_secret", transport).traffic(shop, window)

        assertEquals(Sales(cents = 12_640, payments = 24, currency = "$"), traffic.sales)
        assertEquals(listOf(12_640L), traffic.trend.map { it.salesCents })
        assertEquals(listOf("/analytics/timeseries"), transport.requests.map { it.path })
    }

    @Test
    fun `previous visitors come from the overview, with the span sent as UTC instants`() = runTest {
        val transport = FakeTransport(
            "/analytics/overview" to HttpResponse(200, """{"status":"success","data":[{"visitors":37}]}"""),
        )
        val span = TimeSpan(
            start = LocalDateTime.of(2026, 10, 10, 0, 0),
            end = LocalDateTime.of(2026, 10, 16, 12, 30, 15, 500_000_000),
            zone = ZoneId.of("America/Mexico_City"),
        )

        val visitors = DataFastAnalyticsSource("dft_secret", transport).visitors(shop, span)

        assertEquals(37L, visitors)
        val query = transport.request("/analytics/overview").query
        assertEquals(shop.id, query["websiteId"])
        // DataFast reads a timestamp without an offset as UTC, whatever the timezone parameter says.
        assertEquals("2026-10-10T06:00:00Z", query["startAt"])
        assertEquals("2026-10-16T18:30:15Z", query["endAt"])
        assertEquals("visitors", query["fields"])
    }

    @Test
    fun `trend interval follows the window granularity`() = runTest {
        val intervals = Granularity.entries.associateWith { granularity ->
            val transport = FakeTransport(
                "/analytics/overview" to HttpResponse(200, """{"status":"success","data":[]}"""),
                "/analytics/timeseries" to HttpResponse(200, """{"status":"success","data":[]}"""),
            )
            DataFastAnalyticsSource("dft_secret", transport).traffic(shop, window.copy(granularity = granularity))
            transport.request("/analytics/timeseries").query["interval"]
        }

        assertEquals(
            mapOf(Granularity.HOUR to "hour", Granularity.DAY to "day", Granularity.WEEK to "week"),
            intervals,
        )
    }

    @Test
    fun `a product without data has zero visitors`() = runTest {
        val transport = FakeTransport(
            "/analytics/overview" to HttpResponse(200, """{"status":"success","data":[]}"""),
            "/analytics/timeseries" to HttpResponse(200, """{"status":"success","data":[]}"""),
        )

        val traffic = DataFastAnalyticsSource("dft_secret", transport).traffic(shop, window)

        assertEquals(ProductTraffic(0, emptyList()), traffic)
    }

    @Test
    fun `bucket timestamps keep their wall-clock time whatever the format`() = runTest {
        val transport = FakeTransport(
            "/analytics/overview" to HttpResponse(200, """{"status":"success","data":[{"visitors":3}]}"""),
            "/analytics/timeseries" to HttpResponse(
                200,
                """{"status":"success","data":[
                  {"timestamp":"2026-10-23T14:00:00.000Z","visitors":1},
                  {"timestamp":"2026-10-23T15:00:00-06:00","visitors":1},
                  {"timestamp":"2026-10-23T16:00:00","visitors":1},
                  {"timestamp":"2026-10-24","visitors":0}
                ]}""",
            ),
        )

        val trend = DataFastAnalyticsSource("dft_secret", transport).traffic(shop, window).trend

        assertEquals(
            listOf(
                LocalDateTime.of(2026, 10, 23, 14, 0),
                LocalDateTime.of(2026, 10, 23, 15, 0),
                LocalDateTime.of(2026, 10, 23, 16, 0),
                LocalDateTime.of(2026, 10, 24, 0, 0),
            ),
            trend.map { it.start },
        )
    }

    @Test
    fun `an error response becomes an AnalyticsException with DataFast's message`() = runTest {
        val transport = FakeTransport(
            "/admin/websites" to HttpResponse(
                400,
                """{"status":"error","error":{"code":400,"message":"Invalid date range"}}""",
            ),
        )

        val error = runCatching { DataFastAnalyticsSource("dft_secret", transport).products() }.exceptionOrNull()

        assertTrue(error is AnalyticsException)
        assertEquals("DataFast error 400: Invalid date range", error?.message)
    }

    @Test
    fun `a rejected token explains how to fix it`() = runTest {
        val messages = listOf(401, 403).associateWith { status ->
            val transport = FakeTransport("/admin/websites" to HttpResponse(status, """{"status":"error"}"""))
            runCatching { DataFastAnalyticsSource("dft_secret", transport).products() }.exceptionOrNull()?.message
        }

        assertEquals(
            mapOf(
                401 to "DataFast rejected the token (401). Check DATAFAST_TOKEN in local.properties.",
                403 to "The DataFast token needs the analytics:read and websites:read permissions (403).",
            ),
            messages,
        )
    }

    @Test
    fun `the logo configured in DataFast comes from the website metadata`() = runTest {
        val logo = "https://d1aebdcemlt4l7.cloudfront.net/website-logos/zXQEJ4Q0PpOVLLQXkkqh.jpeg"
        val transport = FakeTransport(
            "/analytics/metadata" to HttpResponse(200, """{"status":"success","data":[{"domain":"shop.example","logo":"$logo"}]}"""),
        )

        assertEquals(logo, DataFastAnalyticsSource("dft_secret", transport).logoUrl(shop))
        assertEquals(mapOf("websiteId" to shop.id, "fields" to "logo"), transport.request("/analytics/metadata").query)
    }

    @Test
    fun `a website without a DataFast logo has none`() = runTest {
        val transport = FakeTransport(
            "/analytics/metadata" to HttpResponse(200, """{"status":"success","data":[{"domain":"shop.example","logo":null}]}"""),
        )

        assertEquals(null, DataFastAnalyticsSource("dft_secret", transport).logoUrl(shop))
    }

    @Test
    fun `the website list is reused for an hour`() = runTest {
        val clock = MutableClock(Instant.parse("2026-10-23T18:00:00Z"))
        val transport = FakeTransport(
            "/admin/websites" to HttpResponse(200, """{"status":"success","data":[{"_id":"1","domain":"shop.example"}]}"""),
        )
        val source = DataFastAnalyticsSource("dft_secret", transport, clock = clock)

        source.products()
        clock.advance(Duration.ofMinutes(59))
        source.products()
        assertEquals(1, transport.requests.size)

        clock.advance(Duration.ofMinutes(2))
        source.products()
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun `a rate limit says when DataFast accepts requests again`() = runTest {
        val now = Instant.parse("2026-10-23T18:00:00Z")
        val transport = FakeTransport(
            "/admin/websites" to HttpResponse(
                429,
                """{"error":"Too many requests","message":"Rate limit exceeded...","limit":60,"remaining":0,"reset":${now.epochSecond + 42}}""",
            ),
        )

        val error = runCatching {
            DataFastAnalyticsSource("dft_secret", transport, clock = MutableClock(now)).products()
        }.exceptionOrNull() as AnalyticsException

        assertEquals("DataFast rate limit reached (60 requests per minute).", error.message)
        assertEquals(42.seconds, error.retryAfter)
    }

    @Test
    fun `a network failure becomes an AnalyticsException`() = runTest {
        val transport = HttpTransport { _, _ -> throw IOException("timeout") }

        val error = runCatching { DataFastAnalyticsSource("dft_secret", transport).products() }.exceptionOrNull()

        assertTrue(error is AnalyticsException)
        assertEquals("Can't reach DataFast. Check the TV's internet connection.", error?.message)
    }

    private data class Request(val url: String, val headers: Map<String, String>) {
        val path: String = URI(url).path.removePrefix("/api/v1")
        val query: Map<String, String> = URI(url).rawQuery.orEmpty()
            .split("&")
            .filter { it.isNotEmpty() }
            .associate { pair ->
                val (key, value) = pair.split("=", limit = 2)
                URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }
    }

    private class MutableClock(private var now: Instant) : Clock() {
        fun advance(duration: Duration) {
            now += duration
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
    }

    private class FakeTransport(vararg responses: Pair<String, HttpResponse>) : HttpTransport {
        private val responsesByPath = responses.toMap()
        val requests = mutableListOf<Request>()

        fun request(path: String): Request = requests.single { it.path == path }

        override suspend fun get(url: String, headers: Map<String, String>): HttpResponse {
            val request = Request(url, headers)
            synchronized(requests) { requests += request }
            return responsesByPath.getValue(request.path)
        }
    }
}
