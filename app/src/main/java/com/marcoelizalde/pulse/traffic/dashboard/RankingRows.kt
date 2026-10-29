package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.RankedProduct
import com.marcoelizalde.pulse.traffic.domain.Sales

/** One line of the ranking. */
data class RankingRow(
    val rank: Int,
    val name: String,
    val detail: String,
    val visitors: Long,
    val share: Double,
    val productId: String,
    /** Null when the product sold nothing in the period. */
    val sales: Sales? = null,
)

/** Every product keeps its own row; the domain is shown only when it adds information. */
fun rankingRows(ranking: List<RankedProduct>): List<RankingRow> =
    ranking.mapIndexed { index, ranked ->
        RankingRow(
            rank = index + 1,
            name = ranked.product.name,
            detail = ranked.product.domain.takeIf { it != ranked.product.name }.orEmpty(),
            visitors = ranked.visitors,
            share = ranked.share,
            productId = ranked.product.id,
            sales = ranked.sales.takeUnless { it.isEmpty },
        )
    }

/** Shown in place of a logo: the first letter or digit of [name], ignoring a leading "www.". */
fun monogram(name: String): String =
    name.removePrefix("www.").firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•"
