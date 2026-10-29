package com.marcoelizalde.pulse.traffic.domain

import java.time.Instant
import java.time.LocalDateTime

data class Product(
    val id: String,
    val name: String,
    val domain: String,
)

/** Visitors and sales in one time bucket; [start] is wall-clock time in the report window's zone. */
data class TrafficPoint(
    val start: LocalDateTime,
    val visitors: Long,
    val salesCents: Long = 0,
)

/** Revenue in cents, so totals add up exactly, and the payments behind it; [currency] is the provider's symbol or code. */
data class Sales(
    val cents: Long,
    val payments: Long,
    val currency: String?,
) {
    val isEmpty: Boolean get() = cents == 0L && payments == 0L

    companion object {
        val NONE = Sales(cents = 0, payments = 0, currency = null)
    }
}

data class ProductTraffic(
    val visitors: Long,
    val trend: List<TrafficPoint>,
    val sales: Sales = Sales.NONE,
)

data class RankedProduct(
    val product: Product,
    val visitors: Long,
    /** Fraction of the dashboard total, from 0.0 to 1.0. */
    val share: Double,
    val sales: Sales = Sales.NONE,
)

data class TrafficDashboard(
    val period: Period,
    val window: ReportWindow,
    val totalVisitors: Long,
    /** Every product's sales added up; null when products report different currencies, which cannot be added. */
    val totalSales: Sales?,
    val ranking: List<RankedProduct>,
    val trend: List<TrafficPoint>,
    val updatedAt: Instant,
)
