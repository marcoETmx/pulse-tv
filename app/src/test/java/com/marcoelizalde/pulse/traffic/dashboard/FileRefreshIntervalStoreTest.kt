package com.marcoelizalde.pulse.traffic.dashboard

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileRefreshIntervalStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a saved interval is read back after a restart`() {
        val file = File(folder.root, "settings/refresh_interval")

        FileRefreshIntervalStore(file).save(RefreshInterval.TEN_MINUTES)

        assertEquals(RefreshInterval.TEN_MINUTES, FileRefreshIntervalStore(file).load())
    }

    @Test
    fun `a missing file or an unknown value means nothing was chosen`() {
        val file = File(folder.root, "refresh_interval")
        val missing = FileRefreshIntervalStore(file).load()

        val unknown = listOf("7", "abc", "").map { value ->
            file.writeText(value)
            FileRefreshIntervalStore(file).load()
        }

        assertEquals(listOf(null, null, null, null), listOf(missing) + unknown)
    }
}
