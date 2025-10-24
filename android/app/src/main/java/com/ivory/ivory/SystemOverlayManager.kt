package com.ivory.ivory

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat

class SystemOverlayManager : Service() {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.ivory.ivory.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.ivory.ivory.HIDE_OVERLAY"

        fun show(context: Context) {
            val i = Intent(context, SystemOverlayManager::class.java).apply { action = ACTION_SHOW_OVERLAY }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }

        fun hide(context: Context) {
            val i = Intent(context, SystemOverlayManager::class.java).apply { action = ACTION_HIDE_OVERLAY }
            context.startService(i)
        }
    }

    private var wm: WindowManager? = null
    private var overlayRoot: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // UI refs
    private var micIcon: ImageView? = null
    private var micStroke: ImageView? = null
    private var voiceContainer: View? = null
    private var sendButton: ImageButton? = null
    private var inputField: EditText? = null

    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private var originalY = 0

    private var waveformView: WaveformView? = null

    // ---------- Service lifecycle ----------
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "overlay_channel", "Assistant Overlay",
                NotificationManager.IMPORTANCE_MIN
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)

            // **Invisible** notification (transparent icon + no text)
            val n = NotificationCompat.Builder(this, "overlay_channel")
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build()
            startForeground(1, n)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    // ---------- Overlay creation ----------
    @SuppressLint("InflateParams")
    private fun showOverlay() {
        if (overlayRoot != null) return

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayRoot = LayoutInflater.from(this).inflate(R.layout.assist_overlay, null)

        // ---------- Width = 95% ----------
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val overlayW = (screenW * 0.95f).toInt()
        overlayRoot?.findViewById<View>(R.id.overlayCard)?.layoutParams?.width = overlayW

        // ---------- Layout params ----------
        layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 20
            originalY = y
        }

        // Blur behind (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            layoutParams?.blurBehindRadius = 30
        }

        wm?.addView(overlayRoot, layoutParams)
        overlayRoot?.post {
        overlayRoot?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
        }
        setupKeyboardListener()

        // ---------- UI refs ----------
        micIcon = overlayRoot?.findViewById(R.id.micIcon)
        micStroke = overlayRoot?.findViewById(R.id.micStroke)
        voiceContainer = overlayRoot?.findViewById(R.id.voiceContainer)
        sendButton = overlayRoot?.findViewById(R.id.sendButton)
        inputField = overlayRoot?.findViewById(R.id.inputField)

        // ---------- Theme ----------
        applyTheme()

        // ---------- Input handling ----------
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

        // ---------- Mic click ----------
        overlayRoot?.findViewById<View>(R.id.micContainer)
            ?.setOnClickListener {
                if (!isListening) startListeningAnimation()
                else stopListeningAnimation()
            }

        // ---------- Send ----------
        sendButton?.setOnClickListener {
            val q = inputField?.text?.toString()?.trim()
            if (!q.isNullOrEmpty()) {
                openMainApp(q)
                hideOverlay()
            }
        }

        // ---------- Ivory star (open app) ----------
        voiceContainer?.setOnClickListener {
            openMainApp(null)
            hideOverlay()
        }

        // ---------- Outside touch → dismiss ----------
        overlayRoot?.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                Log.d(TAG, "Outside touch detected, hiding overlay")
                hideOverlay()
                true
            } else false
        }
        
        wm?.addView(overlayRoot, layoutParams)

        // ---------- Slide-in animation ----------
        overlayRoot?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))

        waveformView = overlayRoot?.findViewById(R.id.waveformView)
    }

    private fun setupKeyboardListener() {
        overlayRoot?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var isKeyboardShowing = false

            override fun onGlobalLayout() {
                val rect = Rect()
                overlayRoot?.getWindowVisibleDisplayFrame(rect)
                val screenHeight = overlayRoot?.rootView?.height ?: 0
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
            wm?.updateViewLayout(overlayRoot, params)
            Log.d(TAG, "Adjusted overlay for keyboard: y=${params.y}")
        }
    }

    private fun resetOverlayPosition() {
        layoutParams?.let { params ->
            params.y = originalY
            wm?.updateViewLayout(overlayRoot, params)
            Log.d(TAG, "Reset overlay position: y=${params.y}")
        }
    }

    // ---------- Listening animation ----------
    private fun startListeningAnimation() {
        isListening = true
        micIcon?.setImageResource(R.drawable.ic_mic_active)
        micIcon?.setColorFilter(Color.WHITE)
        micStroke?.visibility = View.VISIBLE
        waveformView?.visibility = View.VISIBLE
        waveformView?.setListening(true)

        val pulse = AnimationUtils.loadAnimation(this, R.anim.mic_pulse)
        micStroke?.startAnimation(pulse)

        handler.postDelayed({ stopListeningAnimation() }, 5000)
    }

    private fun stopListeningAnimation() {
        handler.removeCallbacksAndMessages(null)
        isListening = false
        micStroke?.clearAnimation()
        micStroke?.visibility = View.GONE
        waveformView?.visibility = View.GONE
        waveformView?.setListening(false)
        applyTheme()
    }

    // ---------- Theme ----------
    private fun applyTheme() {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val textColor = if (dark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (dark) Color.parseColor("#AAAAAA") else Color.parseColor("#888888")
        val iconColor = if (dark) Color.WHITE else Color.parseColor("#333333")

        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        overlayRoot?.findViewById<ImageButton>(R.id.paperclipButton)
            ?.setColorFilter(iconColor)
        sendButton?.setColorFilter(iconColor)
        micIcon?.setColorFilter(iconColor)
    }

    // ---------- Dismiss ----------
    private fun hideOverlay() {
        overlayRoot?.let {
            Log.d(TAG, "Hiding overlay with animation")
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            slideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    try {
                        wm?.removeView(it)
                        overlayRoot = null
                        micIcon = null
                        micStroke = null
                        voiceContainer = null
                        sendButton = null
                        inputField = null
                        layoutParams = null
                        stopSelf()
                        Log.d(TAG, "Overlay removed and service stopped")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing overlay: ${e.message}")
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            it.startAnimation(slideDown)
        } ?: run {
            Log.d(TAG, "Overlay already null, stopping service")
            stopSelf()
        }
    }

    private fun openMainApp(query: String?) {
        val i = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(i)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}