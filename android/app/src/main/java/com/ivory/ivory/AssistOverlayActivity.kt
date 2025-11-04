package com.ivory.ivory

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView
import com.facebook.react.ReactNativeHost

class AssistOverlayActivity : Activity() {
    private var reactRootView: ReactRootView? = null
    private var reactInstanceManager: ReactInstanceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make overlay full-width and at the bottom
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Initialize React surface
        val application = application as ReactApplication
        val reactNativeHost: ReactNativeHost = application.reactNativeHost
        reactInstanceManager = reactNativeHost.reactInstanceManager

        reactRootView = ReactRootView(this).apply {
            startReactApplication(
                reactInstanceManager,
                "OverlayInputBar", // must match AppRegistry name
                null
            )
        }

        setContentView(reactRootView)
    }

    override fun onPause() {
        super.onPause()
        reactInstanceManager?.onHostPause(this)
    }

    override fun onResume() {
        super.onResume()
        reactInstanceManager?.onHostResume(this, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        reactInstanceManager?.onHostDestroy(this)
        reactRootView?.unmountReactApplication()
    }
}
