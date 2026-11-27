package com.marcoelizalde.pulse

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {
    @Test
    fun `launches the dashboard when the device finishes booting`() {
        assertTrue(shouldLaunchOnBoot("android.intent.action.BOOT_COMPLETED"))
    }

    @Test
    fun `ignores any other broadcast`() {
        assertFalse(shouldLaunchOnBoot("android.intent.action.LOCKED_BOOT_COMPLETED"))
        assertFalse(shouldLaunchOnBoot("android.intent.action.SCREEN_ON"))
        assertFalse(shouldLaunchOnBoot(null))
    }
}
