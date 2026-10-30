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
                RankingRow(1, "Product 1", 99, 0.1, productId = "1", bar = 1.0),
                RankingRow(2, "Product 2", 98, 0.1, productId = "2", bar = 98 / 99.0),
                RankingRow(3, "Product 3", 97, 0.1, productId = "3", bar = 97 / 99.0),
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
    fun `a row shows only the product name, never its domain`() {
        val rows = rankingRows(listOf(RankedProduct(Product("1", "Shop", "shop.example"), 5, 1.0)))

        assertEquals(listOf(RankingRow(1, "Shop", 5, 1.0, productId = "1", bar = 1.0)), rows)
    }

    @Test
    fun `the monogram is the first letter or digit of the name`() {
        assertEquals(
            listOf("M", "D", "C", "4", "•"),
            listOf("marcoelizalde.com", "www.devzap.io", "ClaudeFlow", "42 Studio", "—").map(::monogram),
        )
    }

    @Test
    fun `a row points up or down by how its visitors compare with the previous period`() {
        fun change(visitors: Long, previous: Long?) =
            rankingRows(listOf(RankedProduct(Product("1", "Shop", "shop.example"), visitors, 1.0, previousVisitors = previous)))
                .single().change

        assertEquals(
            listOf(Change.UP, Change.DOWN, null, null),
            listOf(change(12, previous = 10), change(8, previous = 10), change(10, previous = 10), change(10, previous = null)),
        )
    }

    private fun sold(cents: Long) = Sales(cents, payments = 1, currency = "$")

    /** In the domain's visitor order. */
    private val byVisitors = listOf(
        RankedProduct(Product("1", "Flowtion", "f.example"), visitors = 300, share = 0.6),
        RankedProduct(Product("2", "DevZap", "d.example"), visitors = 100, share = 0.2),
        RankedProduct(Product("3", "Braix", "b.example"), visitors = 60, share = 0.12, sales = sold(12_600)),
        RankedProduct(Product("4", "ShipList", "s.example"), visitors = 40, share = 0.08, sales = sold(13_900)),
    )

    @Test
    fun `by sales, rows follow the sales and products that sold the same keep their visitor order`() {
        val rows = rankingRows(byVisitors, RankingOrder.SALES)

        assertEquals(listOf("ShipList", "Braix", "Flowtion", "DevZap"), rows.map { it.name })
        assertEquals(listOf(1, 2, 3, 4), rows.map { it.rank })
    }

    @Test
    fun `by sales, the bar and the share follow each product's part of the sales`() {
        val rows = rankingRows(byVisitors, RankingOrder.SALES)

        assertEquals(listOf(13_900 / 26_500.0, 12_600 / 26_500.0, 0.0, 0.0), rows.map { it.share })
        assertEquals(listOf(1.0, 12_600 / 13_900.0, 0.0, 0.0), rows.map { it.bar })
        // The visitors and the sale amounts stay as they were.
        assertEquals(listOf(40L, 60L, 300L, 100L), rows.map { it.visitors })
        assertEquals(listOf(sold(13_900), sold(12_600), null, null), rows.map { it.sales })
    }

    @Test
    fun `by visitors, the bar is each product's visitors against the top product`() {
        val rows = rankingRows(byVisitors, RankingOrder.VISITORS)

        assertEquals(listOf("Flowtion", "DevZap", "Braix", "ShipList"), rows.map { it.name })
        assertEquals(listOf(0.6, 0.2, 0.12, 0.08), rows.map { it.share })
        assertEquals(listOf(1.0, 100 / 300.0, 60 / 300.0, 40 / 300.0), rows.map { it.bar })
    }

    @Test
    fun `sales in different currencies cannot be compared, so the ranking stays by visitors`() {
        assertEquals(
            listOf(RankingOrder.VISITORS, RankingOrder.SALES, RankingOrder.VISITORS),
            listOf(
                effectiveRankingOrder(RankingOrder.SALES, totalSales = null),
                effectiveRankingOrder(RankingOrder.SALES, totalSales = sold(26_500)),
                effectiveRankingOrder(RankingOrder.VISITORS, totalSales = null),
            ),
        )
    }
}
