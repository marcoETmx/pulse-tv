package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.RankedProduct
import com.marcoelizalde.pulse.traffic.domain.Sales

/** One line of the ranking. */
data class RankingRow(
    val rank: Int,
    /** The product's name, which falls back to its domain when the website has none. */
    val name: String,
    val visitors: Long,
    val share: Double,
    val productId: String,
    /** Null when the product sold nothing in the period. */
    val sales: Sales? = null,
    /** How visitors compare with the same stretch of the previous period; null when equal or unknown. */
    val change: Change? = null,
    /** Bar length against the top product, from 0.0 to 1.0, in the measure the ranking is ordered by. */
    val bar: Double = 0.0,
)

enum class Change { UP, DOWN }

/**
 * The order the ranking can show: [chosen], except sales when there is no [totalSales] because products
 * report different currencies, which cannot be compared.
 */
fun effectiveRankingOrder(chosen: RankingOrder, totalSales: Sales?): RankingOrder =
    if (chosen == RankingOrder.SALES && totalSales == null) RankingOrder.VISITORS else chosen

/**
 * Every product keeps its own row, labelled by name only, in [order] (the domain ranks by visitors). The
 * bar and share follow the same measure; by sales, products that sold the same, nothing included, keep
 * their visitor order.
 */
fun rankingRows(ranking: List<RankedProduct>, order: RankingOrder = RankingOrder.VISITORS): List<RankingRow> {
    val bySales = order == RankingOrder.SALES
    val measure: (RankedProduct) -> Long = if (bySales) { ranked -> ranked.sales.cents } else { ranked -> ranked.visitors }
    // A stable sort, so equal sales stay in visitor order.
    val ordered = if (bySales) ranking.sortedByDescending(measure) else ranking
    val top = ranking.maxOfOrNull(measure)?.takeIf { it > 0 }
    val totalCents = ranking.sumOf { it.sales.cents }
    return ordered.mapIndexed { index, ranked ->
        RankingRow(
            rank = index + 1,
            name = ranked.product.name,
            visitors = ranked.visitors,
            share = when {
                !bySales -> ranked.share
                totalCents > 0 -> ranked.sales.cents.toDouble() / totalCents
                else -> 0.0
            },
            productId = ranked.product.id,
            sales = ranked.sales.takeUnless { it.isEmpty },
            change = ranked.previousVisitors?.let { previous ->
                when {
                    ranked.visitors > previous -> Change.UP
                    ranked.visitors < previous -> Change.DOWN
                    else -> null
                }
            },
            bar = top?.let { (measure(ranked).toDouble() / it).coerceIn(0.0, 1.0) } ?: 0.0,
        )
    }
}

/** Shown in place of a logo: the first letter or digit of [name], ignoring a leading "www.". */
fun monogram(name: String): String =
    name.removePrefix("www.").firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•"
