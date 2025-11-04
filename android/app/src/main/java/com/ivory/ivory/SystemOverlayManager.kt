package com.ivory.ivory

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat

class SystemOverlayManager : Service() {

    private val TAG = "SystemOverlayManager"

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var originalY = 0

    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SystemOverlayManager service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) {
            Log.d(TAG, "Overlay already visible")
            return
        }

        Log.d(TAG, "Showing system overlay")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.assist_overlay, null)

        // Set overlay width to 96% of screen
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val overlayWidth = (screenWidth * 0.96).toInt()
        overlayView?.findViewById<View>(R.id.overlayCard)?.layoutParams?.width = overlayWidth

        // Create layout params
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 20
            originalY = y
        }

        // Enable blur behind (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            layoutParams?.blurBehindRadius = 25
        }

        // Apply theme
        applyTheme()

        // === CRITICAL: Setup keyboard listener BEFORE adding view ===
        setupKeyboardListener()

        // === Add view to WindowManager ===
        windowManager?.addView(overlayView, layoutParams)
        Log.d(TAG, "Overlay view added to window manager")

        // === Initialize views AFTER addView ===
        val inputField = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipButton = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val micContainer = overlayView?.findViewById<View>(R.id.micContainer)
        val voiceContainer = overlayView?.findViewById<View>(R.id.voiceContainer)
        val sendButton = overlayView?.findViewById<ImageButton>(R.id.sendButton)
        micIcon = overlayView?.findViewById(R.id.micIcon)
        micBlurLayer = overlayView?.findViewById(R.id.micBlurLayer)

        // Input field
        inputField?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener {
                requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }

        inputField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                voiceContainer?.visibility = if (hasText) View.GONE else View.VISIBLE
                sendButton?.visibility = if (hasText) View.VISIBLE else View.GONE
                if (hasText && isListening) stopListeningAnimation()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Buttons
        paperclipButton?.setOnClickListener {
            Log.d(TAG, "Paperclip clicked")
        }

        micContainer?.setOnClickListener {
            Log.d(TAG, "Mic clicked")
            if (!isListening) {
                startListeningAnimation()
                stopListeningHandler.postDelayed({ stopListeningAnimation() }, 5000)
            } else {
                stopListeningAnimation()
            }
        }

        voiceContainer?.setOnClickListener {
            Log.d(TAG, "Voice button clicked")
            openMainApp(null)
            hideOverlay()
        }

        sendButton?.setOnClickListener {
            val query = inputField?.text.toString().trim()
            if (query.isNotEmpty()) {
                Log.d(TAG, "Sending query: $query")
                openMainApp(query)
                hideOverlay()
            }
        }

        // Close on outside touch
        overlayView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideOverlay()
                true
            } else false
        }
    }

    // === KEYBOARD LISTENER (FIXED) ===
    private fun setupKeyboardListener() {
        overlayView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var wasKeyboardOpen = false

            override fun onGlobalLayout() {
                val rect = Rect()
                overlayView?.getWindowVisibleDisplayFrame(rect)
                val screenHeight = overlayView?.rootView?.height ?: return
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardOpen = keypadHeight > screenHeight * 0.15

                if (isKeyboardOpen != wasKeyboardOpen) {
                    wasKeyboardOpen = isKeyboardOpen
                    Log.d(TAG, "Keyboard: $isKeyboardOpen, height=$keypadHeight")

                    layoutParams?.let { params ->
                        params.y = if (isKeyboardOpen) keypadHeight + 20 else originalY
                        try {
                            windowManager?.updateViewLayout(overlayView, params)
                            Log.d(TAG, "Overlay moved to y=${params.y}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update layout", e)
                        }
                    }
                }
            }
        })
    }

    // === THEME ===
    private fun applyTheme() {
        val overlayCard = overlayView?.findViewById<View>(R.id.overlayCard)
        val inputField = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipBtn = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val sendBtn = overlayView?.findViewById<ImageButton>(R.id.sendButton)
        val voiceContainer = overlayView?.findViewById<View>(R.id.voiceContainer)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                     Configuration.UI_MODE_NIGHT_YES

        overlayCard?.background = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        )
        voiceContainer?.background = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        )

        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)

        val iconTint = if (isDark) Color.WHITE else Color.parseColor("#333333")
        paperclipBtn?.setColorFilter(iconTint)
        sendBtn?.setColorFilter(iconTint)
        micIcon?.setColorFilter(iconTint)
    }

    // === MIC ANIMATION ===
    private fun startListeningAnimation() {
        isListening = true
        micIcon?.let { main ->
            micBlurLayer?.let { blur ->
                main.setImageResource(R.drawable.ic_mic_gradient)
                main.clearColorFilter()

                blur.setImageResource(R.drawable.ic_mic_blur_gradient)
                blur.visibility = View.VISIBLE
                blur.alpha = 0.6f

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blur.setRenderEffect(RenderEffect.createBlurEffect(16f, 16f, Shader.TileMode.CLAMP))
                }

                val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.mic_blur_pulse)
                val mainPulse = AnimationUtils.loadAnimation(this, R.anim.mic_pulse)
                blur.startAnimation(pulseAnim)
                main.startAnimation(mainPulse)
            }
        }
    }

    private fun stopListeningAnimation() {
        stopListeningHandler.removeCallbacksAndMessages(null)
        isListening = false

        micIcon?.let { main ->
            micBlurLayer?.let { blur ->
                main.clearAnimation()
                blur.clearAnimation()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blur.setRenderEffect(null)
                }

                blur.visibility = View.GONE

                Handler(Looper.getMainLooper()).postDelayed({
                    main.setImageResource(R.drawable.ic_mic)
                    applyTheme()
                }, 100)
            }
        }
    }

    // === HIDE & DESTROY ===
    private fun hideOverlay() {
        Log.d(TAG, "Hiding overlay")
        stopListeningHandler.removeCallbacksAndMessages(null)
        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
        micIcon = null
        micBlurLayer = null
        isListening = false
        layoutParams = null
        stopSelf()
    }

    private fun openMainApp(query: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        Log.d(TAG, "Service destroyed")
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.ivory.ivory.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.ivory.ivory.HIDE_OVERLAY"

        fun show(context: Context) {
            val intent = Intent(context, SystemOverlayManager::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, SystemOverlayManager::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
        }
    }
}