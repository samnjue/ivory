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

import com.facebook.react.modules.core.DeviceEventManagerModule 
import com.facebook.react.bridge.ReactContext 
import com.facebook.react.bridge.WritableMap 
import com.facebook.react.bridge.Arguments 
import expo.modules.ReactActivityDelegateWrapper

class MainActivity : ReactActivity() {
    private var isAssistPending: Boolean = false
    private var listenerAdded: Boolean = false
    private val TAG = "MainActivity"
    private var isAssistMode: Boolean = false
    private var pendingQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        SplashScreenManager.registerOnActivity(this)
        super.onCreate(null)

        Log.d(TAG, "onCreate called")
        val intent = intent
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
            ){})
    }

    override fun invokeDefaultOnBackPressed() {
        if (isAssistMode) {
            finish()
            overridePendingTransition(0, 0)
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
        Log.d(TAG, "onResume called, isAssistPending: $isAssistPending, isAssistMode: $isAssistMode")
        if (isAssistPending) {
            val currentContext = reactNativeHost?.reactInstanceManager?.currentReactContext
            if (currentContext != null) {
                Log.d(TAG, "Emitting from onResume")
                emitAssistEvent(currentContext)
                isAssistPending = false
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it)
            Log.d(TAG, "onNewIntent called")
            isAssistMode = it.action == Intent.ACTION_ASSIST || it.getBooleanExtra("showAssistOverlay", false) 
            handleAssistIntent(it)
            setupAssistListener()
        }
    }

    private fun handleAssistIntent(intent: Intent?) {
        intent ?: return

        val showAssist = intent.getBooleanExtra("showAssistOverlay", false)
        val action = intent.action
        pendingQuery = intent.getStringExtra("query")
        Log.d(TAG, "Handling assist intent, showAssist: $showAssist, action: $action, query: $pendingQuery")

        if (showAssist || action == Intent.ACTION_ASSIST) {
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
                object : com.facebook.react.ReactInstanceEventListener {
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
        val params: WritableMap = Arguments.createMap().apply {
            pendingQuery?.let { putString("query", it) }
        }
        context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            ?.emit("onAssistRequested", params)
        pendingQuery = null
    }
    
    override fun finish() {
        super.finish()
        if (isAssistMode) {
            overridePendingTransition(0, 0)
        }
    }
}