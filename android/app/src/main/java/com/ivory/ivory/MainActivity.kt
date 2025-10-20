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
import com.ivory.ivory.R

class MainActivity : ReactActivity() {
    private var isAssistPending: Boolean = false
    private var listenerAdded: Boolean = false
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = intent
        
        // ⚡️ CRITICAL: Check for assist intent and apply the transparent theme BEFORE super.onCreate()
        val isAssistIntent = intent?.action == Intent.ACTION_ASSIST || intent?.getBooleanExtra("showAssistOverlay", false)
        
        if (isAssistIntent) {
            Log.d(TAG, "Assist intent detected. Applying AssistOverlay theme.")
            // Using R.style.Theme_App_AssistOverlay as defined in styles.xml
            setTheme(R.style.Theme_App_AssistOverlay) 
        } else {
            // Apply your normal app theme for regular launches
            Log.d(TAG, "Normal launch. Applying default AppTheme.")
            setTheme(R.style.AppTheme)
        }

        SplashScreenManager.registerOnActivity(this)
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate called")
        handleAssistIntent(intent)
        setupAssistListener()
    }

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
        // When acting as an overlay, the back button should usually close the overlay.
        // For assist, we typically want to finish the activity immediately.
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
                        // Remove the listener after context is ready to prevent memory leaks/re-runs
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