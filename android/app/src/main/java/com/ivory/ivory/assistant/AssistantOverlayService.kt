package com.ivory.ivory.assistant

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import com.facebook.react.bridge.ReactContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.ivory.ivory.R
import kotlin.math.roundToInt

class AssistantOverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var rootView: View
    private lateinit var overlayCard: View
    private lateinit var inputField: EditText
    private lateinit var paperclipBtn: ImageButton
    private lateinit var micContainer: View
    private lateinit var micIcon: ImageView
    private lateinit var waveformView: WaveformView
    private lateinit var voiceContainer: View
    private lateinit var sendBtn: ImageButton

    private var isLandscape = false
    private var isKeyboardVisible = false
    private var params = WindowManager.LayoutParams()

    // --------------------------------------------------------------------- //
    //  SERVICE LIFECYCLE
    // --------------------------------------------------------------------- //
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlay()
            ACTION_HIDE -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    // --------------------------------------------------------------------- //
    //  OVERLAY CREATION
    // --------------------------------------------------------------------- //
    private fun createOverlay() {
        val inflater = LayoutInflater.from(this)
        rootView = inflater.inflate(R.layout.assistant_overlay, null)

        // ---- find views -------------------------------------------------
        overlayCard = rootView.findViewById(R.id.overlayCard)
        inputField = rootView.findViewById(R.id.inputField)
        paperclipBtn = rootView.findViewById(R.id.paperclipButton)
        micContainer = rootView.findViewById(R.id.micContainer)
        micIcon = rootView.findViewById(R.id.micIcon)
        waveformView = rootView.findViewById(R.id.waveformView)
        voiceContainer = rootView.findViewById(R.id.voiceContainer)
        sendBtn = rootView.findViewById(R.id.sendButton)

        // ---- make the star circle transparent ---------------------------
        voiceContainer.background = null

        // ---- clicks ------------------------------------------------------
        paperclipBtn.setOnClickListener { /* TODO: file picker */ }
        micContainer.setOnClickListener { toggleVoice() }
        sendBtn.setOnClickListener { sendQuery() }
        inputField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard()
        }

        // ---- keyboard detection -----------------------------------------
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenH = rootView.rootView.height
            val keypadH = screenH - r.bottom
            val visible = keypadH > screenH * 0.15
            if (visible != isKeyboardVisible) {
                isKeyboardVisible = visible
                adjustForKeyboard()
            }
        }

        // ---- initial params (portrait) -----------------------------------
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        overlayCard.translationY = 500f          // start off-screen
        overlayCard.alpha = 0f
    }

    // --------------------------------------------------------------------- //
    //  SHOW / HIDE ANIMATIONS
    // --------------------------------------------------------------------- //
    private fun showOverlay() {
        if (rootView.parent != null) return
        wm.addView(rootView, params)

        // slide-up + fade-in
        val slide = ObjectAnimator.ofFloat(overlayCard, "translationY", 500f, 0f)
        val fade = ObjectAnimator.ofFloat(overlayCard, "alpha", 0f, 1f)
        AnimatorSet().apply {
            playTogether(slide, fade)
            duration = 300
            start()
        }

        // make the overlay focusable once it is visible
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        wm.updateViewLayout(rootView, params)
        inputField.requestFocus()
    }

    private fun hideOverlay() {
        if (rootView.parent == null) return

        val slide = ObjectAnimator.ofFloat(overlayCard, "translationY", 0f, 500f)
        val fade = ObjectAnimator.ofFloat(overlayCard, "alpha", 1f, 0f)
        AnimatorSet().apply {
            playTogether(slide, fade)
            duration = 250
            doOnEnd {
                wm.removeView(rootView)
                stopSelf()
            }
            start()
        }
    }

    // --------------------------------------------------------------------- //
    //  VOICE / SEND LOGIC
    // --------------------------------------------------------------------- //
    private var listening = false
    private fun toggleVoice() {
        listening = !listening
        waveformView.setListening(listening)
        micIcon.visibility = if (listening) View.GONE else View.VISIBLE
        // TODO: start/stop SpeechRecognizer
    }

    private fun sendQuery() {
        val query = inputField.text.toString().trim()
        if (query.isEmpty()) return

        // ---- send to React-Native ---------------------------------------
        sendEventToRN("onAssistRequested", mapOf("query" to query))

        // ---- close overlay ------------------------------------------------
        hideOverlay()
    }

    // --------------------------------------------------------------------- //
    //  KEYBOARD HANDLING
    // --------------------------------------------------------------------- //
    private fun showKeyboard() {
        inputField.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun adjustForKeyboard() {
        val margin = if (isKeyboardVisible) 300 else 20 // dp
        val lp = overlayCard.layoutParams as ViewGroup.MarginLayoutParams
        lp.bottomMargin = dpToPx(margin)
        overlayCard.layoutParams = lp
    }

    // --------------------------------------------------------------------- //
    //  ORIENTATION HANDLING
    // --------------------------------------------------------------------- //
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        updateGravity()
    }

    private fun updateGravity() {
        params.gravity = if (isLandscape) {
            Gravity.BOTTOM or Gravity.END
        } else {
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        if (rootView.parent != null) wm.updateViewLayout(rootView, params)
    }

    // --------------------------------------------------------------------- //
    //  UTILS
    // --------------------------------------------------------------------- //
    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).roundToInt()

    // --------------------------------------------------------------------- //
    //  COMMUNICATE WITH REACT-NATIVE
    // --------------------------------------------------------------------- //
    private fun sendEventToRN(eventName: String, params: Map<String, Any>) {
        try {
            val reactContext = (application as com.ivory.ivory.MainApplication)
                .reactNativeHost
                .reactInstanceManager
                .currentReactContext as? ReactContext
            reactContext
                ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?.emit(eventName, com.facebook.react.bridge.Arguments.fromJavaArgs(arrayOf(params)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_SHOW = "com.ivory.ivory.SHOW_ASSISTANT"
        const val ACTION_HIDE = "com.ivory.ivory.HIDE_ASSISTANT"

        /** Called from Java (AssistantModule) */
        @JvmStatic
        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val i = Intent(context, AssistantOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        @JvmStatic
        fun stop(context: Context) {
            val i = Intent(context, AssistantOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(i)
        }
    }
}