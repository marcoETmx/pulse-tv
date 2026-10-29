package com.marcoelizalde.pulse.traffic.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

class LoadTrafficDashboard(
    private val source: AnalyticsSource,
    private val clock: Clock,
    private val maxConcurrentRequests: Int = 4,
) {
    /** Loads every product for [period]; any product failure fails the whole load. */
    suspend operator fun invoke(period: Period): TrafficDashboard = coroutineScope {
        val window = period.windowEndingOn(LocalDate.now(clock), clock.zone)
        val gate = Semaphore(maxConcurrentRequests)
        val traffic = source.products()
            .map { product -> async { gate.withPermit { product to source.traffic(product, window) } } }
            .awaitAll()

        val total = traffic.sumOf { (_, productTraffic) -> productTraffic.visitors }
        val totalSales = addUp(traffic.map { (_, productTraffic) -> productTraffic.sales })
        TrafficDashboard(
            period = period,
            window = window,
            totalVisitors = total,
            totalSales = totalSales,
            ranking = rank(traffic, total),
            trend = mergeTrends(traffic.map { (_, productTraffic) -> productTraffic.trend })
                // Providers return whole days; buckets that have not started would read as zero visits.
                .filterNot { it.start.isAfter(LocalDateTime.now(clock)) }
                // Amounts in different currencies cannot be added into one bar.
                .map { if (totalSales == null) it.copy(salesCents = 0) else it },
            updatedAt = clock.instant(),
        )
    }

    private fun rank(traffic: List<Pair<Product, ProductTraffic>>, total: Long): List<RankedProduct> =
        traffic
            .map { (product, productTraffic) ->
                val share = if (total == 0L) 0.0 else productTraffic.visitors.toDouble() / total
                RankedProduct(product, productTraffic.visitors, share, productTraffic.sales)
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
}
