package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.domain.Product
import com.marcoelizalde.pulse.traffic.domain.RankedProduct
import com.marcoelizalde.pulse.traffic.domain.Sales
import org.junit.Assert.assertEquals
import org.junit.Test

class RankingRowsTest {
    private fun ranked(count: Int) = (1..count).map { i ->
        RankedProduct(Product("$i", "Product $i", "p$i.example"), visitors = 100L - i, share = 0.1)
    }

    @Test
    fun `every product gets a ranked row`() {
        val rows = rankingRows(ranked(3))

        assertEquals(
            listOf(
                RankingRow(1, "Product 1", "p1.example", 99, 0.1, productId = "1"),
                RankingRow(2, "Product 2", "p2.example", 98, 0.1, productId = "2"),
                RankingRow(3, "Product 3", "p3.example", 97, 0.1, productId = "3"),
            ),
            rows,
        )
    }

    @Test
    fun `a row shows the product's sales only when it sold something`() {
        val sold = Sales(cents = 13_900, payments = 1, currency = "$")
        val ranking = listOf(
            RankedProduct(Product("1", "Shop", "shop.example"), visitors = 20, share = 0.5, sales = sold),
            RankedProduct(Product("2", "Blog", "blog.example"), visitors = 20, share = 0.5, sales = Sales(0, 0, "$")),
        )

        assertEquals(listOf(sold, null), rankingRows(ranking).map { it.sales })
    }

    /** S3: every website keeps its own ranked row (regression for verifier finding B1). */
    @Test
    fun `every product keeps its own ranked row with seven websites`() {
        val rows = rankingRows(ranked(7))

        assertEquals((1..7).map { "Product $it" }, rows.map { it.name })
        assertEquals((1..7).toList(), rows.map { it.rank })
    }

    @Test
    fun `the domain is hidden when it is already the name`() {
        val rows = rankingRows(listOf(RankedProduct(Product("1", "shop.example", "shop.example"), 5, 1.0)))

        assertEquals("", rows.single().detail)
    }

    @Test
    fun `the monogram is the first letter or digit of the name`() {
        assertEquals(
            listOf("M", "D", "C", "4", "•"),
            listOf("marcoelizalde.com", "www.devzap.io", "ClaudeFlow", "42 Studio", "—").map(::monogram),
        )
    }
}
