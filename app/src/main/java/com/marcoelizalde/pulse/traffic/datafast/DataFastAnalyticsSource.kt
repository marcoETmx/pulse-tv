package com.marcoelizalde.pulse.traffic.datafast

import com.marcoelizalde.pulse.traffic.domain.AnalyticsException
import com.marcoelizalde.pulse.traffic.domain.AnalyticsSource
import com.marcoelizalde.pulse.traffic.domain.Granularity
import com.marcoelizalde.pulse.traffic.domain.Product
import com.marcoelizalde.pulse.traffic.domain.ProductTraffic
import com.marcoelizalde.pulse.traffic.domain.ReportWindow
import com.marcoelizalde.pulse.traffic.domain.Sales
import com.marcoelizalde.pulse.traffic.domain.TimeSpan
import com.marcoelizalde.pulse.traffic.domain.TrafficPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URLEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.toKotlinDuration

/** Reads every website of a DataFast account with a read-only `dft_` account token. */
class DataFastAnalyticsSource(
    private val token: String,
    private val transport: HttpTransport,
    private val baseUrl: String = "https://datafa.st/api/v1",
    private val clock: Clock = Clock.systemUTC(),
) : AnalyticsSource {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedProducts: Pair<List<Product>, Instant>? = null

    /** Websites rarely change, so the list is reused for an hour to save rate-limit budget. */
    override suspend fun products(): List<Product> {
        cachedProducts?.let { (products, fetchedAt) ->
            if (Duration.between(fetchedAt, clock.instant()) < PRODUCTS_TTL) return products
        }
        val products = get<Envelope<List<WebsiteDto>>>("/admin/websites").data.orEmpty().map { website ->
            Product(
                id = website.id,
                name = website.name?.takeIf { it.isNotBlank() } ?: website.domain,
                domain = website.domain,
            )
        }
        cachedProducts = products to clock.instant()
        return products
    }

    /** The logo URL configured for [product] in DataFast's website settings, or null. */
    suspend fun logoUrl(product: Product): String? =
        get<Envelope<List<MetadataDto>>>("/analytics/metadata", listOf("websiteId" to product.id, "fields" to "logo"))
            .data?.firstOrNull()?.logo?.takeIf { it.isNotBlank() }

    /**
     * One request per product: `totals.visitors` are the window's unique visitors, as in `/analytics/overview`,
     * and `totals.revenue` / `totals.payments` its sales.
     */
    override suspend fun traffic(product: Product, window: ReportWindow): ProductTraffic {
        val timeseries = get<Envelope<List<BucketDto>>>(
            "/analytics/timeseries",
            listOf(
                "websiteId" to product.id,
                "startAt" to window.start.toString(),
                "endAt" to window.end.toString(),
                "timezone" to window.zone.id,
                "fields" to "visitors,revenue,payments",
                "interval" to window.granularity.apiValue,
                "limit" to "1000",
            ),
        )
        val buckets = timeseries.data.orEmpty()
        return ProductTraffic(
            visitors = timeseries.totals?.visitors ?: buckets.sumOf { it.visitors },
            trend = buckets.map {
                TrafficPoint(parseBucketStart(it.timestamp), it.visitors, salesCents = (it.revenue * 100).roundToLong())
            },
            sales = Sales(
                cents = ((timeseries.totals?.revenue ?: buckets.sumOf { it.revenue }) * 100).roundToLong(),
                payments = timeseries.totals?.payments ?: buckets.sumOf { it.payments },
                currency = timeseries.currency,
            ),
        )
    }

    /** Unique visitors in [span], from `/analytics/overview`. */
    override suspend fun visitors(product: Product, span: TimeSpan): Long =
        get<Envelope<List<OverviewDto>>>(
            "/analytics/overview",
            listOf(
                "websiteId" to product.id,
                "startAt" to utcInstant(span.start, span.zone),
                "endAt" to utcInstant(span.end, span.zone),
                "fields" to "visitors",
            ),
        ).data?.firstOrNull()?.visitors ?: 0

    private suspend inline fun <reified T : Envelope<*>> get(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
    ): T {
        val response = try {
            transport.get(url(path, query), mapOf("Authorization" to "Bearer $token", "Accept" to "application/json"))
        } catch (e: IOException) {
            throw AnalyticsException("Can't reach DataFast. Check the TV's internet connection.", e)
        }
        if (response.status !in 200..299) throw errorFor(response)
        val envelope = try {
            json.decodeFromString<T>(response.body)
        } catch (e: SerializationException) {
            throw AnalyticsException(UNEXPECTED_RESPONSE, e)
        } catch (e: IllegalArgumentException) {
            throw AnalyticsException(UNEXPECTED_RESPONSE, e)
        }
        if (envelope.status == "error") throw errorFor(response)
        return envelope
    }

    private fun errorFor(response: HttpResponse): AnalyticsException = when (response.status) {
        401 -> AnalyticsException("DataFast rejected the token (401). Check DATAFAST_TOKEN in local.properties.")
        403 -> AnalyticsException("The DataFast token needs the analytics:read and websites:read permissions (403).")
        429 -> {
            val limit = runCatching { json.decodeFromString<RateLimitDto>(response.body) }.getOrNull()
            val retryAfter = limit?.reset?.let { reset ->
                Duration.between(clock.instant(), Instant.ofEpochSecond(reset)).toKotlinDuration().coerceAtLeast(ZERO)
            }
            AnalyticsException("DataFast rate limit reached (${limit?.limit ?: 60} requests per minute).", retryAfter = retryAfter)
        }
        else -> {
            val detail = runCatching { json.decodeFromString<ErrorEnvelope>(response.body).error?.message }.getOrNull()
            AnalyticsException("DataFast error ${response.status}" + detail?.let { ": $it" }.orEmpty())
        }
    }

    private fun url(path: String, query: List<Pair<String, String>>): String {
        if (query.isEmpty()) return baseUrl + path
        return baseUrl + path + "?" + query.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val UNEXPECTED_RESPONSE = "DataFast sent an unexpected response."
        val PRODUCTS_TTL: Duration = Duration.ofHours(1)
    }
}

