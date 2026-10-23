package com.marcoelizalde.pulse.traffic.datafast

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class UrlConnectionTransportTest {
    private val servers = mutableListOf<HttpServer>()

    @After
    fun tearDown() = servers.forEach { it.stop(0) }

    @Test
    fun `redirects are not followed so the token never reaches another host`() = runTest {
        val otherHostAuthorizations = CopyOnWriteArrayList<String?>()
        val otherHost = server { exchange ->
            otherHostAuthorizations += exchange.requestHeaders.getFirst("Authorization")
            exchange.sendResponseHeaders(200, -1)
        }
        val api = server { exchange ->
            exchange.responseHeaders.add("Location", "http://localhost:${otherHost.address.port}/steal")
            exchange.sendResponseHeaders(302, -1)
        }

        val response = UrlConnectionTransport().get(
            "http://127.0.0.1:${api.address.port}/admin/websites",
            mapOf("Authorization" to "Bearer dft_secret"),
        )

        assertEquals(302, response.status)
        assertTrue(otherHostAuthorizations.isEmpty())
    }

    private fun server(handle: (com.sun.net.httpserver.HttpExchange) -> Unit): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange -> exchange.use(handle) }
            start()
            servers += this
        }
}
