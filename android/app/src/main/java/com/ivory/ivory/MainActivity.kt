package com.ivory.ivory

import expo.modules.splashscreen.SplashScreenManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import expo.modules.ReactActivityDelegateWrapper
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.ReactInstanceEventListener
import com.facebook.react.bridge.ReactContext
// The explicit import 'com.ivory.ivory.R' is correct, but let's make the theme setting more robust.
// import com.ivory.ivory.R // Keep this for now, but focus on the fix below.

class MainActivity : ReactActivity() {
    private var isAssistPending: Boolean = false
    private var listenerAdded: Boolean = false
    private val TAG = "MainActivity"

    // Helper function to dynamically get the style resource ID
    private fun getThemeStyleId(themeName: String): Int {
        return resources.getIdentifier(themeName, "style", packageName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        
        // ⚡️ CRITICAL: Check for assist intent and apply the transparent theme BEFORE super.onCreate()
        val isAssistIntent = intent?.action == Intent.ACTION_ASSIST || intent?.getBooleanExtra("showAssistOverlay", false)
        
        if (isAssistIntent) {
            Log.d(TAG, "Assist intent detected. Applying AssistOverlay theme.")
            
            // 💡 FIX: Use dynamic lookup for the Assist Overlay theme
            val assistThemeId = getThemeStyleId("Theme_App_AssistOverlay")
            if (assistThemeId != 0) {
                setTheme(assistThemeId)
            } else {
                Log.e(TAG, "FATAL: Theme_App_AssistOverlay not found! Using default AppTheme.")
                setTheme(R.style.AppTheme) // Fallback for the build system
            }
        } else {
            // Apply your normal app theme for regular launches
            Log.d(TAG, "Normal launch. Applying default AppTheme.")
            
            // 💡 FIX: Use dynamic lookup for the normal AppTheme, or the explicit R.style reference
            // Sticking with the explicit R.style.AppTheme here should be fine if Theme_App_AssistOverlay was the specific issue.
            setTheme(R.style.AppTheme)
        }

        SplashScreenManager.registerOnActivity(this)
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate called")
        handleAssistIntent(intent)
        setupAssistListener()
    }

    // ... (rest of the MainActivity.kt code remains the same) ...

    override fun getMainComponentName(): String = "main"

    override fun createReactActivityDelegate(): ReactActivityDelegate {
        return ReactActivityDelegateWrapper(
            this,
            BuildConfig.IS_NEW_ARCHITECTURE_ENABLED,
            object : DefaultReactActivityDelegate(
                this,
                mainComponentName,
                fabricEnabled
            ) {}
        )
    }

    override fun invokeDefaultOnBackPressed() {
        if (intent.action == Intent.ACTION_ASSIST || isAssistPending) {
             Log.d(TAG, "Back pressed on Assist Activity, finishing.")
             finish()
             return
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (!moveTaskToBack(false)) {
                super.invokeDefaultOnBackPressed()
            }
            return
        }
        super.invokeDefaultOnBackPressed()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called, isAssistPending: $isAssistPending")
        if (isAssistPending) {
            val currentContext = reactNativeHost?.reactInstanceManager?.currentReactContext
            if (currentContext != null) {
                Log.d(TAG, "Emitting from onResume")
                emitAssistEvent(currentContext)
                isAssistPending = false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "onNewIntent called")
        handleAssistIntent(intent)
        setupAssistListener()
    }

    private fun handleAssistIntent(intent: Intent?) {
        if (intent == null) return

        val showAssist = intent.getBooleanExtra("showAssistOverlay", false)
        val action = intent.action
        Log.d(TAG, "Handling assist intent, showAssist: $showAssist, action: $action")

        if (showAssist || Intent.ACTION_ASSIST == action) {
            val currentContext = reactNativeHost?.reactInstanceManager?.currentReactContext
            Log.d(TAG, "Context available: ${currentContext != null}")
            if (currentContext != null) {
                Log.d(TAG, "Emitting immediately")
                emitAssistEvent(currentContext)
                isAssistPending = false
            } else {
                Log.d(TAG, "Context null, setting pending")
                isAssistPending = true
            }
        }
    }

    private fun setupAssistListener() {
        if (!listenerAdded && reactNativeHost != null) {
            Log.d(TAG, "Setting up listener")
            reactNativeHost?.reactInstanceManager?.addReactInstanceEventListener(
                object : ReactInstanceEventListener {
                    override fun onReactContextInitialized(context: ReactContext) {
                        Log.d(TAG, "Context initialized, isAssistPending: $isAssistPending")
                        if (isAssistPending) {
                            Log.d(TAG, "Emitting from listener")
                            emitAssistEvent(context)
                            isAssistPending = false
                        }
                        reactNativeHost?.reactInstanceManager?.removeReactInstanceEventListener(this)
                    }
                }
            )
            listenerAdded = true
        }
    }

    private fun emitAssistEvent(context: ReactContext) {
        Log.d(TAG, "Emitting onAssistRequested")
        context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            ?.emit("onAssistRequested", null)
    }
}