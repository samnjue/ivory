package com.ivory.ivory

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.view.MotionEvent

class AssistOverlayActivity : Activity() {
    private val TAG = "AssistOverlayActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "AssistOverlayActivity created")
        
        // Transparent overlay
        window.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            
            // Appears over everything
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            
            // Optional: dim the background
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                dimAmount = 0.3f
            }
        }
        
        // Launch the main app with overlay flag
        launchMainActivityWithOverlay()
    }
    
    private fun launchMainActivityWithOverlay() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_ASSIST
            putExtra("showAssistOverlay", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        startActivity(intent)
        Log.d(TAG, "Launched MainActivity with overlay flag")
        
        // Finish this transparent activity immediately
        finish()
        overridePendingTransition(0, 0)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Close on any touch outside
        if (event.action == MotionEvent.ACTION_OUTSIDE) {
            finish()
            return true
        }
        return super.onTouchEvent(event)
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}