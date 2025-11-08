package com.ivory.ivory

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.widget.*
import androidx.core.content.ContextCompat
import kotlin.math.abs

class IvoryOverlayService : Service() {

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, IvoryOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        @JvmStatic
        fun stop(context: Context) {
            val intent = Intent(context, IvoryOverlayService::class.java)
            context.stopService(intent)
        }
    }

    private val TAG = "IvoryOverlayService"

    // Window Manager
    private lateinit var windowManager: WindowManager
    private lateinit var wmParams: WindowManager.LayoutParams

    // Views
    private var orbView: View? = null
    private var overlayContainer: View? = null
    private var originalInputCard: FrameLayout? = null
    private var thinkingCard: FrameLayout? = null
    private var responseCard: FrameLayout? = null
    private var responseScrollView: ScrollView? = null
    private var miniInputContainer: FrameLayout? = null
    private var miniInputCard: FrameLayout? = null

    // Input views
    private var inputField: EditText? = null
    private var paperclipButton: ImageButton? = null
    private var micContainer: View? = null
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var voiceContainer: View? = null
    private var sendButton: ImageButton? = null

    // Thinking
    private var thinkingIvoryStar: ImageView? = null
    private var thinkingText: TextView? = null

    // Response
    private var aiResponseText: TextView? = null
    private var aiResponseTitle: TextView? = null
    private var aiResponseIcon: ImageView? = null

    // Mini input
    private var miniInputField: EditText? = null
    private var miniPaperclipButton: ImageButton? = null
    private var miniMicContainer: View? = null
    private var miniMicIcon: ImageView? = null
    private var miniVoiceContainer: View? = null
    private var miniSendButton: ImageButton? = null

    // State
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    private var currentAnimator: ValueAnimator? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private var thinkingDotsRunnable: Runnable? = null

    // Orb drag
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // Dummy response
    private val dummyResponse =
        "Einstein's field equations are the core of Einstein's general theory of relativity. They describe how matter and energy in the universe curve the fabric of spacetime. Essentially, they tell us that the curvature of spacetime is directly related to the energy and momentum of whatever is present. The equations are a set of ten interrelated differential equations..."

    private val NOTIFICATION_ID = 1  

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("Ivory Assistant")
            .setContentText("Floating orb active")
            .setSmallIcon(R.drawable.ivorystar)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOrb()
        createOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "overlay_channel",
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("InflateParams")
    private fun createOrb() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        orbView = inflater.inflate(R.layout.ivory_orb, null).apply {
            setOnTouchListener(OrbTouchListener())
        }

        wmParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            gravity = Gravity.TOP or Gravity.START
            width = dpToPx(56)
            height = dpToPx(56)
            x = dpToPx(16)
            y = windowManager.defaultDisplay.height / 2
        }

        windowManager.addView(orbView, wmParams)
    }

    @SuppressLint("InflateParams")
    private fun createOverlay() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayContainer = inflater.inflate(R.layout.assist_overlay, null).apply {
            alpha = 0f
            visibility = View.GONE
        }

        val overlayParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            gravity = Gravity.TOP or Gravity.START
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }

        windowManager.addView(overlayContainer, overlayParams)
        bindOverlayViews()
        setupUi()
        applyTheme()
    }

    private fun bindOverlayViews() {
        overlayContainer?.let { root ->
            originalInputCard = root.findViewById(R.id.originalInputCard)
            thinkingCard = root.findViewById(R.id.thinkingCard)
            responseCard = root.findViewById(R.id.responseCard)
            responseScrollView = root.findViewById(R.id.responseScrollView)
            miniInputContainer = root.findViewById(R.id.miniInputContainer)
            miniInputCard = root.findViewById(R.id.miniInputCard)

            inputField = root.findViewById(R.id.inputField)
            paperclipButton = root.findViewById(R.id.paperclipButton)
            micContainer = root.findViewById(R.id.micContainer)
            micIcon = root.findViewById(R.id.micIcon)
            micBlurLayer = root.findViewById(R.id.micBlurLayer)
            voiceContainer = root.findViewById(R.id.voiceContainer)
            sendButton = root.findViewById(R.id.sendButton)

            thinkingIvoryStar = root.findViewById(R.id.thinkingIvoryStar)
            thinkingText = root.findViewById(R.id.thinkingText)

            aiResponseText = root.findViewById(R.id.aiResponseText)
            aiResponseTitle = root.findViewById(R.id.aiResponseTitle)
            aiResponseIcon = root.findViewById(R.id.aiResponseIcon)

            miniInputField = root.findViewById(R.id.miniInputField)
            miniPaperclipButton = root.findViewById(R.id.miniPaperclipButton)
            miniMicContainer = root.findViewById(R.id.miniMicContainer)
            miniMicIcon = root.findViewById(R.id.miniMicIcon)
            miniVoiceContainer = root.findViewById(R.id.miniVoiceContainer)
            miniSendButton = root.findViewById(R.id.miniSendButton)

            // Remove scroll indicator
            responseScrollView?.isVerticalScrollBarEnabled = false
            responseScrollView?.overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    private fun setupUi() {
        orbView?.setOnClickListener { toggleOverlay() }

        // Outside tap dismisses
        overlayContainer?.findViewById<View>(R.id.rootOverlay)?.setOnClickListener { hideOverlay() }

        originalInputCard?.setOnClickListener { /* no-op */ }
        responseCard?.setOnClickListener { /* no-op */ }
        miniInputCard?.setOnClickListener { /* no-op */ }

        // Original input
        paperclipButton?.setOnClickListener { Log.d(TAG, "Paperclip tapped") }

        voiceContainer?.setOnClickListener {
            openMainApp(null)
            hideOverlay()
        }

        sendButton?.setOnClickListener {
            val text = inputField?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                hideKeyboard()
                inputField?.text?.clear()
                startThinkingPhase()
            }
        }

        micContainer?.setOnClickListener {
            if (!isListening) {
                startListeningAnimation()
                stopListeningHandler.postDelayed({ stopListeningAnimation() }, 5000)
            } else {
                stopListeningAnimation()
            }
        }

        inputField?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard(inputField)
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

        // Mini input
        miniPaperclipButton?.setOnClickListener { Log.d(TAG, "Mini paperclip tapped") }

        miniVoiceContainer?.setOnClickListener {
            openMainApp(null)
            hideOverlay()
        }

        miniSendButton?.setOnClickListener {
            val text = miniInputField?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                hideKeyboard()
                miniInputField?.text?.clear()
                startThinkingPhase()
            }
        }

        miniMicContainer?.setOnClickListener {
            Log.d(TAG, "Mini mic tapped")
        }

        miniInputField?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard(miniInputField)
        }

        miniInputField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                miniVoiceContainer?.visibility = if (hasText) View.GONE else View.VISIBLE
                miniSendButton?.visibility = if (hasText) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun toggleOverlay() {
        if (overlayContainer?.visibility == View.VISIBLE) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    private fun showOverlay() {
        overlayContainer?.visibility = View.VISIBLE
        overlayContainer?.alpha = 0f
        overlayContainer?.animate()?.alpha(1f)?.setDuration(200)?.start()

        positionOverlayNextToOrb()
        originalInputCard?.visibility = View.VISIBLE
        thinkingCard?.visibility = View.GONE
        responseCard?.visibility = View.GONE
    }

    private fun hideOverlay() {
        overlayContainer?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            overlayContainer?.visibility = View.GONE
            resetState()
        }?.start()
    }

    private fun resetState() {
        thinkingDotsRunnable?.let { uiHandler.removeCallbacks(it) }
        thinkingIvoryStar?.clearAnimation()
        aiResponseText?.text = ""
        responseScrollView?.scrollTo(0, 0)
    }

    private fun positionOverlayNextToOrb() {
        val orbLocation = IntArray(2)
        orbView?.getLocationOnScreen(orbLocation)
        val orbX = orbLocation[0]
        val screenWidth = resources.displayMetrics.widthPixels

        val isOnRight = orbX > screenWidth / 2

        val overlayParams = overlayContainer?.layoutParams as WindowManager.LayoutParams
        overlayParams.x = if (isOnRight) {
            orbX - dpToPx(300) // extend left
        } else {
            orbX + dpToPx(56) + dpToPx(8) // extend right
        }
        overlayParams.y = orbLocation[1] - dpToPx(20)
        overlayParams.width = dpToPx(300)
        overlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowManager.updateViewLayout(overlayContainer, overlayParams)
    }

    private inner class OrbTouchListener : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = wmParams.x
                    initialY = wmParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) isDragging = true

                    wmParams.x = (initialX + dx).toInt()
                    wmParams.y = (initialY + dy).toInt()
                    windowManager.updateViewLayout(orbView, wmParams)

                    // Move overlay with orb
                    if (overlayContainer?.visibility == View.VISIBLE) {
                        positionOverlayNextToOrb()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        v.performClick()
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun startThinkingPhase() {
        thinkingDotsRunnable?.let { uiHandler.removeCallbacks(it) }

        originalInputCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            originalInputCard?.visibility = View.GONE
            originalInputCard?.alpha = 1f
        }?.start()

        responseCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            responseCard?.visibility = View.GONE
            responseCard?.alpha = 1f
        }?.start()

        uiHandler.postDelayed({
            thinkingCard?.visibility = View.VISIBLE
            thinkingCard?.alpha = 0f
            thinkingCard?.animate()?.alpha(1f)?.setDuration(200)?.start()

            startSpinningAnimation()
            animateThinkingDots()

            uiHandler.postDelayed({ showResponsePhase() }, 5000)
        }, 200)
    }

    private fun showResponsePhase() {
        thinkingDotsRunnable?.let { uiHandler.removeCallbacks(it) }
        thinkingIvoryStar?.clearAnimation()

        thinkingCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            thinkingCard?.visibility = View.GONE
            thinkingCard?.alpha = 1f

            responseCard?.visibility = View.VISIBLE
            responseCard?.alpha = 0f
            responseCard?.animate()?.alpha(1f)?.setDuration(300)?.start()

            startTypewriterEffect()
        }?.start()
    }

    private fun startTypewriterEffect() {
        aiResponseText?.text = ""
        val words = dummyResponse.split(" ")
        var delay = 0L

        for (word in words) {
            uiHandler.postDelayed({
                val cur = aiResponseText?.text?.toString() ?: ""
                aiResponseText?.text = if (cur.isEmpty()) word else "$cur $word"

                responseScrollView?.post {
                    updateResponseCardHeight()
                    responseScrollView?.fullScroll(View.FOCUS_DOWN)
                }
            }, delay)
            delay += 60
        }
    }

    private fun updateResponseCardHeight() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val maxHeight = (screenHeight * 0.8).toInt()

        aiResponseText?.measure(
            View.MeasureSpec.makeMeasureSpec(responseScrollView?.width ?: 0, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )

        val contentHeight = (aiResponseText?.measuredHeight ?: 0) +
                (16 * displayMetrics.density * 2).toInt() +
                (miniInputContainer?.height ?: 0) +
                100

        val targetHeight = contentHeight.coerceAtMost(maxHeight)

        responseScrollView?.layoutParams?.height = targetHeight
        responseScrollView?.requestLayout()
    }

    private fun animateThinkingDots() {
        val dots = listOf("", ".", "..", "...")
        var idx = 0
        thinkingDotsRunnable = object : Runnable {
            override fun run() {
                if (thinkingCard?.visibility == View.VISIBLE) {
                    thinkingText?.text = "Thinking${dots[idx]}"
                    idx = (idx + 1) % dots.size
                    uiHandler.postDelayed(this, 500)
                }
            }
        }
        thinkingDotsRunnable?.let { uiHandler.post(it) }
    }

    private fun startSpinningAnimation() {
        thinkingIvoryStar?.let { star ->
            val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_indefinite)
            star.startAnimation(anim)
        }
    }

    private fun startListeningAnimation() {
        isListening = true
        micIcon?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            clearColorFilter()
            startAnimation(AnimationUtils.loadAnimation(this@IvoryOverlayService, R.anim.mic_pulse))
        }
        micBlurLayer?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            visibility = View.VISIBLE
            alpha = 0.7f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
            }
            startAnimation(AnimationUtils.loadAnimation(this@IvoryOverlayService, R.anim.mic_blur_pulse))
        }
    }

    private fun stopListeningAnimation() {
        stopListeningHandler.removeCallbacksAndMessages(null)
        isListening = false
        micIcon?.clearAnimation()
        micBlurLayer?.apply {
            clearAnimation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRenderEffect(null)
            visibility = View.GONE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            micIcon?.setImageResource(R.drawable.ic_mic)
            applyTheme()
        }, 100)
    }

    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val iconTintColor = if (isDark) Color.WHITE else Color.parseColor("#333333")

        // Text colors
        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        miniInputField?.setTextColor(textColor)
        miniInputField?.setHintTextColor(hintColor)
        thinkingText?.setTextColor(textColor)
        aiResponseText?.setTextColor(textColor)

        val tintList = ContextCompat.getColorStateList(this,
            if (isDark) R.color.icon_tint_dark else R.color.icon_tint_light
        ) ?: run {
            val fallbackColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
            android.content.res.ColorStateList.valueOf(fallbackColor)
        }

        paperclipButton?.imageTintList = tintList
        sendButton?.imageTintList = tintList
        micIcon?.imageTintList = tintList
        miniPaperclipButton?.imageTintList = tintList
        miniSendButton?.imageTintList = tintList
        miniMicIcon?.imageTintList = tintList

        // Backgrounds
        val bgRes = if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        val bg = ContextCompat.getDrawable(this, bgRes)
        originalInputCard?.background = bg
        thinkingCard?.background = bg
        responseCard?.background = bg
        miniInputCard?.background = bg

        val borderRes = if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        val gradientBorder = ContextCompat.getDrawable(this, borderRes)
        voiceContainer?.background = gradientBorder
        miniVoiceContainer?.background = gradientBorder

        applyGradientToTitle()
    }

    private fun applyGradientToTitle() {
        aiResponseTitle?.post {
            val width = aiResponseTitle?.width?.toFloat() ?: return@post
            if (width <= 0) return@post

            val gradient = LinearGradient(
                0f, 0f, width, 0f,
                Color.parseColor("#e63946"),
                Color.parseColor("#4285f4"),
                Shader.TileMode.CLAMP
            )
            aiResponseTitle?.paint?.shader = gradient
            aiResponseTitle?.invalidate()
        }

        aiResponseIcon?.post {
            val width = aiResponseIcon?.width?.toFloat() ?: return@post
            if (width <= 0) return@post
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val gradient = LinearGradient(
                    0f, 0f, width, 0f,
                    Color.parseColor("#e63946"),
                    Color.parseColor("#4285f4"),
                    Shader.TileMode.CLAMP
                )
                aiResponseIcon?.colorFilter = BlendModeColorFilter(Color.parseColor("#e63946"), BlendMode.SRC_IN)
            } else {
                aiResponseIcon?.setColorFilter(Color.parseColor("#e63946"))
            }
        }
    }

    private fun showKeyboard(view: EditText?) {
        view?.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputField?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        miniInputField?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun openMainApp(query: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(intent)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        orbView?.let { windowManager.removeView(it) }
        overlayContainer?.let { windowManager.removeView(it) }
        stopListeningHandler.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacksAndMessages(null)
        currentAnimator?.cancel()
        thinkingIvoryStar?.clearAnimation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
    }
}