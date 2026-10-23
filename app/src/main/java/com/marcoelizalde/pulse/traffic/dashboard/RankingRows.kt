package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.RankedProduct

/** One line of the ranking. */
data class RankingRow(
    val rank: Int,
    val name: String,
    val detail: String,
    val visitors: Long,
    val share: Double,
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
        )
    }
