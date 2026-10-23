package com.marcoelizalde.pulse.traffic.domain

/** A provider failure, with a [message] that is safe to show on screen. */
class AnalyticsException(message: String, cause: Throwable? = null) : Exception(message, cause)
