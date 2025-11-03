package com.ivory.ivory

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
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
        overlayView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var isKeyboardShowing = false
            
            override fun onGlobalLayout() {
                val rect = Rect()
                overlayView?.getWindowVisibleDisplayFrame(rect)
                val screenHeight = overlayView?.rootView?.height ?: 0
                val keypadHeight = screenHeight - rect.bottom

                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is showing
                    if (!isKeyboardShowing) {
                        isKeyboardShowing = true
                        adjustOverlayForKeyboard(keypadHeight)
                    }
                } else {
                    // Keyboard is hidden
                    if (isKeyboardShowing) {
                        isKeyboardShowing = false
                        resetOverlayPosition()
                    }
                }
            }
        })
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
        val inputField = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipButton = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val sendButton = overlayView?.findViewById<ImageButton>(R.id.sendButton)
        
        val isDarkMode = (resources.configuration.uiMode and 
                         Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Update overlay background based on theme
        overlayCard?.background = ContextCompat.getDrawable(
            this, 
            if (isDarkMode) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        )

        // Update gradient border inner color based on theme
        val voiceContainer = overlayView?.findViewById<View>(R.id.voiceContainer)
        voiceContainer?.background = ContextCompat.getDrawable(
            this,
            if (isDarkMode) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        )

        val iconColor = if (isDarkMode) Color.WHITE else Color.parseColor("#333333")
        
        if (isDarkMode) {
            // Dark mode
            inputField?.setTextColor(Color.WHITE)
            inputField?.setHintTextColor(Color.parseColor("#88FFFFFF"))
        } else {
            // Light mode
            inputField?.setTextColor(Color.parseColor("#333333"))
            inputField?.setHintTextColor(Color.parseColor("#88333333"))
        }
        
        // Apply color filter to icons
        paperclipButton?.setColorFilter(iconColor)
        sendButton?.setColorFilter(iconColor)
        micIcon?.setColorFilter(iconColor)
    }

    private fun startListeningAnimation() {
        isListening = true
        micIcon?.let { icon ->
            micBlurLayer?.let { blur ->
                // Use the gradient mic drawable
                icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_gradient))
                icon.clearColorFilter()
                
                // Show and setup blur layer with same gradient drawable
                blur.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_gradient))
                blur.clearColorFilter()
                blur.visibility = View.VISIBLE
                blur.alpha = 0.6f
                
                // Start animations
                val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.mic_pulse)
                val blurPulse = AnimationUtils.loadAnimation(this, R.anim.mic_blur_pulse)
                
                icon.startAnimation(pulseAnim)
                blur.startAnimation(blurPulse)
                
                Log.d(TAG, "Started listening animation")
            }
        }
    }

    private fun stopListeningAnimation() {
        stopListeningHandler.removeCallbacksAndMessages(null)
        isListening = false
        
        micIcon?.let { icon ->
            micBlurLayer?.let { blur ->
                // Clear animations
                icon.clearAnimation()
                blur.clearAnimation()
                
                // Fade out blur layer
                val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
                fadeOut.fillAfter = true
                blur.startAnimation(fadeOut)
                
                // Delay hiding elements and resetting icon
                Handler(Looper.getMainLooper()).postDelayed({
                    blur.visibility = View.GONE
                    
                    // Reset to normal icon based on theme
                    icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic))
                    val isDarkMode = (resources.configuration.uiMode and 
                                     Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    icon.setColorFilter(if (isDarkMode) Color.WHITE else Color.parseColor("#333333"))
                }, 300)
                
                Log.d(TAG, "Stopped listening animation")
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