package com.marcoelizalde.pulse.traffic.domain

import kotlin.time.Duration

/**
 * A provider failure, with a [message] that is safe to show on screen. [retryAfter] is set when the
 * provider says how long to wait, for example after a rate limit.
 */
class AnalyticsException(
    message: String,
    cause: Throwable? = null,
    val retryAfter: Duration? = null,
) : Exception(message, cause)
