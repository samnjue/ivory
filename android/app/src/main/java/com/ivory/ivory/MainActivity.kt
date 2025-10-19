package com.ivory.ivory

import expo.modules.splashscreen.SplashScreenManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import expo.modules.ReactActivityDelegateWrapper
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.ReactContext
import com.facebook.react.ReactInstanceManager.ReactInstanceEventListener

class MainActivity : ReactActivity() {
    private var isAssistPending: Boolean = false
    private var listenerAdded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Conditionally set transparent theme for assist launches
        val intent = intent
        if (intent != null && (Intent.ACTION_ASSIST == intent.action || intent.getBooleanExtra("showAssistOverlay", false))) {
            setTheme(R.style.TransparentActivity)  // Use your transparent theme
        }

        SplashScreenManager.registerOnActivity(this)
        super.onCreate(savedInstanceState)  // Use savedInstanceState to avoid potential issues

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
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (!moveTaskToBack(false)) {
                super.invokeDefaultOnBackPressed()
            }
            return
        }
        super.invokeDefaultOnBackPressed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAssistIntent(intent)
        setupAssistListener()
    }

    private fun handleAssistIntent(intent: Intent?) {
        if (intent == null) return

        val showAssist = intent.getBooleanExtra("showAssistOverlay", false)
        val action = intent.action

        if (showAssist || Intent.ACTION_ASSIST == action) {
            val host = getReactNativeHost()
            if (host == null) {
                isAssistPending = true
                return
            }
            val instanceManager = getReactInstanceManager()
            val currentContext = instanceManager.currentReactContext

            if (currentContext != null) {
                currentContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("onAssistRequested", null)
                isAssistPending = false
            } else {
                isAssistPending = true
            }
        }
    }

    private fun setupAssistListener() {
        val host = getReactNativeHost() ?: return
        val instanceManager = getReactInstanceManager()

        if (!listenerAdded) {
            instanceManager.addReactInstanceEventListener(object : ReactInstanceEventListener {
                override fun onReactContextInitialized(context: ReactContext) {
                    if (isAssistPending) {
                        context
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                            .emit("onAssistRequested", null)
                        isAssistPending = false
                    }
                }
            })
            listenerAdded = true
        }
    }
}