package com.marcoelizalde.pulse.traffic.logos

import com.marcoelizalde.pulse.traffic.datafast.HttpResponse
import com.marcoelizalde.pulse.traffic.datafast.HttpTransport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SiteIconFinderTest {
    private val page = "https://shipli.st/"

    @Test
    fun `the largest png icon wins and keeps its cache-busting query`() {
        val html = """
            <link rel="icon" href="/favicon.ico?favicon.0ocz4uhp8qbxs.ico" sizes="48x48" type="image/x-icon">
            <link rel="icon" href="/icon.png?icon.0f9upjd6sowla.png" type="image/png" sizes="512x512">
            <link rel="apple-touch-icon" href="/apple-icon.png?apple-icon.2b5ujbvwe_c8f.png" type="image/png" sizes="180x180">
        """

        assertEquals("https://shipli.st/icon.png?icon.0f9upjd6sowla.png", bestIconUrl(html, page))
    }

    @Test
    fun `an apple touch icon without sizes beats a small favicon png`() {
        val html = """
            <link rel='icon' type='image/png' href='/favicon-32x32.png' sizes='32x32'/>
            <link rel=apple-touch-icon href=/apple-touch-icon.png>
        """

        assertEquals("https://shipli.st/apple-touch-icon.png", bestIconUrl(html, page))
    }

    @Test
    fun `svg, ico and mask icons are ignored`() {
        val html = """
            <link rel="icon" href="/favicon.svg" type="image/svg+xml">
            <link rel="shortcut icon" href="/favicon.ico">
            <link rel="mask-icon" href="/safari.png" color="#000">
        """

        assertNull(bestIconUrl(html, page))
    }

    @Test
    fun `absolute and protocol-relative hrefs are resolved and entities decoded`() {
        assertEquals(
            "https://cdn.example/i.png?a=1&b=2",
            bestIconUrl("""<link href="//cdn.example/i.png?a=1&amp;b=2" rel="icon">""", page),
        )
        assertEquals(
            "https://img.example/logo.jpg",
            bestIconUrl("""<LINK REL="Apple-Touch-Icon" HREF="https://img.example/logo.jpg">""", page),
        )
    }

    @Test
    fun `the finder reads the home page without any credentials`() = runTest {
        val requests = mutableListOf<Pair<String, Map<String, String>>>()
        val finder = SiteIconFinder { url, headers ->
            requests += url to headers
            HttpResponse(200, """<link rel="icon" href="/icon.png" sizes="192x192">""", "text/html")
        }

        assertEquals("https://devzap.io/icon.png", finder.iconUrl("devzap.io"))
        assertEquals(listOf("https://devzap.io/" to emptyMap<String, String>()), requests)
    }

    @Test
    fun `without a usable link the conventional apple touch icon is tried`() = runTest {
        val finder = SiteIconFinder { _, _ -> HttpResponse(200, "<html><head></head></html>", "text/html") }

        assertEquals("https://notokit.com/apple-touch-icon.png", finder.iconUrl("notokit.com"))
    }

    @Test
    fun `a site that does not answer with a page has no icon`() = runTest {
        val finder = SiteIconFinder { _, _ -> HttpResponse(503, "down", "text/plain") }

        assertNull(finder.iconUrl("braixchat.com"))
    }

    @Test
    fun `network failures are reported, not hidden as no icon`() = runTest {
        val finder = SiteIconFinder(HttpTransport { _, _ -> throw IOException("timeout") })

        assertTrue(runCatching { finder.iconUrl("notokit.com") }.exceptionOrNull() is IOException)
    }
}
