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
import androidx.core.graphics.drawable.DrawableCompat

class SystemOverlayManager : Service() {
    private val TAG = "SystemOverlayManager"
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    
    private var originalY = 0
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isKeyboardShowing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SystemOverlayManager service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_OVERLAY) {
            showOverlay()
        } else if (intent?.action == ACTION_HIDE_OVERLAY) {
            hideOverlay()
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

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val overlayWidth = (screenWidth * 0.96).toInt()
        
        overlayView?.findViewById<View>(R.id.overlayCard)?.layoutParams?.width = overlayWidth

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
        )

        layoutParams?.apply {
            gravity = Gravity.BOTTOM
            y = 20
            originalY = y
        }
        
        // Enable blur behind for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            layoutParams?.blurBehindRadius = 25
        }

        applyTheme()
        setupKeyboardListener()

        val inputField = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipButton = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val micContainer = overlayView?.findViewById<View>(R.id.micContainer)
        val voiceContainer = overlayView?.findViewById<View>(R.id.voiceContainer)
        val sendButton = overlayView?.findViewById<ImageButton>(R.id.sendButton)
        micIcon = overlayView?.findViewById(R.id.micIcon)
        micBlurLayer = overlayView?.findViewById(R.id.micBlurLayer)

        // Make input field focusable and show keyboard
        inputField?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener {
                requestFocus()
                // Show keyboard
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
                
                // Stop listening animation if user starts typing
                if (hasText && isListening) {
                    stopListeningAnimation()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Paperclip button (attachment - placeholder functionality)
        paperclipButton?.setOnClickListener {
            Log.d(TAG, "Paperclip clicked - attachment functionality")
        }

        // Mic button click - start listening animation
        micContainer?.setOnClickListener {
            Log.d(TAG, "Mic button clicked")
            if (!isListening) {
                startListeningAnimation()
                // Auto-stop after 5 seconds
                stopListeningHandler.postDelayed({
                    stopListeningAnimation()
                }, 5000)
            } else {
                stopListeningAnimation()
            }
        }

        // Ivory star button click
        voiceContainer?.setOnClickListener {
            Log.d(TAG, "Voice button clicked")
            openMainApp(null)
            hideOverlay()
        }

        // Send button click
        sendButton?.setOnClickListener {
            val query = inputField?.text.toString().trim()
            if (query.isNotEmpty()) {
                Log.d(TAG, "Sending query: $query")
                openMainApp(query)
                hideOverlay()
            }
        }

        // Handle outside touches to close
        overlayView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideOverlay()
                true
            } else {
                false
            }
        }

        windowManager?.addView(overlayView, layoutParams)
        Log.d(TAG, "Overlay view added to window manager")
    }

    private fun setupKeyboardListener() {
        overlayView?.viewTreeObserver?.addOnGlobalLayoutListener {
            val r = Rect()
            overlayView?.getWindowVisibleDisplayFrame(r)
            val screenHeight = overlayView?.rootView?.height ?: 0
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) {   // keyboard visible
                if (!isKeyboardShowing) {
                    isKeyboardShowing = true
                    // Move overlay **above** the keyboard (add a small gap)
                    layoutParams?.y = keypadHeight + 20
                    windowManager?.updateViewLayout(overlayView, layoutParams)
                }
            } else {
                if (isKeyboardShowing) {
                    isKeyboardShowing = false 
                    layoutParams?.y = originalY 
                    windowManager?.updateViewLayout(overlayView, layoutParams)
                }
            }
        }
    }

    private fun adjustOverlayForKeyboard(keyboardHeight: Int) {
        layoutParams?.let { params ->
            params.y = keyboardHeight + 20
            windowManager?.updateViewLayout(overlayView, params)
            Log.d(TAG, "Adjusted overlay for keyboard: y=${params.y}")
        }
    }

    private fun resetOverlayPosition() {
        layoutParams?.let { params ->
            params.y = originalY
            windowManager?.updateViewLayout(overlayView, params)
            Log.d(TAG, "Reset overlay position: y=${params.y}")
        }
    }

    private fun applyTheme() {
        val overlayCard = overlayView?.findViewById<View>(R.id.overlayCard)
        val inputField   = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipBtn = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val sendBtn      = overlayView?.findViewById<ImageButton>(R.id.sendButton)
        val voiceContainer = overlayView?.findViewById<View>(R.id.voiceContainer)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                     Configuration.UI_MODE_NIGHT_YES

        // Card background
        overlayCard?.background = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        )
        voiceContainer?.background = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        )

        // Text colors
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)

        // Icon tint (paper-clip, send, mic)
        val iconTint = if (isDark) Color.WHITE else Color.parseColor("#333333")
        paperclipBtn?.setColorFilter(iconTint)
        sendBtn?.setColorFilter(iconTint)
        micIcon?.setColorFilter(iconTint)          // <-- mic now tinted correctly
    }

    private fun startListeningAnimation() {
        isListening = true

        micIcon?.let { main ->
            micBlurLayer?.let { blur ->

                // Main icon: gradient mic
                main.setImageResource(R.drawable.ic_mic_gradient)
                main.clearColorFilter()

                // Blur layer: larger gradient mic
                blur.setImageResource(R.drawable.ic_mic_blur_gradient)
                blur.visibility = View.VISIBLE
                blur.alpha = 0.6f

                // Apply real blur (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blur.setRenderEffect(
                        RenderEffect.createBlurEffect(16f, 16f, Shader.TileMode.CLAMP)
                    )
                }

                // Pulsate blur layer
                val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.mic_blur_pulse)
                blur.startAnimation(pulseAnim)

                // Subtle pulse on main icon
                val mainPulse = AnimationUtils.loadAnimation(this, R.anim.mic_pulse)
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
                    applyTheme() // re-apply tint
                }, 100)
            }
        }
    }

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
            if (query != null) {
                putExtra("query", query)
            }
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