package com.ivory.ivory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager

class AssistOverlayActivity : Activity() {
    private val TAG = "AssistOverlayActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "AssistOverlayActivity created")
        
        // Make this activity transparent and appear as an overlay
        window.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        }
        
        // Check for overlay permission
        if (!OverlayPermissionHelper.hasOverlayPermission(this)) {
            Log.w(TAG, "Overlay permission not granted, requesting...")
            OverlayPermissionHelper.requestOverlayPermission(this)
            finish()
            return
        }
        
        // Show the native system overlay
        launchSystemOverlay()
    }
    
    private fun launchSystemOverlay() {
        Log.d(TAG, "Launching system overlay")
        SystemOverlayManager.show(this)
        // Finish this transparent activity immediately
        finish()
        overridePendingTransition(0, 0)
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}