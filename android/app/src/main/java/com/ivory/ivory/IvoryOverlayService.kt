package com.ivory.ivory

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.widget.*
import androidx.core.app.NotificationCompat
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
            context.stopService(Intent(context, IvoryOverlayService::class.java))
        }
    }

    private val TAG = "IvoryOverlayService"
    private lateinit var windowManager: WindowManager

    // Views
    private var orbRoot: View? = null
    private var orbGradient: ImageView? = null
    private var orbTintRing: ImageView? = null
    private var orbStar: ImageView? = null
    private var removePill: View? = null
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

    // Layout params
    private lateinit var orbParams: WindowManager.LayoutParams
    private lateinit var pillParams: WindowManager.LayoutParams

    // State
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    private val uiHandler = Handler(Looper.getMainLooper())
    private var thinkingDotsRunnable: Runnable? = null

    // Drag state
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var isOverRemove = false

    // Constants
    private val ORB_SIZE = 56
    private val PILL_MARGIN = 80
    private val SNAP_THRESHOLD = 100
    private val INPUT_CARD_HEIGHT_DP = 56
    private val VOICE_BUTTON_SIZE_DP = 30
    private val MINI_INPUT_HEIGHT_DP = 48

    // Dummy response
    private val dummyResponse =
        "Einstein's field equations are the core of Einstein's general theory of relativity. They describe how matter and energy in the universe curve the fabric of spacetime. Essentially, they tell us that the curvature of spacetime is directly related to the energy and momentum of whatever is present. The equations are a set of ten interrelated differential equations..."
    
    private val NOTIFICATION_ID = 1

    // Service lifecycle
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
        createRemovePill()
        createOverlay()
    }

    // Notification channel
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "overlay_channel",
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Orb creation
    @SuppressLint("InflateParams")
    private fun createOrb() {
        val inflater = LayoutInflater.from(this)
        orbRoot = inflater.inflate(R.layout.ivory_orb, null).apply {
            orbGradient = findViewById(R.id.orb_gradient)
            orbTintRing = findViewById(R.id.orb_tint_ring)
            orbStar = findViewById(R.id.orb_star)
            setOnTouchListener(OrbTouchListener())
            setOnClickListener { toggleOverlay() }
        }
        
        orbParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            gravity = Gravity.TOP or Gravity.START
            width = dpToPx(ORB_SIZE)
            height = dpToPx(ORB_SIZE)
            x = dpToPx(16)
            y = windowManager.defaultDisplay.height / 2
        }
        windowManager.addView(orbRoot, orbParams)
    }

    // Remove-pill
    @SuppressLint("InflateParams")
    private fun createRemovePill() {
        val inflater = LayoutInflater.from(this)
        removePill = inflater.inflate(R.layout.remove_pill, null)
        pillParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            y = PILL_MARGIN
        }
        windowManager.addView(removePill, pillParams)
    }

    // Overlay creation
    @SuppressLint("InflateParams")
    private fun createOverlay() {
        val inflater = LayoutInflater.from(this)
        overlayContainer = inflater.inflate(R.layout.assist_overlay, null).apply {
            alpha = 0f
            visibility = View.GONE
            clipChildren = false
            clipToPadding = false
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
        applyCardFixes()
    }

    // View binding
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

            responseScrollView?.isVerticalScrollBarEnabled = false
            responseScrollView?.overScrollMode = View.OVER_SCROLL_NEVER

            // Disable clipping on key containers
            responseCard?.clipChildren = false
            responseCard?.clipToPadding = false
            miniInputContainer?.clipChildren = false
            miniInputContainer?.clipToPadding = false
            thinkingCard?.clipChildren = false
            thinkingCard?.clipToPadding = false
            originalInputCard?.clipChildren = false
            originalInputCard?.clipToPadding = false
        }
    }

    // UI setup
    private fun setupUi() {
        overlayContainer?.findViewById<View>(R.id.rootOverlay)
            ?.setOnClickListener { hideOverlay() }

        // Original input
        paperclipButton?.setOnClickListener { Log.d(TAG, "Paperclip tapped") }
        voiceContainer?.setOnClickListener { openMainApp(null); hideOverlay() }
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
        inputField?.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showKeyboard(inputField) }
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
        miniVoiceContainer?.setOnClickListener { openMainApp(null); hideOverlay() }
        miniSendButton?.setOnClickListener {
            val text = miniInputField?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                hideKeyboard()
                miniInputField?.text?.clear()
                startThinkingPhase()
            }
        }
        miniMicContainer?.setOnClickListener { Log.d(TAG, "Mini mic tapped") }
        miniInputField?.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showKeyboard(miniInputField) }
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

    // Touch / Drag
    private inner class OrbTouchListener : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && (abs(dx) > 10 || abs(dy) > 10)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        val newX = (orbParams.x + dx).toInt()
                        val newY = (orbParams.y + dy).toInt()

                        val screenW = resources.displayMetrics.widthPixels
                        val screenH = resources.displayMetrics.heightPixels
                        val orbW = dpToPx(ORB_SIZE)
                        val orbH = dpToPx(ORB_SIZE)

                        orbParams.x = newX.coerceIn(0, screenW - orbW)
                        orbParams.y = newY.coerceIn(0, screenH - orbH)

                        windowManager.updateViewLayout(orbRoot, orbParams)
                        
                        updateRemovePillVisibility(orbParams.y + orbH)
                        checkOverlapWithRemovePill()
                        if (overlayContainer?.visibility == View.VISIBLE) {
                            positionOverlayNextToOrb()
                        }

                        // Reset for next move
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        v.performClick()
                    } else {
                        snapToEdgeOrRemove()
                    }
                    isDragging = false
                    hideRemovePill()
                    resetOrbColor()
                    return true
                }
            }
            return false
        }
    }

    // Snap-to-edge
    private fun snapToEdgeOrRemove() {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        if (isOverRemove) {
            hideOrb()
            return
        }

        val leftDist = orbParams.x
        val rightDist = screenW - (orbParams.x + dpToPx(ORB_SIZE))
        val topDist = orbParams.y
        val bottomDist = screenH - (orbParams.y + dpToPx(ORB_SIZE))

        val targetX: Int
        val targetY: Int
        
        when {
            leftDist < SNAP_THRESHOLD -> {
                targetX = dpToPx(16)
                targetY = orbParams.y.coerceIn(dpToPx(50), screenH - dpToPx(ORB_SIZE) - dpToPx(50))
            }
            rightDist < SNAP_THRESHOLD -> {
                targetX = screenW - dpToPx(ORB_SIZE) - dpToPx(16)
                targetY = orbParams.y.coerceIn(dpToPx(50), screenH - dpToPx(ORB_SIZE) - dpToPx(50))
            }
            topDist < SNAP_THRESHOLD -> {
                targetX = orbParams.x.coerceIn(dpToPx(16), screenW - dpToPx(ORB_SIZE) - dpToPx(16))
                targetY = dpToPx(50)
            }
            bottomDist < SNAP_THRESHOLD -> {
                targetX = orbParams.x.coerceIn(dpToPx(16), screenW - dpToPx(ORB_SIZE) - dpToPx(16))
                targetY = screenH - dpToPx(ORB_SIZE) - dpToPx(50)
            }
            else -> {
                targetX = if (leftDist < rightDist) dpToPx(16) else screenW - dpToPx(ORB_SIZE) - dpToPx(16)
                targetY = if (topDist < bottomDist) dpToPx(50) else screenH - dpToPx(ORB_SIZE) - dpToPx(50)
            }
        }

        animateSnap(targetX, targetY)
    }

    private fun animateSnap(targetX: Int, targetY: Int) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val prog = anim.animatedValue as Float
                orbParams.x = (orbParams.x + (targetX - orbParams.x) * prog).toInt()
                orbParams.y = (orbParams.y + (targetY - orbParams.y) * prog).toInt()
                windowManager.updateViewLayout(orbRoot, orbParams)
            }
            start()
        }
    }

    // Red-tint
    private fun setOrbRed() {
        orbTintRing?.alpha = 0.6f
        orbTintRing?.setColorFilter(Color.parseColor("#FF3B30"), PorterDuff.Mode.SRC_ATOP)
    }

    private fun resetOrbColor() {
        orbTintRing?.alpha = 0f
        orbTintRing?.clearColorFilter()
    }

    private fun setPillRed() {
        removePill?.background = ContextCompat.getDrawable(this, R.drawable.remove_pill_red)
    }

    private fun setPillBlack() {
        removePill?.background = ContextCompat.getDrawable(this, R.drawable.remove_pill_background)
    }

    // Remove-pill visibility & overlap
    private fun updateRemovePillVisibility(orbBottomY: Int) {
        val screenHeight = resources.displayMetrics.heightPixels
        val bottomQuadrant = screenHeight * 0.75f
        if (orbBottomY > bottomQuadrant) showRemovePill() else hideRemovePill()
    }

    private fun showRemovePill() {
        if (removePill?.visibility != View.VISIBLE) {
            removePill?.visibility = View.VISIBLE
            removePill?.alpha = 0f
            removePill?.animate()?.alpha(1f)?.setDuration(200)?.start()
        }
    }

    private fun hideRemovePill() {
        removePill?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            removePill?.visibility = View.GONE
        }?.start()
    }

    private fun checkOverlapWithRemovePill() {
        val pillLoc = IntArray(2)
        removePill?.getLocationOnScreen(pillLoc) ?: return
        val pillRect = Rect(
            pillLoc[0], pillLoc[1],
            pillLoc[0] + (removePill?.width ?: 0),
            pillLoc[1] + (removePill?.height ?: 0)
        )
        val orbLoc = IntArray(2)
        orbRoot?.getLocationOnScreen(orbLoc)
        val orbCenterX = orbLoc[0] + dpToPx(ORB_SIZE) / 2
        val orbCenterY = orbLoc[1] + dpToPx(ORB_SIZE) / 2
        val over = pillRect.contains(orbCenterX.toInt(), orbCenterY.toInt())
        if (over != isOverRemove) {
            isOverRemove = over
            if (over) {
                setOrbRed()
                setPillRed()
            } else {
                resetOrbColor()
                setPillBlack()
            }
        }
    }

    private fun hideOrb() {
        orbRoot?.animate()
            ?.scaleX(0.3f)?.scaleY(0.3f)?.alpha(0f)?.setDuration(200)
            ?.withEndAction { stopSelf() }
            ?.start()
    }

    // Overlay show / hide
    private fun toggleOverlay() {
        if (overlayContainer?.visibility == View.VISIBLE) hideOverlay() else showOverlay()
    }

    private fun showOverlay() {
        overlayContainer?.visibility = View.VISIBLE
        overlayContainer?.alpha = 0f
        overlayContainer?.animate()?.alpha(1f)?.setDuration(200)?.start()
        positionOverlayNextToOrb()
        originalInputCard?.visibility = View.VISIBLE
        thinkingCard?.visibility = View.GONE
        responseCard?.visibility = View.GONE
        applyTheme() 
        applyCardFixes() 
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
        val loc = IntArray(2)
        orbRoot?.getLocationOnScreen(loc)
        val orbX = loc[0]
        val screenW = resources.displayMetrics.widthPixels
        val onRight = orbX > screenW / 2

        val params = overlayContainer?.layoutParams as WindowManager.LayoutParams
        params.x = if (onRight) orbX - dpToPx(300) else orbX + dpToPx(56) + dpToPx(8)
        params.y = loc[1] - dpToPx(20)
        params.width = dpToPx(300)
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        windowManager.updateViewLayout(overlayContainer, params)
    }

    // Thinking / response phases
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
            applyTheme() 
            applyCardFixes()
            thinkingCard?.requestLayout()
            thinkingCard?.invalidate()
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
            applyTheme()
            applyCardFixes()
            responseCard?.requestLayout()
            responseCard?.invalidate()
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
        val dm = resources.displayMetrics
        val maxH = (dm.heightPixels * 0.8).toInt()
        aiResponseText?.measure(
            View.MeasureSpec.makeMeasureSpec(responseScrollView?.width ?: 0, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )
        val contentH = (aiResponseText?.measuredHeight ?: 0) +
                (16 * dm.density * 2).toInt() +
                (miniInputContainer?.height ?: 0) + 100
        val targetH = contentH.coerceAtMost(maxH)
        responseScrollView?.layoutParams?.height = targetH
        responseScrollView?.requestLayout()
        responseCard?.invalidate()
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

    // Listening animation
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

    // Theme + Card Fixes
    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")

        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        miniInputField?.setTextColor(textColor)
        miniInputField?.setHintTextColor(hintColor)
        thinkingText?.setTextColor(textColor)
        aiResponseText?.setTextColor(textColor)

        val tint = if (isDark) Color.WHITE else Color.parseColor("#333333")
        paperclipButton?.imageTintList = android.content.res.ColorStateList.valueOf(tint)
        sendButton?.imageTintList = android.content.res.ColorStateList.valueOf(tint)
        micIcon?.imageTintList = android.content.res.ColorStateList.valueOf(tint)
        miniPaperclipButton?.imageTintList = android.content.res.ColorStateList.valueOf(tint)
        miniSendButton?.imageTintList = android.content.res.ColorStateList.valueOf(tint)
        miniMicIcon?.imageTintList = android.content.res.ColorStateList.valueOf(tint)

        val bgRes = if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        val bg = ContextCompat.getDrawable(this, bgRes)
        originalInputCard?.background = bg
        thinkingCard?.background = bg
        responseCard?.background = bg

        val borderRes = if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        val border = ContextCompat.getDrawable(this, borderRes)
        voiceContainer?.background = border
        miniVoiceContainer?.background = border

        // Special pill background for mini input card
        val cardColor = if (isDark) Color.parseColor("#1E1E1E") else Color.WHITE
        val miniBg = GradientDrawable()
        miniBg.shape = GradientDrawable.RECTANGLE
        miniBg.cornerRadius = dpToPx(24).toFloat()
        miniBg.setColor(cardColor)
        miniInputCard?.background = miniBg

        applyGradientToTitle()
    }

    private fun applyCardFixes() {
        // Original Input Card
        originalInputCard?.apply {
            post {
                val lp = layoutParams
                lp.height = dpToPx(INPUT_CARD_HEIGHT_DP)
                layoutParams = lp

                // Voice button smaller
                voiceContainer?.layoutParams?.apply {
                    width = dpToPx(VOICE_BUTTON_SIZE_DP)
                    height = dpToPx(VOICE_BUTTON_SIZE_DP)
                }

                // Input field
                inputField?.apply {
                    textSize = 15f
                    setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                }
                requestLayout()
                invalidate()
            }
        }

        // Mini Input Card
        miniInputCard?.apply {
            post {
                val lp = layoutParams as? FrameLayout.LayoutParams
                lp?.let {
                    it.height = dpToPx(MINI_INPUT_HEIGHT_DP)
                    it.width = WindowManager.LayoutParams.WRAP_CONTENT
                    it.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    it.bottomMargin = dpToPx(8)
                }
                layoutParams = lp
                
                (getChildAt(0) as? LinearLayout)?.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
                clipToOutline = true
                outlineProvider = ViewOutlineProvider.BACKGROUND
                requestLayout()
                invalidate()
            }
        }
        // Invalidate other cards to ensure redraw
        thinkingCard?.post {
            thinkingCard?.requestLayout()
            thinkingCard?.invalidate()
        }
        responseCard?.post {
            responseCard?.requestLayout()
            responseCard?.invalidate()
        }
    }
    private fun applyGradientToTitle() {
        aiResponseTitle?.post {
            val w = aiResponseTitle?.width?.toFloat() ?: return@post
            if (w <= 0) return@post
            val grad = LinearGradient(
                0f, 0f, w, 0f,
                Color.parseColor("#e63946"), Color.parseColor("#4285f4"),
                Shader.TileMode.CLAMP
            )
            aiResponseTitle?.paint?.shader = grad
            aiResponseTitle?.invalidate()
        }
        aiResponseIcon?.post {
            aiResponseIcon?.setColorFilter(Color.parseColor("#e63946"))
        }
    }

    // Keyboard helpers
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

    // Service lifecycle
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        orbRoot?.let { windowManager.removeView(it) }
        removePill?.let { windowManager.removeView(it) }
        overlayContainer?.let { windowManager.removeView(it) }
        stopListeningHandler.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacksAndMessages(null)
        thinkingIvoryStar?.clearAnimation()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
        applyCardFixes()
    }
}