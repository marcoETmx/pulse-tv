package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.Product
import com.marcoelizalde.pulse.traffic.domain.RankedProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class RankingRowsTest {
    private fun ranked(count: Int) = (1..count).map { i ->
        RankedProduct(Product("$i", "Product $i", "p$i.example"), visitors = 100L - i, share = 0.1)
    }

    @Test
    fun `every product gets a ranked row when they fit`() {
        val rows = rankingRows(ranked(3), maxRows = 6)

        assertEquals(
            listOf(
                RankingRow(1, "Product 1", "p1.example", 99, 0.1),
                RankingRow(2, "Product 2", "p2.example", 98, 0.1),
                RankingRow(3, "Product 3", "p3.example", 97, 0.1),
            ),
            rows,
        )
    }

    @Test
    fun `the tail folds into one Other row when products do not fit`() {
        val rows = rankingRows(ranked(8), maxRows = 6)

        assertEquals(listOf(1, 2, 3, 4, 5, null), rows.map { it.rank })
        assertEquals(RankingRow(null, "Other", "3 products", 92 + 93 + 94, 0.1 * 3), rows.last())
    }

    @Test
    fun `the domain is hidden when it is already the name`() {
        val rows = rankingRows(listOf(RankedProduct(Product("1", "shop.example", "shop.example"), 5, 1.0)), maxRows = 6)

        assertEquals("", rows.single().detail)
    }
}
