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

class MainActivity : ReactActivity() {
    private var isAssistPending: Boolean = false
    private var listenerAdded: Boolean = false
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        
        // ⚡️ CRITICAL FIX: Set the theme BEFORE super.onCreate()
        if (intent != null && (Intent.ACTION_ASSIST == intent.action || intent.getBooleanExtra("showAssistOverlay", false))) {
            Log.d(TAG, "Setting transparent theme for assist")
            // Apply the fully transparent theme
            setTheme(R.style.TransparentActivity) 
        } else {
            Log.d(TAG, "Setting standard splash theme")
            // Apply the standard splash screen theme for normal launches
            setTheme(R.style.Theme_App_SplashScreen) 
        }

        SplashScreenManager.registerOnActivity(this)
        super.onCreate(savedInstanceState) // The window is initialized with the theme set above

        Log.d(TAG, "onCreate called")
        handleAssistIntent(intent)
        setupAssistListener()
    }
    
    // ... (rest of the code remains the same)

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