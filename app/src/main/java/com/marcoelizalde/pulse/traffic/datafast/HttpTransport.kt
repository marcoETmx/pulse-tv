package com.marcoelizalde.pulse.traffic.datafast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class HttpResponse(val status: Int, val bytes: ByteArray, val contentType: String? = null) {
    constructor(status: Int, body: String, contentType: String? = "application/json") :
        this(status, body.encodeToByteArray(), contentType)

    val body: String get() = bytes.decodeToString()
}

fun interface HttpTransport {
    /** Performs a GET; throws [java.io.IOException] when the network fails. */
    suspend fun get(url: String, headers: Map<String, String>): HttpResponse
}

/**
 * [followRedirects] stays off for anything carrying the DataFast token, so it is never replayed to
 * wherever a redirect points; public fetches (product sites and their icons) turn it on.
 */
class UrlConnectionTransport(
    private val timeoutMillis: Int = 15_000,
    private val followRedirects: Boolean = false,
    private val maxBytes: Int = 2 * 1024 * 1024,
) : HttpTransport {
    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = timeoutMillis
                connection.readTimeout = timeoutMillis
                connection.instanceFollowRedirects = followRedirects
                headers.forEach(connection::setRequestProperty)
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                HttpResponse(status, stream?.use { it.readAtMost(maxBytes) } ?: ByteArray(0), connection.contentType)
            } finally {
                connection.disconnect()
            }
        }
}

private fun InputStream.readAtMost(maxBytes: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        if (out.size() + read > maxBytes) throw IOException("Response is larger than $maxBytes bytes")
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
