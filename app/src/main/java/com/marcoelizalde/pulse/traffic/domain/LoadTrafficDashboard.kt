package com.marcoelizalde.pulse.traffic.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Clock
import java.time.LocalDate

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
        TrafficDashboard(
            period = period,
            window = window,
            totalVisitors = total,
            ranking = rank(traffic, total),
            trend = mergeTrends(traffic.map { (_, productTraffic) -> productTraffic.trend }),
            updatedAt = clock.instant(),
        )
    }

    private fun rank(traffic: List<Pair<Product, ProductTraffic>>, total: Long): List<RankedProduct> =
        traffic
            .map { (product, productTraffic) ->
                val share = if (total == 0L) 0.0 else productTraffic.visitors.toDouble() / total
                RankedProduct(product, productTraffic.visitors, share)
            }
            .sortedWith(compareByDescending<RankedProduct> { it.visitors }.thenBy { it.product.name.lowercase() })

    private fun mergeTrends(trends: List<List<TrafficPoint>>): List<TrafficPoint> =
        trends.flatten()
            .groupBy { it.start }
            .map { (start, points) -> TrafficPoint(start, points.sumOf { it.visitors }) }
            .sortedBy { it.start }
}
