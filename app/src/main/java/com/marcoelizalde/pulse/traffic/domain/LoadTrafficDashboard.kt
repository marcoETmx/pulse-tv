package com.marcoelizalde.pulse.traffic.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

class LoadTrafficDashboard(
    private val source: AnalyticsSource,
    private val clock: Clock,
    private val maxConcurrentRequests: Int = 4,
) {
    /** Previous visitors by product and start of the compared stretch, with when they were loaded. */
    private val comparisons = ConcurrentHashMap<Pair<Product, LocalDateTime>, Pair<Long, Instant>>()

    /** Loads every product for [period]; any product failure fails the whole load. */
    suspend operator fun invoke(period: Period): TrafficDashboard = coroutineScope {
        // The clock is read once, so the window, the comparison and the chart describe the same moment
        // even when the load runs across midnight.
        val loadedAt = clock.instant()
        val now = LocalDateTime.ofInstant(loadedAt, clock.zone)
        val window = period.windowEndingOn(now.toLocalDate(), clock.zone)
        val gate = Semaphore(maxConcurrentRequests)
        val products = source.products()
        val traffic = products
            .map { product -> async { gate.withPermit { product to source.traffic(product, window) } } }
            .awaitAll()
        // Asked for only once the period itself has loaded, so a rate limit costs the comparison, not the dashboard.
        val span = period.previousSpan(window, now)
        val previous = products
            .map { product -> async { gate.withPermit { product to previousVisitors(product, span, loadedAt) } } }
            .awaitAll()
            .toMap()

        val total = traffic.sumOf { (_, productTraffic) -> productTraffic.visitors }
        val totalSales = addUp(traffic.map { (_, productTraffic) -> productTraffic.sales })
        TrafficDashboard(
            period = period,
            window = window,
            totalVisitors = total,
            totalSales = totalSales,
            ranking = rank(traffic, total, previous),
            trend = mergeTrends(traffic.map { (_, productTraffic) -> productTraffic.trend })
                // Providers return whole days; buckets that have not started would read as zero visits.
                .filterNot { it.start.isAfter(now) }
                // Amounts in different currencies cannot be added into one bar.
                .map { if (totalSales == null) it.copy(salesCents = 0) else it },
            updatedAt = loadedAt,
        )
    }

    /**
     * The comparison is only a hint, so a product whose previous visitors fail to load goes without one, and
     * a loaded one is reused while it is younger than 1% of the compared stretch (at most 30 minutes): a span
     * that only grew that much at its end stays within 1% of a fresh answer, at half the requests per refresh.
     */
    private suspend fun previousVisitors(product: Product, span: TimeSpan, now: Instant): Long? {
        val key = product to span.start
        val reuseFor = minOf(Duration.between(span.start, span.end).dividedBy(100), MAX_COMPARISON_REUSE)
        comparisons[key]?.let { (visitors, loadedAt) ->
            if (Duration.between(loadedAt, now) < reuseFor) return visitors
        }
        val visitors = try {
            source.visitors(product, span)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        comparisons.values.removeIf { (_, loadedAt) -> Duration.between(loadedAt, now) >= MAX_COMPARISON_REUSE }
        comparisons[key] = visitors to now
        return visitors
    }

    private fun rank(
        traffic: List<Pair<Product, ProductTraffic>>,
        total: Long,
        previous: Map<Product, Long?>,
    ): List<RankedProduct> =
        traffic
            .map { (product, productTraffic) ->
                val share = if (total == 0L) 0.0 else productTraffic.visitors.toDouble() / total
                RankedProduct(product, productTraffic.visitors, share, productTraffic.sales, previous[product])
            }
            .sortedWith(compareByDescending<RankedProduct> { it.visitors }.thenBy { it.product.name.lowercase() })

    private fun addUp(sales: List<Sales>): Sales? {
        val currencies = sales.filterNot { it.isEmpty }.mapNotNull { it.currency }.distinct()
        if (currencies.size > 1) return null
        return Sales(
            cents = sales.sumOf { it.cents },
            payments = sales.sumOf { it.payments },
            currency = currencies.singleOrNull() ?: sales.firstNotNullOfOrNull { it.currency },
        )
    }

    private fun mergeTrends(trends: List<List<TrafficPoint>>): List<TrafficPoint> =
        trends.flatten()
            .groupBy { it.start }
            .map { (start, points) -> TrafficPoint(start, points.sumOf { it.visitors }, points.sumOf { it.salesCents }) }
            .sortedBy { it.start }

    private companion object {
        val MAX_COMPARISON_REUSE: Duration = Duration.ofMinutes(30)
    }
}
