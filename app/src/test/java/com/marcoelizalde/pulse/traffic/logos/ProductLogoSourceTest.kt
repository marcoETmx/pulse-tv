package com.marcoelizalde.pulse.traffic.logos

import com.marcoelizalde.pulse.traffic.domain.Product
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ProductLogoSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val shop = Product("665f0b3c4d2e1a0012345678", "Shop", "shop.example")
    private val dataFastLogo = byteArrayOf(1)
    private val siteLogo = byteArrayOf(2)
    private val clock = MutableClock(Instant.parse("2026-10-23T18:00:00Z"))
    private val lookups = mutableListOf<String>()

    private fun source(
        dataFastUrl: String? = null,
        siteUrl: String? = null,
        images: Map<String, ByteArray> = mapOf("https://cdn/df.png" to dataFastLogo, "https://shop.example/icon.png" to siteLogo),
        failing: Set<String> = emptySet(),
    ) = ProductLogoSource(
        dataFastLogoUrl = { lookups += "datafast"; if ("datafast" in failing) throw IOException("429"); dataFastUrl },
        siteIconUrl = { domain -> lookups += "site:$domain"; if ("site" in failing) throw IOException("timeout"); siteUrl },
        download = { url -> lookups += "download:$url"; images[url] },
        cache = LogoCache(folder.root, clock),
    )

    @Test
    fun `the DataFast logo comes first`() = runTest {
        val logo = source(dataFastUrl = "https://cdn/df.png", siteUrl = "https://shop.example/icon.png").logo(shop)

        assertArrayEquals(dataFastLogo, logo)
        assertEquals(listOf("datafast", "download:https://cdn/df.png"), lookups)
    }

    @Test
    fun `without a DataFast logo the site's icon is used`() = runTest {
        val logo = source(dataFastUrl = null, siteUrl = "https://shop.example/icon.png").logo(shop)

        assertArrayEquals(siteLogo, logo)
        assertEquals(listOf("datafast", "site:shop.example", "download:https://shop.example/icon.png"), lookups)
    }

    @Test
    fun `a DataFast logo that is not an image falls back to the site's icon`() = runTest {
        val logo = source(dataFastUrl = "https://cdn/broken", siteUrl = "https://shop.example/icon.png").logo(shop)

        assertArrayEquals(siteLogo, logo)
    }

    @Test
    fun `logos are cached for a week`() = runTest {
        val source = source(dataFastUrl = "https://cdn/df.png")
        source.logo(shop)
        lookups.clear()

        clock.advance(Duration.ofDays(6))
        assertArrayEquals(dataFastLogo, source.logo(shop))
        assertEquals(emptyList<String>(), lookups)

        clock.advance(Duration.ofDays(2))
        source.logo(shop)
        assertEquals("datafast", lookups.first())
    }

    @Test
    fun `a product with no logo anywhere is remembered as such`() = runTest {
        val source = source()
        assertNull(source.logo(shop))
        lookups.clear()

        assertNull(source.logo(shop))
        assertEquals(emptyList<String>(), lookups)
    }

    @Test
    fun `a failed lookup is retried next time instead of being cached as no logo`() = runTest {
        assertNull(source(failing = setOf("datafast", "site")).logo(shop))
        lookups.clear()

        val logo = source(dataFastUrl = "https://cdn/df.png").logo(shop)

        assertArrayEquals(dataFastLogo, logo)
        assertEquals(listOf("datafast", "download:https://cdn/df.png"), lookups)
    }

    @Test
    fun `cache files stay inside the cache directory whatever the product id`() = runTest {
        val sneaky = Product("../../etc/passwd", "Sneaky", "sneaky.example")

        source(dataFastUrl = "https://cdn/df.png").logo(sneaky)

        assertEquals(listOf(folder.root), folder.root.walkTopDown().filter { it.isFile }.map { it.parentFile }.toList())
    }

    private class MutableClock(private var now: Instant) : Clock() {
        fun advance(duration: Duration) {
            now += duration
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
    }
}
