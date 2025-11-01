package com.ivory.ivory

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

class AssistantModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "Assistant"

    @ReactMethod
    fun showOverlay() {
        SystemOverlayService.show(reactApplicationContext)
    }

    @ReactMethod
    fun hideOverlay() {
        SystemOverlayService.hide(reactApplicationContext)
    }

    // called from the overlay when the user presses Send
    fun sendTextToJS(text: String) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("OverlayText", text)
    }

    // called when the ivory-star is tapped
    fun openMainApp() {
        val intent = Intent(reactApplicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        reactApplicationContext.startActivity(intent)
    }

    @ReactMethod
    fun requestOverlayPermission() {
        val activity: Activity? = currentActivity
        if (activity == null) return
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = android.net.Uri.parse("package:${reactApplicationContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}