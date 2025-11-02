package com.ivory.ivory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

class AssistantModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), PermissionListener {

    private val MICROPHONE_REQUEST_CODE = 1001
    private var micPromise: Promise? = null

    override fun getName() = "AssistantModule"

    @ReactMethod
    fun requestOverlayPermission(promise: Promise) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = android.net.Uri.parse("package:${reactApplicationContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            reactApplicationContext.startActivity(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("OVERLAY_ERROR", e)
        }
    }

    @ReactMethod
    fun hasOverlayPermission(promise: Promise) {
        promise.resolve(Settings.canDrawOverlays(reactApplicationContext))
    }

    @ReactMethod
    fun requestAssistPermission(promise: Promise) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = reactApplicationContext.getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager?.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT) == true) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    reactApplicationContext.startActivity(intent)
                    promise.resolve(true)
                } else {
                    promise.resolve(false)
                }
            } else {
                promise.resolve(false)
            }
        } catch (e: Exception) {
            promise.reject("ASSIST_ERROR", e)
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

    @ReactMethod
    fun requestMicrophonePermission(promise: Promise) {
        val currentActivity = currentActivity
        if (currentActivity == null) {
            promise.reject("ACTIVITY_NULL", "Current activity is null")
            return
        }

        if (ContextCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED) {
            promise.resolve(true)
            return
        }

        micPromise = promise
        val permissionAwareActivity = currentActivity as? PermissionAwareActivity
        permissionAwareActivity?.requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MICROPHONE_REQUEST_CODE,
            this
        ) ?: promise.reject("PERMISSION_ERROR", "Activity is not PermissionAwareActivity")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ): Boolean {
        if (requestCode == MICROPHONE_REQUEST_CODE) {
            val granted = grantResults?.isNotEmpty() == true && 
                         grantResults[0] == PackageManager.PERMISSION_GRANTED
            micPromise?.resolve(granted)
            micPromise = null
            return true
        }
        return false
    }

    @ReactMethod
    fun openMainApp() {
        val intent = Intent(reactApplicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        reactApplicationContext.startActivity(intent)
    }

    @ReactMethod
    fun sendText(text: String) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("OverlayText", text)
    }

    @ReactMethod
    fun finishActivity() {
        SystemOverlayService.hide(reactApplicationContext)
    }
}