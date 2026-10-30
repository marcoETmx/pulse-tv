package com.marcoelizalde.pulse.traffic.datafast

import com.marcoelizalde.pulse.net.HttpResponse
import com.marcoelizalde.pulse.net.HttpTransport
import com.marcoelizalde.pulse.traffic.domain.Product
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

class RateLimiterTest {
    private val shop = Product("665f0b3c4d2e1a0012345678", "Shop", "shop.example")
    private val ok = HttpResponse(200, """{"status":"success","data":[{"logo":null}]}""")

    /** No [perMillis]-long window holds more than [max] of [times]. */
    private fun assertWindow(times: List<Long>, max: Int, perMillis: Long) {
        val sorted = times.sorted()
        for (i in max until sorted.size) {
            assertTrue(
                "requests ${i - max} and $i are ${sorted[i] - sorted[i - max]} apart: $sorted",
                sorted[i] - sorted[i - max] >= perMillis,
            )
        }
    }

    @Test
    fun `random concurrent callers and cancellations never exceed the budget and nobody is left waiting`() = runTest {
        repeat(30) { seed ->
            val random = Random(seed)
            val sentAt = mutableListOf<Long>()
            val transport = HttpTransport { _, _ ->
                sentAt += testScheduler.currentTime
                delay(random.nextLong(0, 5_000))
                ok
            }
            val source = DataFastAnalyticsSource(
                "dft_secret",
                transport,
                rateLimiter = RateLimiter(maxRequests = 3, per = 1.minutes, timeSource = testScheduler.timeSource),
            )
            val jobs = (1..40).map {
                val startAfter = random.nextLong(0, 300_000)
                launch {
                    delay(startAfter)
                    source.logoUrl(shop)
                }
            }
            jobs.filter { random.nextInt(3) == 0 }.forEach { job ->
                val at = random.nextLong(0, 900_000)
                launch {
                    delay(at)
                    job.cancel()
                }
            }
            advanceUntilIdle()

            assertTrue("seed $seed: a caller is stuck", jobs.all { it.isCompleted })
            assertWindow(sentAt, max = 3, perMillis = 60_000)
            assertTrue(sentAt.size >= jobs.count { !it.isCancelled })
        }
    }

    @Test
    fun `waiters go out in arrival order and cancelled ones free their turn`() = runTest {
        val limiter = RateLimiter(maxRequests = 2, per = 1.minutes, timeSource = testScheduler.timeSource)
        val order = mutableListOf<Int>()
        val sentAt = mutableListOf<Long>()
        val jobs = (0 until 10).map { i ->
            launch {
                delay(i * 10L)
                limiter.acquire()
                order += i
                sentAt += testScheduler.currentTime
            }
        }
        advanceTimeBy(1_000)
        jobs[2].cancel() // holds the lock, delaying
        jobs[5].cancel() // queued on the lock
        advanceUntilIdle()

        assertEquals(listOf(0, 1, 3, 4, 6, 7, 8, 9), order)
        assertEquals(listOf(0L, 10L, 60_000L, 60_010L, 120_000L, 120_010L, 180_000L, 180_010L), sentAt)
    }

    @Test
    fun `a caller cancelled right after acquiring keeps its slot counted and nobody waits forever`() = runTest {
        val limiter = RateLimiter(maxRequests = 1, per = 1.minutes, timeSource = testScheduler.timeSource)
        val first = launch {
            limiter.acquire()
            awaitCancellation()
        }
        runCurrent()
        first.cancel()
        val second = async {
            limiter.acquire()
            testScheduler.currentTime
        }
        advanceUntilIdle()

        assertEquals(60_000L, second.await())
    }

    @Test
    fun `requests that fail still count against the window`() = runTest {
        // Failures that are not tried again; network failures and 5xx retries are covered in DataFastAnalyticsSourceTest.
        val failures = listOf<suspend () -> HttpResponse>(
            { HttpResponse(400, """{"status":"error","error":{"message":"Invalid date range"}}""") },
            { HttpResponse(429, """{"error":"Too many requests","limit":60,"remaining":0,"reset":0}""") },
            { HttpResponse(200, """{"status":"error","error":{"message":"nope"}}""") },
        )
        for (failure in failures) {
            val start = testScheduler.currentTime
            val sentAt = mutableListOf<Long>()
            var calls = 0
            val transport = HttpTransport { _, _ ->
                sentAt += testScheduler.currentTime - start
                if (calls++ < 2) failure() else ok
            }
            val source = DataFastAnalyticsSource(
                "dft_secret",
                transport,
                rateLimiter = RateLimiter(maxRequests = 2, per = 1.minutes, timeSource = testScheduler.timeSource),
            )
            repeat(2) { assertTrue(runCatching { source.logoUrl(shop) }.isFailure) }
            source.logoUrl(shop)

            assertEquals(listOf(0L, 0L, 60_000L), sentAt)
        }
    }

    @Test
    fun `a cached website list spends no budget`() = runTest {
        val transport = HttpTransport { _, _ -> HttpResponse(200, """{"status":"success","data":[]}""") }
        val source = DataFastAnalyticsSource(
            "dft_secret",
            transport,
            rateLimiter = RateLimiter(maxRequests = 1, per = 1.minutes, timeSource = testScheduler.timeSource),
        )
        source.products()
        source.products()

        assertEquals(0L, testScheduler.currentTime)
    }
}
