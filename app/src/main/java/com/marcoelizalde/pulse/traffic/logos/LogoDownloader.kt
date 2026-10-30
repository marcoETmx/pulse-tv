package com.marcoelizalde.pulse.traffic.logos

import com.marcoelizalde.pulse.net.HttpTransport

/** Downloads images the TV can decode; anything else counts as "no logo". */
class LogoDownloader(private val transport: HttpTransport) {
    /** Returns the image bytes, or null when [url] is not a decodable image; throws on network failures. */
    suspend fun download(url: String): ByteArray? {
        val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme != "https" && scheme != "http") return null
        val response = transport.get(url, emptyMap())
        if (response.status !in 200..299) return null
        // Servers mislabel images and serve HTML error pages as 200, so the bytes decide.
        return response.bytes.takeIf { it.isDecodableImage() }
    }
}

private val Png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
private val Jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val Gif = "GIF8".encodeToByteArray()
private val Riff = "RIFF".encodeToByteArray()
private val Webp = "WEBP".encodeToByteArray()

private fun ByteArray.isDecodableImage(): Boolean =
    startsWith(Png) || startsWith(Jpeg) || startsWith(Gif) ||
        (startsWith(Riff) && size >= 12 && copyOfRange(8, 12).contentEquals(Webp))

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
