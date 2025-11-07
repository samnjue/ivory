package com.ivory.ivory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            // Optional: only start if user previously enabled
            val prefs = context.getSharedPreferences("ivory_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("floating_orb_enabled", false)) {
                IvoryOverlayService.start(context)
            }
        }
    }
}