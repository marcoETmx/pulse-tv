package com.marcoelizalde.pulse.traffic.dashboard.components

import com.marcoelizalde.pulse.traffic.domain.TrafficPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TrendChartTest {
    private fun hours(vararg hours: Int) = hours.map { TrafficPoint(LocalDateTime.of(2026, 10, 23, it, 0), visitors = 1) }

    @Test
    fun `the cursor stays on its bucket when a refresh brings new data`() {
        val refreshed = hours(10, 11, 12, 13)

        assertEquals(1, cursorIndex(refreshed, cursorStart = LocalDateTime.of(2026, 10, 23, 11, 0)))
    }

    @Test
    fun `a cursor that was never moved sits on the last bucket`() {
        assertEquals(3, cursorIndex(hours(10, 11, 12, 13), cursorStart = null))
    }

    @Test
    fun `a cursor whose bucket is gone goes back to the last bucket`() {
        assertEquals(2, cursorIndex(hours(10, 11, 12), cursorStart = LocalDateTime.of(2026, 10, 22, 11, 0)))
    }
}
