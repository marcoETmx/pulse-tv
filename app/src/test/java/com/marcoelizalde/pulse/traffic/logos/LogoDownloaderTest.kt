package com.marcoelizalde.pulse.traffic.logos

import com.marcoelizalde.pulse.traffic.datafast.HttpResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogoDownloaderTest {
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3)
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 9)
    private val webp = "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".encodeToByteArray()

    @Test
    fun `png, jpeg and webp images are returned whatever the content type says`() = runTest {
        for (image in listOf(png, jpeg, webp)) {
            val downloader = LogoDownloader { _, _ -> HttpResponse(200, image, "application/octet-stream") }

            assertArrayEquals(image, downloader.download("https://example.com/logo"))
        }
    }

    @Test
    fun `pages, svg and ico files are not logos`() = runTest {
        val bodies = listOf(
            "<!doctype html><title>Not found</title>".encodeToByteArray(),
            "<svg xmlns='http://www.w3.org/2000/svg'/>".encodeToByteArray(),
            byteArrayOf(0, 0, 1, 0, 1, 0),
        )
        for (body in bodies) {
            val downloader = LogoDownloader { _, _ -> HttpResponse(200, body, "image/png") }

            assertNull(downloader.download("https://example.com/icon.png"))
        }
    }

    @Test
    fun `error responses and non-web urls are not fetched as logos`() = runTest {
        val requested = mutableListOf<String>()
        val downloader = LogoDownloader { url, _ ->
            requested += url
            HttpResponse(404, png, "image/png")
        }

        assertNull(downloader.download("https://example.com/icon.png"))
        assertNull(downloader.download("file:///data/data/secret.png"))
        assertNull(downloader.download("data:image/png;base64,AAAA"))
        assertEquals(listOf("https://example.com/icon.png"), requested)
    }
}
