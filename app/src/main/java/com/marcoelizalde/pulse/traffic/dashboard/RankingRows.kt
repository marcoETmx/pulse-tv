package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.RankedProduct

/** One line of the ranking; [rank] is null for the folded "Other" row. */
data class RankingRow(
    val rank: Int?,
    val name: String,
    val detail: String,
    val visitors: Long,
    val share: Double,
)

/** Keeps the ranking within [maxRows] lines by folding the tail into "Other". */
fun rankingRows(ranking: List<RankedProduct>, maxRows: Int): List<RankingRow> {
    val rows = ranking.mapIndexed { index, ranked ->
        RankingRow(
            rank = index + 1,
            name = ranked.product.name,
            detail = ranked.product.domain.takeIf { it != ranked.product.name }.orEmpty(),
            visitors = ranked.visitors,
            share = ranked.share,
        )
    }
    if (rows.size <= maxRows) return rows

    val tail = rows.drop(maxRows - 1)
    val other = RankingRow(null, "Other", "${tail.size} products", tail.sumOf { it.visitors }, tail.sumOf { it.share })
    return rows.take(maxRows - 1) + other
}
