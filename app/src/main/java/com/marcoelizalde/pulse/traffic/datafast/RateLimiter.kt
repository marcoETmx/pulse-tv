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

    /** Set from outside the lock, since the request that learns about a hold must not queue behind waiters. */
    @Volatile
    private var heldUntil: ComparableTimeMark? = null

    /**
     * Sends nothing more for [duration], for when the provider says its own limit ran out: something else
     * may be spending the same budget, which this window cannot see.
     */
    fun holdFor(duration: Duration) {
        val until = timeSource.markNow() + duration
        synchronized(this) {
            if (heldUntil.let { it == null || until > it }) heldUntil = until
        }
    }

    /** Suspends until one more request fits in the window and no hold is on, then counts it as sent. */
    suspend fun acquire() = mutex.withLock {
        // The lock is held while waiting, so waiters go out first come, first served.
        while (true) {
            val held = heldUntil?.let { -it.elapsedNow() }
            if (held != null && held.isPositive()) {
                delay(held)
                continue
            }
            if (sent.size < maxRequests) break
            val wait = per - sent.first().elapsedNow()
            if (wait.isPositive()) delay(wait) else sent.removeFirst()
        }
        sent.addLast(timeSource.markNow())
    }
}
