package com.marcoelizalde.pulse.traffic.datafast

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Lets at most [maxRequests] requests through in any [per] window. The rest wait their turn, in
 * order, instead of being turned away by the provider; a request cancelled while waiting frees its turn.
 */
class RateLimiter(
    private val maxRequests: Int,
    private val per: Duration,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {
    private val mutex = Mutex()
    private val sent = ArrayDeque<ComparableTimeMark>()

    /** Suspends until one more request fits in the window, then counts it as sent. */
    suspend fun acquire() = mutex.withLock {
        // The lock is held while waiting, so waiters go out first come, first served.
        while (sent.size >= maxRequests) {
            val wait = per - sent.first().elapsedNow()
            if (wait.isPositive()) delay(wait) else sent.removeFirst()
        }
        sent.addLast(timeSource.markNow())
    }
}
