package com.marcoelizalde.pulse.traffic.dashboard

import com.marcoelizalde.pulse.traffic.dashboard.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a saved interval is read back after a restart`() {
        val file = File(folder.root, "settings/refresh_interval")

        RefreshInterval.store(file).save(RefreshInterval.TEN_MINUTES)

        assertEquals(RefreshInterval.TEN_MINUTES, RefreshInterval.store(file).load())
    }

    @Test
    fun `the interval file keeps holding its minutes, so a choice saved by an earlier version still loads`() {
        val file = File(folder.root, "refresh_interval")
        file.writeText("15")

        assertEquals(RefreshInterval.FIFTEEN_MINUTES, RefreshInterval.store(file).load())

        RefreshInterval.store(file).save(RefreshInterval.TEN_MINUTES)
        assertEquals("10", file.readText())
    }

    @Test
    fun `a missing file or an unknown value means nothing was chosen`() {
        val file = File(folder.root, "refresh_interval")
        val missing = RefreshInterval.store(file).load()

        val unknown = listOf("7", "abc", "").map { value ->
            file.writeText(value)
            RefreshInterval.store(file).load()
        }

        assertEquals(listOf(null, null, null, null), listOf(missing) + unknown)
    }

    @Test
    fun `a saved ranking order is read back after a restart, and an unknown one means nothing was chosen`() {
        val file = File(folder.root, "ranking_order")

        RankingOrder.store(file).save(RankingOrder.SALES)
        assertEquals(RankingOrder.SALES, RankingOrder.store(file).load())

        file.writeText("revenue")
        assertEquals(null, RankingOrder.store(file).load())
    }

    @Test
    fun `a saved language is read back after a restart, and an unknown one means nothing was chosen`() {
        val file = File(folder.root, "language")

        AppLanguage.store(file).save(AppLanguage.SPANISH)
        assertEquals("es", file.readText())
        assertEquals(AppLanguage.SPANISH, AppLanguage.store(file).load())

        file.writeText("klingon")
        assertEquals(null, AppLanguage.store(file).load())
    }
}
