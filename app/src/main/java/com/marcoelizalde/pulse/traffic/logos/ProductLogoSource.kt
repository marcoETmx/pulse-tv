package com.marcoelizalde.pulse.traffic.logos

import com.marcoelizalde.pulse.traffic.domain.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.Duration

/** Disk cache of logo bytes per product; an empty file records "this product has no logo". */
class LogoCache(
    private val directory: File,
    private val clock: Clock,
    private val ttl: Duration = Duration.ofDays(7),
) {
    class Entry(val bytes: ByteArray?)

    fun read(productId: String): Entry? {
        val file = fileFor(productId)
        if (!file.isFile || clock.millis() - file.lastModified() > ttl.toMillis()) return null
        return Entry(file.readBytes().takeIf { it.isNotEmpty() })
    }

    fun write(productId: String, bytes: ByteArray?) {
        directory.mkdirs()
        val file = fileFor(productId)
        file.writeBytes(bytes ?: ByteArray(0))
        file.setLastModified(clock.millis())
    }

    // Ids come from the network; anything but letters and digits is flattened so paths stay in [directory].
    private fun fileFor(productId: String): File =
        File(directory, productId.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("") + ".logo")
}

/** The product's logo: the one configured in DataFast, else the site's own icon, else none. */
class ProductLogoSource(
    private val dataFastLogoUrl: suspend (Product) -> String?,
    private val siteIconUrl: suspend (String) -> String?,
    private val download: suspend (String) -> ByteArray?,
    private val cache: LogoCache,
) {
    suspend fun logo(product: Product): ByteArray? = withContext(Dispatchers.IO) {
        cache.read(product.id)?.let { return@withContext it.bytes }

        var failed = false
        suspend fun <T : Any> attempt(lookup: suspend () -> T?): T? = try {
            lookup()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
            null
        }

        val logo = attempt { dataFastLogoUrl(product) }?.let { url -> attempt { download(url) } }
            ?: attempt { siteIconUrl(product.domain) }?.let { url -> attempt { download(url) } }
        // "No logo" is remembered only when every lookup really answered; failures retry next time.
        if (logo != null || !failed) cache.write(product.id, logo)
        logo
    }
}
