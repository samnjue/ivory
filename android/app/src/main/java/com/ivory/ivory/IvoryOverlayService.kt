package com.ivory.ivory

import android.animation.*
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.ivory.ivory.R

class IvoryOverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.ivory.ivory.SHOW_ORB"
        const val ACTION_HIDE = "com.ivory.ivory.HIDE_ORB"

        @JvmStatic
        fun start(context: Context) {
            val i = Intent(context, IvoryOverlayService::class.java).apply { action = ACTION_SHOW }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        @JvmStatic
        fun stop(context: Context) {
            val i = Intent(context, IvoryOverlayService::class.java).apply { action = ACTION_HIDE }
            context.startService(i)
        }
    }

    // Views
    private var wm: WindowManager? = null
    private var orbContainer: FrameLayout? = null
    private var cardWindow: FrameLayout? = null
    private var orbWrapper: FrameLayout? = null
    private var orbBlurView: View? = null
    private var gradientBorder: GradientBorderView? = null
    private var starIcon: ImageView? = null
    private var removeZone: View? = null

    // Card Views
    private var inputCard: FrameLayout? = null
    private var originalInputCard: FrameLayout? = null
    private var thinkingCard: FrameLayout? = null
    private var responseCard: FrameLayout? = null
    private var responseScrollView: ScrollView? = null
    private var thinkingIvoryStar: ImageView? = null
    private var thinkingText: TextView? = null
    private var aiResponseText: TextView? = null
    private var aiResponseTitle: TextView? = null
    private var aiResponseIcon: ImageView? = null
    private var inputField: EditText? = null
    private var sendButton: ImageButton? = null
    private var miniInputField: EditText? = null
    private var miniSendButton: ImageButton? = null
    private var paperclipButton: ImageButton? = null
    private var micContainer: View? = null
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var voiceContainer: View? = null
    private var miniPaperclipButton: ImageButton? = null
    private var miniMicContainer: View? = null
    private var miniMicIcon: ImageView? = null
    private var miniVoiceContainer: View? = null

    // Close buttons
    private var originalCloseButton: ImageView? = null
    private var thinkingCloseButton: ImageView? = null
    private var responseCloseButton: ImageView? = null

    // State
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    private var lastImeHeight = 0
    private val uiHandler = Handler(Looper.getMainLooper())
    private var thinkingDotsRunnable: Runnable? = null
    private val dummyResponse = """
        Einstein's field equations are the core of Einstein's general theory of relativity.
        They describe how matter and energy in the universe curve the fabric of spacetime.
        Essentially, they tell us that the curvature of spacetime is directly related to
        the energy and momentum of whatever is present. The equations are a set of ten
        interrelated differential equations...
    """.trimIndent()

    // Drag
    private var initialX = 0; private var initialY = 0
    private var initialTouchX = 0f; private var initialTouchY = 0f
    private var isDragging = false

    // Idle shrink
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable { shrinkOrb() }

    // Window
    private lateinit var orbParams: WindowManager.LayoutParams
    private lateinit var cardParams: WindowManager.LayoutParams
    private var screenW = 0; private var screenH = 0
    private var orbSize = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Foreground Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ivory_overlay_channel",
                "Ivory Assistant Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Floating assistant button"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            val n = NotificationCompat.Builder(this, "ivory_overlay_channel")
                .setContentTitle("Ivory Assistant")
                .setContentText("Floating button is active")
                .setSmallIcon(R.drawable.ivorystar)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
            startForeground(1, n)
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm!!.currentWindowMetrics.bounds
            screenW = b.width(); screenH = b.height()
        } else {
            @Suppress("DEPRECATION")
            wm!!.defaultDisplay?.getRealSize(size)
            screenW = size.x; screenH = size.y
        }

        if (screenW == 0 || screenH == 0) {
            Log.e("IvoryOverlay", "Invalid screen size")
            stopSelf()
            return
        }

        try {
            createOrbOverlay()
            createCardOverlay()
        } catch (e: Exception) {
            Log.e("IvoryOverlay", "createOverlay", e)
            stopSelf()
        }
    }

    private fun createOrbOverlay() {
        orbSize = dp(56)

        orbContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        orbWrapper = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize * 2, orbSize * 2).apply {
                gravity = Gravity.CENTER
            }
            clipToOutline = false
            clipChildren = false
        }

        // Blur background instead of plain black
        orbBlurView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize).apply {
                gravity = Gravity.CENTER
            }
            setBackgroundColor(Color.parseColor("#66000000"))
            elevation = 12f
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) = o.setOval(0, 0, orbSize, orbSize)
            }
            clipToOutline = true

            // Apply blur effect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(16f, 16f, Shader.TileMode.CLAMP))
            }
        }
        orbWrapper!!.addView(orbBlurView)

        // Gradient border
        gradientBorder = GradientBorderView(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize).apply {
                gravity = Gravity.CENTER
            }
        }
        orbWrapper!!.addView(gradientBorder)

        // Star icon
        starIcon = ImageView(this).apply {
            setImageResource(R.drawable.ivorystar)
            layoutParams = FrameLayout.LayoutParams(dp(32), dp(32)).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        orbWrapper!!.addView(starIcon)

        orbContainer!!.addView(orbWrapper)

        // Window params
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        orbParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - orbSize * 2 - dp(16)
            y = screenH / 2
        }

        wm!!.addView(orbContainer, orbParams)

        // Touch listener
        orbWrapper!!.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = orbParams.x; initialY = orbParams.y
                    initialTouchX = ev.rawX; initialTouchY = ev.rawY
                    idleHandler.removeCallbacks(idleRunnable)
                    expandOrb()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - initialTouchX).toInt()
                    val dy = (ev.rawY - initialTouchY).toInt()
                    orbParams.x = initialX + dx; orbParams.y = initialY + dy
                    wm!!.updateViewLayout(orbContainer, orbParams)
                    removeZone?.isVisible = orbParams.y > screenH - dp(120)
                    isDragging = true
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        if (removeZone?.isVisible == true) {
                            stopSelf(); return@setOnTouchListener true
                        }
                        snapOrbToEdge()
                    } else {
                        onOrbTap()
                    }
                    scheduleIdleShrink()
                    true
                }
                else -> false
            }
        }

        // Remove zone
        removeZone = View(this).apply {
            isVisible = false
            setBackgroundResource(R.drawable.remove_zone_bg)
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120)).apply {
                gravity = Gravity.BOTTOM
            }
        }
        val rp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(120),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
        wm!!.addView(removeZone, rp)
    }

    private fun createCardOverlay() {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        cardWindow = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVisible = false
        }

        val inflater = LayoutInflater.from(this)
        inputCard = inflater.inflate(R.layout.assist_overlay, cardWindow, false) as FrameLayout
        inputCard?.isVisible = false
        cardWindow!!.addView(inputCard)

        // Bind views
        originalInputCard = inputCard?.findViewById(R.id.originalInputCard)
        thinkingCard = inputCard?.findViewById(R.id.thinkingCard)
        responseCard = inputCard?.findViewById(R.id.responseCard)
        responseScrollView = inputCard?.findViewById(R.id.responseScrollView)
        thinkingIvoryStar = inputCard?.findViewById(R.id.thinkingIvoryStar)
        thinkingText = inputCard?.findViewById(R.id.thinkingText)
        aiResponseText = inputCard?.findViewById(R.id.aiResponseText)
        aiResponseTitle = inputCard?.findViewById(R.id.aiResponseTitle)
        aiResponseIcon = inputCard?.findViewById(R.id.aiResponseIcon)
        inputField = inputCard?.findViewById(R.id.inputField)
        sendButton = inputCard?.findViewById(R.id.sendButton)
        miniInputField = inputCard?.findViewById(R.id.miniInputField)
        miniSendButton = inputCard?.findViewById(R.id.miniSendButton)
        paperclipButton = inputCard?.findViewById(R.id.paperclipButton)
        micContainer = inputCard?.findViewById(R.id.micContainer)
        micIcon = inputCard?.findViewById(R.id.micIcon)
        micBlurLayer = inputCard?.findViewById(R.id.micBlurLayer)
        voiceContainer = inputCard?.findViewById(R.id.voiceContainer)
        miniPaperclipButton = inputCard?.findViewById(R.id.miniPaperclipButton)
        miniMicContainer = inputCard?.findViewById(R.id.miniMicContainer)
        miniMicIcon = inputCard?.findViewById(R.id.miniMicIcon)
        miniVoiceContainer = inputCard?.findViewById(R.id.miniVoiceContainer)

        addCloseButtons()
        setupOverlayUi()
        applyTheme()
        applyGradientToTitle()

        sendButton?.setOnClickListener { onSend(inputField) }
        miniSendButton?.setOnClickListener { onSend(miniInputField) }

        cardParams = WindowManager.LayoutParams(
            dp(340), dp(420),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        wm!!.addView(cardWindow, cardParams)
    }

    private fun addCloseButtons() {
        listOf(
            Triple(originalInputCard, originalCloseButton) { hideInputCard() },
            Triple(thinkingCard, thinkingCloseButton) { hideInputCard() },
            Triple(responseCard, responseCloseButton) { hideInputCard() }
        ).forEach { (parent, _, action) ->
            val btn = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(dp(32), dp(32)).apply {
                    gravity = Gravity.TOP or Gravity.END
                    setMargins(0, dp(8), dp(8), 0)
                }
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.parseColor("#666666"))
                background = createCircleBackground(Color.parseColor("#22000000"))
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener { action() }
            }
            parent?.addView(btn)
            when (parent) {
                originalInputCard -> originalCloseButton = btn
                thinkingCard -> thinkingCloseButton = btn
                responseCard -> responseCloseButton = btn
            }
        }
    }

    private fun createCircleBackground(color: Int) = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.OVAL
        setColor(color)
    }

    inner class GradientBorderView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(3).toFloat()
        }
        private val gradient = LinearGradient(
            0f, 0f, orbSize.toFloat(), orbSize.toFloat(),
            intArrayOf(Color.parseColor("#e63946"), Color.parseColor("#4285f4")),
            null, Shader.TileMode.CLAMP
        )

        override fun onDraw(canvas: Canvas) {
            paint.shader = gradient
            val radius = orbSize / 2f - paint.strokeWidth / 2f
            canvas.drawCircle(orbSize / 2f, orbSize / 2f, radius, paint)
        }
    }

    private fun setupOverlayUi() {
        inputCard?.findViewById<View>(R.id.rootOverlay)?.setOnClickListener { hideInputCard() }
        originalInputCard?.setOnClickListener { /* prevent click through */ }
        responseCard?.setOnClickListener { /* prevent click through */ }

        paperclipButton?.setOnClickListener { Log.d("Overlay", "Paperclip") }
        voiceContainer?.setOnClickListener { openMainApp(null); hideInputCard() }
        micContainer?.setOnClickListener {
            if (!isListening) {
                startListeningAnimation()
                stopListeningHandler.postDelayed({ stopListeningAnimation() }, 5000)
            } else {
                stopListeningAnimation()
            }
        }

        inputField?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard(inputField)
        }
        miniInputField?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showKeyboard(miniInputField)
        }

        inputField?.addTextChangedListener(watcher { voiceContainer, sendButton })
        miniInputField?.addTextChangedListener(watcher { miniVoiceContainer, miniSendButton })
    }

    private fun watcher(views: () -> Pair<View?, ImageButton?>): TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val (voice, send) = views()
            val hasText = !s.isNullOrEmpty()
            voice?.visibility = if (hasText) View.GONE else View.VISIBLE
            send?.visibility = if (hasText) View.VISIBLE else View.GONE
            if (hasText && isListening) stopListeningAnimation()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun showKeyboard(editText: EditText?) {
        editText?.postDelayed({
            editText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    private fun applyTheme() {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val txt = if (dark) Color.WHITE else Color.parseColor("#333333")
        val hint = if (dark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val icon = if (dark) Color.WHITE else Color.parseColor("#333333")
        listOf(inputField, miniInputField).forEach {
            it?.setTextColor(txt); it?.setHintTextColor(hint)
        }
        listOf(thinkingText, aiResponseText).forEach { it?.setTextColor(txt) }
        listOf(paperclipButton, sendButton, micIcon, miniPaperclipButton, miniSendButton, miniMicIcon).forEach {
            it?.setColorFilter(icon)
        }
        val bg = ContextCompat.getDrawable(this, if (dark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light)
        listOf(originalInputCard, thinkingCard, responseCard).forEach { it?.background = bg }
        val border = ContextCompat.getDrawable(this, if (dark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light)
        listOf(voiceContainer, miniVoiceContainer).forEach { it?.background = border }
        applyGradientToTitle()
    }

    private fun applyGradientToTitle() {
        aiResponseTitle?.post {
            val w = aiResponseTitle?.width?.toFloat() ?: return@post
            val gradient = LinearGradient(0f, 0f, w, 0f,
                Color.parseColor("#e63946"), Color.parseColor("#4285f4"), Shader.TileMode.CLAMP)
            aiResponseTitle?.paint?.shader = gradient
        }
        aiResponseIcon?.setColorFilter(Color.parseColor("#e63946"))
    }

    private fun onOrbTap() {
        cardWindow?.isVisible = true
        inputCard?.isVisible = true
        inputCard?.alpha = 0f
        inputCard?.scaleX = 0.8f; inputCard?.scaleY = 0.8f

        positionCardNearOrb()

        // Enable focus for keyboard
        cardParams.flags = cardParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        wm?.updateViewLayout(cardWindow, cardParams)

        inputCard?.animate()
            ?.alpha(1f)?.scaleX(1f)?.scaleY(1f)
            ?.setDuration(350)?.setInterpolator(DecelerateInterpolator())
            ?.withEndAction {
                originalInputCard?.visibility = View.VISIBLE
                showKeyboard(inputField)
            }?.start()
    }

    private fun positionCardNearOrb() {
        val cardWidth = dp(340)
        val cardHeight = dp(420)
        val spacing = dp(12)
        val offsetY = dp(20)

        val orbCenterX = orbParams.x + orbSize
        val isRight = orbCenterX > screenW / 2

        val x = if (isRight) {
            (orbParams.x - cardWidth - spacing).coerceAtLeast(dp(8))
        } else {
            (orbParams.x + orbSize * 2 + spacing).coerceAtMost(screenW - cardWidth - dp(8))
        }

        val y = (orbParams.y + orbSize + offsetY).coerceIn(dp(8), screenH - cardHeight - dp(8))

        cardParams.x = x
        cardParams.y = y
        wm?.updateViewLayout(cardWindow, cardParams)
    }

    private fun hideInputCard() {
        hideKeyboard()
        inputCard?.animate()
            ?.alpha(0f)?.scaleX(0.8f)?.scaleY(0.8f)
            ?.setDuration(300)
            ?.withEndAction {
                inputCard?.isVisible = false
                cardWindow?.isVisible = false
                cardParams.flags = cardParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                wm?.updateViewLayout(cardWindow, cardParams)
                resetCardState()
                scheduleIdleShrink()
            }?.start()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        listOf(inputField, miniInputField).forEach {
            it?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        }
    }

    private fun resetCardState() {
        originalInputCard?.visibility = View.VISIBLE
        thinkingCard?.visibility = View.GONE
        responseCard?.visibility = View.GONE
    }

    private fun expandOrb() {
        orbWrapper?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(200)?.start()
        scheduleIdleShrink()
    }

    private fun shrinkOrb() {
        orbWrapper?.animate()?.scaleX(0.8f)?.scaleY(0.8f)?.setDuration(300)?.start()
    }

    private fun scheduleIdleShrink() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, 3000)
    }

    private fun snapOrbToEdge() {
        val margin = dp(16)
        val tx = if (orbParams.x + orbSize < screenW / 2) margin else screenW - orbSize * 2 - margin
        val ty = orbParams.y.coerceIn(margin, screenH - orbSize * 2 - margin)

        ObjectAnimator.ofPropertyValuesHolder(
            orbParams,
            PropertyValuesHolder.ofInt("x", orbParams.x, tx),
            PropertyValuesHolder.ofInt("y", orbParams.y, ty)
        ).apply {
            duration = 300; interpolator = DecelerateInterpolator()
            addUpdateListener {
                orbParams.x = it.getAnimatedValue("x") as Int
                orbParams.y = it.getAnimatedValue("y") as Int
                wm!!.updateViewLayout(orbContainer, orbParams)
            }
            start()
        }
    }

    private fun onSend(edit: EditText?) {
        val text = edit?.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return
        edit.text.clear()
        hideKeyboard()
        originalInputCard?.animate()
            ?.alpha(0f)?.scaleX(0.9f)?.scaleY(0.9f)
            ?.setDuration(200)
            ?.withEndAction { startThinkingPhase() }
            ?.start()
    }

    private fun startThinkingPhase() {
        originalInputCard?.visibility = View.GONE
        responseCard?.visibility = View.GONE
        thinkingCard?.visibility = View.VISIBLE
        thinkingCard?.alpha = 0f
        thinkingCard?.animate()?.alpha(1f)?.setDuration(200)?.start()
        startSpinningAnimation()
        animateThinkingDots()
        uiHandler.postDelayed({ showResponsePhase() }, 5000)
    }

    private fun showResponsePhase() {
        thinkingDotsRunnable?.let { uiHandler.removeCallbacks(it) }
        thinkingIvoryStar?.clearAnimation()
        thinkingCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            thinkingCard?.visibility = View.GONE
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
                responseScrollView?.fullScroll(View.FOCUS_DOWN)
            }, delay)
            delay += 60
        }
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
        thinkingIvoryStar?.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_indefinite))
    }

    private fun startListeningAnimation() {
        isListening = true
        micIcon?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            clearColorFilter()
            startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@IvoryOverlayService, R.anim.mic_pulse))
        }
        micBlurLayer?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            visibility = View.VISIBLE
            alpha = 0.7f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
            }
            startAnimation(android.view.animation.AnimationUtils.loadAnimation(this@IvoryOverlayService, R.anim.mic_blur_pulse))
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

    private fun openMainApp(query: String?) {
        val i = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(i)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                orbContainer?.visibility = View.VISIBLE
                removeZone?.visibility = View.GONE
            }
            ACTION_HIDE -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        idleHandler.removeCallbacksAndMessages(null)
        stopListeningHandler.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacksAndMessages(null)
        thinkingIvoryStar?.clearAnimation()
        wm?.let {
            listOfNotNull(orbContainer, cardWindow, removeZone).forEach { v -> it.removeView(v) }
        }
        super.onDestroy()
    }

    private fun dp(dp: Int) = (resources.displayMetrics.density * dp).toInt()
}