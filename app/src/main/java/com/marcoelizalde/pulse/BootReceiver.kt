package com.marcoelizalde.pulse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Opens the dashboard when the TV device finishes booting, so a wall TV shows Pulse
 * after a power cut or restart without anyone picking up the remote.
 *
 * Waking from standby needs nothing: Android TV and Fire OS bring back the app that was open.
 * Starting an activity from a receiver works up to Android 9; later versions ignore it.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!shouldLaunchOnBoot(intent.action)) return
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

internal fun shouldLaunchOnBoot(action: String?): Boolean = action == Intent.ACTION_BOOT_COMPLETED
