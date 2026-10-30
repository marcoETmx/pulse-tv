package com.marcoelizalde.pulse.traffic.domain

import kotlin.time.Duration

/**
 * A provider failure. [problem] says what went wrong, and the screen words it in the chosen language.
 * [retryAfter] is set when the provider says how long to wait, for example after a rate limit.
 */
class AnalyticsException(
    val problem: AnalyticsProblem,
    cause: Throwable? = null,
    val retryAfter: Duration? = null,
) : Exception(problem.toString(), cause)

/** What went wrong while loading analytics. */
sealed interface AnalyticsProblem {
    /** The provider could not be reached, usually because the TV is offline. */
    data object Unreachable : AnalyticsProblem

    /** The provider rejected the token (401). */
    data object TokenRejected : AnalyticsProblem

    /** The token lacks the read permissions Pulse needs (403). */
    data object MissingPermissions : AnalyticsProblem

    data class RateLimited(val requestsPerMinute: Int) : AnalyticsProblem

    /** Any other error status, with the provider's own explanation when it sends one; that text is never translated. */
    data class ProviderError(val status: Int, val detail: String?) : AnalyticsProblem

    /** An answer Pulse could not read. */
    data object UnexpectedResponse : AnalyticsProblem

    /** A failure that did not come from the provider, such as a bug in Pulse. */
    data object Unknown : AnalyticsProblem
}
