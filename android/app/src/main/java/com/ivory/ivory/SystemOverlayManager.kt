package com.ivory.ivory

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class SystemOverlayManager : Service() {
    private val TAG = "SystemOverlayManager"
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "overlay_channel"
    
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SystemOverlayManager service created")
        startForegroundService()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Assistant Overlay Channel",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Channel for assistant overlay service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Assistant Overlay")
                .setContentText("Running assistant overlay")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(Notification.PRIORITY_MIN)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started")
        }
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

        val params = WindowManager.LayoutParams(
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

        params.gravity = Gravity.BOTTOM
        params.y = 20
        
        // Enable blur behind for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.blurBehindRadius = 25 // 5dp blur converted to pixels roughly
        }

        // Apply theme-based styling
        applyTheme()

        // Find views
        val inputField = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipButton = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val voiceContainer = overlayView?.findViewById<View>(R.id.voiceContainer)
        val sendButton = overlayView?.findViewById<ImageButton>(R.id.sendButton)
        micIcon = overlayView?.findViewById(R.id.micIcon)
        micBlurLayer = overlayView?.findViewById(R.id.micBlurLayer)

        // Setup text watcher for input
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

        // Voice button click - start listening animation
        voiceContainer?.setOnClickListener {
            Log.d(TAG, "Voice button clicked")
            if (!isListening) {
                startListeningAnimation()
                // Auto-stop after 5 seconds
                stopListeningHandler.postDelayed({
                    stopListeningAnimation()
                }, 5000)
            } else {
                openMainApp(null)
                hideOverlay()
            }
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

        windowManager?.addView(overlayView, params)
        Log.d(TAG, "Overlay view added to window manager")
    }

    private fun applyTheme() {
        val overlayCard = overlayView?.findViewById<CardView>(R.id.overlayCard)
        val inputField = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipButton = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val sendButton = overlayView?.findViewById<ImageButton>(R.id.sendButton)
        
        val isDarkMode = (resources.configuration.uiMode and 
                         Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (isDarkMode) {
            // Dark mode: #151515 with 50% opacity
            overlayCard?.setCardBackgroundColor(Color.parseColor("#80151515"))
            inputField?.setTextColor(Color.WHITE)
            inputField?.setHintTextColor(Color.parseColor("#AAAAAA"))
            paperclipButton?.setColorFilter(Color.WHITE)
            sendButton?.setColorFilter(Color.WHITE)
        } else {
            // Light mode: White with 40% opacity
            overlayCard?.setCardBackgroundColor(Color.parseColor("#66FFFFFF"))
            inputField?.setTextColor(Color.parseColor("#333333"))
            inputField?.setHintTextColor(Color.parseColor("#888888"))
            paperclipButton?.setColorFilter(Color.parseColor("#333333"))
            sendButton?.setColorFilter(Color.parseColor("#333333"))
        }
    }

    private fun startListeningAnimation() {
        isListening = true
        micIcon?.let { icon ->
            micBlurLayer?.let { blur ->
                // Show gradient version
                icon.setImageResource(R.drawable.ic_mic_gradient)
                
                // Show and animate blur layer
                blur.visibility = View.VISIBLE
                val isDarkMode = (resources.configuration.uiMode and 
                                 Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                blur.setColorFilter(if (isDarkMode) Color.WHITE else Color.parseColor("#333333"))
                
                val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                blur.startAnimation(fadeIn)
                blur.alpha = 0.6f
                
                // Start pulse animation
                val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.mic_pulse)
                icon.startAnimation(pulseAnim)
                blur.startAnimation(pulseAnim)
                
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
                
                // Delay hiding blur layer and resetting icon
                Handler(Looper.getMainLooper()).postDelayed({
                    blur.visibility = View.GONE
                    // Reset to normal white/dark icon based on theme
                    icon.setImageResource(R.drawable.ic_mic)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hide(context: Context) {
            val intent = Intent(context, SystemOverlayManager::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
        }
    }
}