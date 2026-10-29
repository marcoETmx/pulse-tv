package com.marcoelizalde.pulse.traffic.domain

/** Port to whatever analytics provider tracks the products. */
interface AnalyticsSource {
    suspend fun products(): List<Product>

    suspend fun traffic(product: Product, window: ReportWindow): ProductTraffic

    /** Unique visitors of [product] during [span]. */
    suspend fun visitors(product: Product, span: TimeSpan): Long
}
