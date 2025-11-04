package com.ivory.ivory

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.util.Log
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView
import com.facebook.react.ReactNativeHost
import com.facebook.react.bridge.ReactContext

class AssistOverlayActivity : Activity() {
    private var reactRootView: ReactRootView? = null
    private var reactInstanceManager: ReactInstanceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("AssistOverlayActivity", "onCreate called")

        // Configure window
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Set up React environment
        val application = application as ReactApplication
        val reactNativeHost: ReactNativeHost = application.reactNativeHost
        reactInstanceManager = reactNativeHost.reactInstanceManager

        reactRootView = ReactRootView(this)

        // 🧠 Ensure the React context is ready before rendering
        val currentContext: ReactContext? = reactInstanceManager?.currentReactContext

        if (currentContext != null) {
            Log.d("AssistOverlayActivity", "React context already available — starting immediately")
            startOverlayReactView()
        } else {
            Log.d("AssistOverlayActivity", "React context not ready — waiting for initialization")
            reactInstanceManager?.addReactInstanceEventListener(object :
                ReactInstanceManager.ReactInstanceEventListener {
                override fun onReactContextInitialized(context: ReactContext) {
                    Log.d("AssistOverlayActivity", "React context initialized — starting overlay")
                    startOverlayReactView()
                    reactInstanceManager?.removeReactInstanceEventListener(this)
                }
            })
            reactInstanceManager?.createReactContextInBackground()
        }
    }

    private fun startOverlayReactView() {
        reactRootView?.startReactApplication(
            reactInstanceManager,
            "OverlayInputBar", // Must match AppRegistry.registerComponent name
            null
        )
        setContentView(reactRootView)
    }

    override fun onResume() {
        super.onResume()
        reactInstanceManager?.onHostResume(this, null)
    }

    override fun onPause() {
        super.onPause()
        reactInstanceManager?.onHostPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        reactInstanceManager?.onHostDestroy(this)
        reactRootView?.unmountReactApplication()
    }
}