private val Granularity.apiValue: String
    get() = when (this) {
        Granularity.HOUR -> "hour"
        Granularity.DAY -> "day"
        Granularity.WEEK -> "week"
    }

/**
 * DataFast labels buckets with their wall-clock start in the requested timezone
 * (a New York day bucket arrives as `2026-05-01T00:00:00Z`), so the offset is dropped, not applied.
 */
private fun parseBucketStart(timestamp: String): LocalDateTime =
    runCatching { OffsetDateTime.parse(timestamp).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(timestamp) }
        .recoverCatching { LocalDate.parse(timestamp).atStartOfDay() }
        .getOrElse { throw AnalyticsException("DataFast sent an unexpected response.", it) }

/** DataFast reads a timestamp without an offset as UTC, whatever the timezone parameter says. */
private fun utcInstant(time: LocalDateTime, zone: ZoneId): String =
    time.atZone(zone).toInstant().truncatedTo(ChronoUnit.SECONDS).toString()

@Serializable
private data class Envelope<T>(
    val status: String? = null,
    val data: T? = null,
    val totals: TotalsDto? = null,
    val currency: String? = null,
)

@Serializable
private data class TotalsDto(val visitors: Long? = null, val revenue: Double? = null, val payments: Long? = null)

@Serializable
private data class WebsiteDto(@SerialName("_id") val id: String, val domain: String, val name: String? = null)

@Serializable
private data class OverviewDto(val visitors: Long = 0)

@Serializable
private data class MetadataDto(val logo: String? = null)

@Serializable
private data class BucketDto(val timestamp: String, val visitors: Long = 0, val revenue: Double = 0.0, val payments: Long = 0)

@Serializable
private data class ErrorEnvelope(val error: ErrorDto? = null)

@Serializable
private data class ErrorDto(val message: String? = null)

/** DataFast's 429 body: `{"error":"Too many requests","limit":60,"remaining":0,"reset":<epoch seconds>}`. */
@Serializable
private data class RateLimitDto(val limit: Int? = null, val reset: Long? = null)
