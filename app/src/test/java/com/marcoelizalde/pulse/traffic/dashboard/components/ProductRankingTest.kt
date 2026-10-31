package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductRankingTest {
    @Test
    fun `the ranking shows as many whole rows as the card has room for`() {
        // A row is 44dp tall with 4dp between rows: seven take 332dp, six take 284dp.
        assertEquals(
            listOf(7, 7, 6, 6, 5),
            listOf(340.dp, 332.dp, 331.dp, 284.dp, 283.dp).map(::rowsThatFit),
        )
    }

    @Test
    fun `one row is shown even when there is room for none`() {
        assertEquals(listOf(1, 1), listOf(30.dp, 0.dp).map(::rowsThatFit))
    }
}
