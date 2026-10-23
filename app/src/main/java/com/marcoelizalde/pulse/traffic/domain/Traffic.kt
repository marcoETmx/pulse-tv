package com.marcoelizalde.pulse.traffic.domain

import java.time.Instant
import java.time.LocalDateTime

data class Product(
    val id: String,
    val name: String,
    val domain: String,
)

/** Visitors in one time bucket; [start] is wall-clock time in the report window's zone. */
data class TrafficPoint(
    val start: LocalDateTime,
    val visitors: Long,
)

data class ProductTraffic(
    val visitors: Long,
    val trend: List<TrafficPoint>,
)

data class RankedProduct(
    val product: Product,
    val visitors: Long,
    /** Fraction of the dashboard total, from 0.0 to 1.0. */
    val share: Double,
)

data class TrafficDashboard(
    val period: Period,
    val window: ReportWindow,
    val totalVisitors: Long,
    val ranking: List<RankedProduct>,
    val trend: List<TrafficPoint>,
    val updatedAt: Instant,
)
