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

    override fun onCreate(savedInstanceState: Bundle?) {
        // Required for expo splash screen
        SplashScreenManager.registerOnActivity(this)
        super.onCreate(null)

        // Safely register a listener after React initializes
        val instanceManager = getReactInstanceManager()
        instanceManager?.addReactInstanceEventListener(object : ReactInstanceEventListener {
            override fun onReactContextInitialized(context: ReactContext) {
                if (isAssistPending) {
                    context
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("onAssistRequested", null)
                    isAssistPending = false
                }
            }
        })

        // Handle any incoming assist intent
        handleAssistIntent(intent)
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
    }

    private fun handleAssistIntent(intent: Intent?) {
        if (intent == null) return

        val showAssist = intent.getBooleanExtra("showAssistOverlay", false)
        val action = intent.action

        if (showAssist || Intent.ACTION_ASSIST == action) {
            val instanceManager = getReactInstanceManager()
            val currentContext = instanceManager?.currentReactContext

            if (currentContext != null) {
                currentContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("onAssistRequested", null)
            } else {
                // React not ready yet — flag it
                isAssistPending = true
            }
        }
    }
}
