package com.ivory.ivory

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat

class SystemOverlayManager : Service() {
    companion object {
        const val ACTION_SHOW_OVERLAY = "com.ivory.ivory.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.ivory.ivory.HIDE_OVERLAY"
        private const val TAG = "SystemOverlayManager"

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
    private var micIcon: ImageView? = null
    private var micStroke: ImageView? = null
    private var micContainer: View? = null
    private var voiceContainer: View? = null
    private var sendButton: ImageButton? = null
    private var inputField: EditText? = null
    private var waveformView: WaveformView? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private var originalY = 20

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
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                ?.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, "overlay_channel")
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build()
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InflateParams")
    private fun showOverlay() {
        if (overlayRoot != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted")
            stopSelf()
            return
        }

        wm = getSystemService(WINDOW_SERVICE) as? WindowManager
        if (wm == null) {
            Log.e(TAG, "WindowManager is null")
            stopSelf()
            return
        }

        overlayRoot = LayoutInflater.from(this).inflate(R.layout.assist_overlay, null)

        // Set overlay width to 95% of screen
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val overlayW = (screenW * 0.95f).toInt()
        overlayRoot?.findViewById<View>(R.id.overlayCard)?.layoutParams?.width = overlayW

        // Layout params
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
            y = originalY
            // Blur behind (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = 30
            }
        }

        // Initialize UI elements
        micIcon = overlayRoot?.findViewById(R.id.micIcon)
        micStroke = overlayRoot?.findViewById(R.id.micStroke)
        micContainer = overlayRoot?.findViewById(R.id.micContainer)
        voiceContainer = overlayRoot?.findViewById(R.id.voiceContainer)
        sendButton = overlayRoot?.findViewById(R.id.sendButton)
        inputField = overlayRoot?.findViewById(R.id.inputField)
        waveformView = overlayRoot?.findViewById(R.id.waveformView)

        // Apply theme
        applyTheme()

        // Set up listeners
        setupInputField()
        micContainer?.setOnClickListener {
            if (!isListening) startVoiceRecognition() else stopListeningAnimation()
        }
        sendButton?.setOnClickListener {
            val query = inputField?.text?.toString()?.trim()
            if (!query.isNullOrEmpty()) {
                openMainApp(query)
                hideOverlay()
            }
        }
        voiceContainer?.setOnClickListener {
            openMainApp(null)
            hideOverlay()
        }
        overlayRoot?.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                Log.d(TAG, "Outside touch detected, hiding overlay")
                hideOverlay()
                true
            } else false
        }

        // Add view and start animation
        wm?.addView(overlayRoot, layoutParams)
        overlayRoot?.post {
            overlayRoot?.startAnimation(AnimationUtils.loadAnimation(this@SystemOverlayManager, R.anim.slide_up))
        }

        setupKeyboardListener()
    }

    private fun setupInputField() {
        inputField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                if (hasText && voiceContainer?.visibility == View.VISIBLE) {
                    voiceContainer?.startAnimation(AnimationUtils.loadAnimation(this@SystemOverlayManager, R.anim.fade_out))
                    voiceContainer?.visibility = View.GONE
                    sendButton?.visibility = View.VISIBLE
                    sendButton?.startAnimation(AnimationUtils.loadAnimation(this@SystemOverlayManager, R.anim.fade_in))
                } else if (!hasText && voiceContainer?.visibility == View.GONE) {
                    sendButton?.startAnimation(AnimationUtils.loadAnimation(this@SystemOverlayManager, R.anim.fade_out))
                    sendButton?.visibility = View.GONE
                    voiceContainer?.visibility = View.VISIBLE
                    voiceContainer?.startAnimation(AnimationUtils.loadAnimation(this@SystemOverlayManager, R.anim.fade_in))
                }
                if (hasText && isListening) stopListeningAnimation()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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
                    if (!isKeyboardShowing) {
                        isKeyboardShowing = true
                        adjustOverlayForKeyboard(keypadHeight)
                    }
                } else {
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
            params.y = keyboardHeight + originalY
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

    private fun startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Microphone permission not granted")
            return
        }
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        inputField?.setText(matches[0])
                        sendQueryToReactNative(matches[0])
                    }
                    stopListeningAnimation()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        inputField?.setText(matches[0])
                    }
                }
                override fun onError(error: Int) {
                    Log.e(TAG, "Speech recognition error: $error")
                    stopListeningAnimation()
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer.startListening(recognizerIntent)
            startListeningAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            stopListeningAnimation()
        }
    }

    private fun startListeningAnimation() {
        isListening = true
        micIcon?.setImageResource(R.drawable.ic_mic_active)
        micIcon?.setColorFilter(Color.WHITE)
        micStroke?.visibility = View.VISIBLE
        waveformView?.visibility = View.VISIBLE
        waveformView?.setListening(true)

        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
        micContainer?.startAnimation(bounce)

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

    private fun applyTheme() {
        val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        overlayRoot?.setBackgroundResource(
            if (dark) R.drawable.overlay_background_night else R.drawable.overlay_background
        )
        val textColor = if (dark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (dark) Color.parseColor("#AAAAAA") else Color.parseColor("#888888")
        val iconColor = if (dark) Color.WHITE else Color.parseColor("#333333")

        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        overlayRoot?.findViewById<ImageButton>(R.id.paperclipButton)?.setColorFilter(iconColor)
        sendButton?.setColorFilter(iconColor)
        micIcon?.setColorFilter(iconColor)
    }

    private fun sendQueryToReactNative(query: String) {
        val intent = Intent("com.ivory.ivory.ASSIST_REQUESTED").apply {
            putExtra("query", query)
        }
        sendBroadcast(intent)
    }

    private fun openMainApp(query: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(intent)
    }

    private fun hideOverlay() {
        overlayRoot?.let { view ->
            Log.d(TAG, "Hiding overlay with animation")
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            slideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    try {
                        wm?.removeView(view)
                        overlayRoot = null
                        micIcon = null
                        micStroke = null
                        micContainer = null
                        voiceContainer = null
                        sendButton = null
                        inputField = null
                        waveformView = null
                        layoutParams = null
                        stopSelf()
                        Log.d(TAG, "Overlay removed and service stopped")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing overlay: ${e.message}")
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            view.startAnimation(slideDown)
        } ?: run {
            Log.d(TAG, "Overlay already null, stopping service")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }
}