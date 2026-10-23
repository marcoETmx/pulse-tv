package com.marcoelizalde.pulse.traffic.logos

import com.marcoelizalde.pulse.traffic.datafast.HttpTransport
import java.net.URI

/** Finds a product site's own icon by reading the `<link rel="…icon…">` tags of its home page. */
class SiteIconFinder(private val transport: HttpTransport) {
    /** Returns the best icon URL, or null when the site has none; throws on network failures. */
    suspend fun iconUrl(domain: String): String? {
        val home = "https://$domain/"
        val response = transport.get(home, emptyMap())
        if (response.status !in 200..299) return null
        // Sites that declare nothing usable often still serve the conventional path.
        return bestIconUrl(response.body, home) ?: "${home}apple-touch-icon.png"
    }
}

/** Picks the largest PNG, JPEG or WebP icon declared in [html], resolved against [pageUrl]. */
internal fun bestIconUrl(html: String, pageUrl: String): String? =
    LinkTag.findAll(html)
        .map { tag -> attributes(tag.value) }
        .mapNotNull { attributes -> iconCandidate(attributes, pageUrl) }
        .maxByOrNull { it.size }
        ?.url

private class IconCandidate(val url: String, val size: Int)

private val LinkTag = Regex("""<link\b[^>]*>""", RegexOption.IGNORE_CASE)
private val Attribute = Regex("""([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))""")
private val RasterTypes = setOf("image/png", "image/jpeg", "image/jpg", "image/webp")
private val RasterExtensions = setOf("png", "jpg", "jpeg", "webp")

private fun attributes(tag: String): Map<String, String> =
    Attribute.findAll(tag).associate { match ->
        val (name, doubleQuoted, singleQuoted, bare) = match.destructured
        name.lowercase() to doubleQuoted.ifEmpty { singleQuoted.ifEmpty { bare } }
    }

private fun iconCandidate(attributes: Map<String, String>, pageUrl: String): IconCandidate? {
    val rel = attributes["rel"]?.lowercase()?.split(Regex("\\s+")) ?: return null
    val apple = "apple-touch-icon" in rel || "apple-touch-icon-precomposed" in rel
    // "mask-icon" is a single token, so Safari's monochrome masks never match.
    if (!apple && "icon" !in rel) return null

    val href = attributes["href"]?.replace("&amp;", "&")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val url = runCatching { URI(pageUrl).resolve(href) }.getOrNull() ?: return null
    if (url.scheme?.lowercase() !in setOf("https", "http")) return null

    val raster = attributes["type"]?.lowercase()?.let { it in RasterTypes }
        ?: (url.path.orEmpty().substringAfterLast('.', "").lowercase() in RasterExtensions)
    if (!raster) return null

    val declaredSize = attributes["sizes"]?.let { sizes ->
        Regex("""(\d+)x\d+""", RegexOption.IGNORE_CASE).findAll(sizes).maxOfOrNull { it.groupValues[1].toInt() }
    }
    return IconCandidate(url.toString(), declaredSize ?: if (apple) 180 else 48)
}
