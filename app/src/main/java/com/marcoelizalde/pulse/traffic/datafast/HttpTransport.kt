package com.marcoelizalde.pulse.traffic.datafast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class HttpResponse(val status: Int, val body: String)

fun interface HttpTransport {
    /** Performs a GET; throws [java.io.IOException] when the network fails. */
    suspend fun get(url: String, headers: Map<String, String>): HttpResponse
}

class UrlConnectionTransport(private val timeoutMillis: Int = 15_000) : HttpTransport {
    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = timeoutMillis
                connection.readTimeout = timeoutMillis
                // Never replay the bearer token to wherever a redirect points.
                connection.instanceFollowRedirects = false
                headers.forEach(connection::setRequestProperty)
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                HttpResponse(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
            } finally {
                connection.disconnect()
            }
        }
}
