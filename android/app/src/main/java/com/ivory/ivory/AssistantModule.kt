package com.ivory.ivory

import android.content.Intent
import android.provider.Settings
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class AssistantModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "Assistant"

    // Permission helpers
    @ReactMethod
    fun requestOverlayPermission(promise: Promise) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = android.net.Uri.parse("package:${reactApplicationContext.packageName}")
            }
            reactApplicationContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("OVERLAY", e)
        }
    }

    @ReactMethod
    fun hasOverlayPermission(promise: Promise) {
        promise.resolve(Settings.canDrawOverlays(reactApplicationContext))
    }

    // Assistant role
    @ReactMethod
    fun requestAssistPermission(promise: Promise) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = reactApplicationContext.getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager?.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT) == true) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT)
                    reactApplicationContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    promise.resolve(true)
                } else {
                    promise.resolve(false)
                }
            } else {
                promise.resolve(false)
            }
        } catch (e: Exception) {
            promise.reject("ASSIST", e)
        }
    }

    @ReactMethod
    fun isAssistantEnabled(promise: Promise) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = reactApplicationContext.getSystemService(android.app.role.RoleManager::class.java)
            promise.resolve(roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT) == true)
        } else {
            promise.resolve(false)
        }
    }

    // Open main app
    @ReactMethod
    fun openMainApp() {
        val intent = Intent(reactApplicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        reactApplicationContext.startActivity(intent)
    }

    // Send text from overlay to JS 
    @ReactMethod
    fun sendText(text: String) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("OverlayText", text)
    }

    // Finish overlay activity (optional) 
    @ReactMethod
    fun finishActivity() {
        SystemOverlayService.hide(reactApplicationContext)
    }
}