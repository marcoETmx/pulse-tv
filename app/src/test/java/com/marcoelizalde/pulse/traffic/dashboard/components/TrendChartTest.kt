package com.marcoelizalde.pulse.traffic.dashboard.components

import androidx.compose.ui.geometry.Offset
import com.marcoelizalde.pulse.traffic.domain.TrafficPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun offsets(vararg ys: Float) = ys.mapIndexed { index, y -> Offset(index * 30f, y) }

    @Test
    fun `the smooth line passes through every point`() {
        val points = offsets(80f, 20f, 50f, 50f, 0f)

        val segments = smoothLine(points)

        assertEquals(points.drop(1), segments.map { it.end })
    }

    @Test
    fun `the smooth line never rises above or dips below the two points it joins`() {
        val points = offsets(100f, 0f, 90f, 10f, 10f, 60f, 100f, 0f)

        var start = points.first()
        smoothLine(points).forEach { segment ->
            // A cubic Bézier stays inside the hull of its control points.
            val low = minOf(start.y, segment.end.y)
            val high = maxOf(start.y, segment.end.y)
            listOf(segment.c1.y, segment.c2.y).forEach { y -> assertTrue("$y outside $low..$high", y in low..high) }
            start = segment.end
        }
    }

    @Test
    fun `the smooth line stays flat where the visitors do not change`() {
        val segments = smoothLine(offsets(40f, 40f, 40f))

        segments.forEach { assertEquals(listOf(40f, 40f), listOf(it.c1.y, it.c2.y)) }
    }

    @Test
    fun `a single point needs no curve`() {
        assertTrue(smoothLine(offsets(40f)).isEmpty())
    }
}
