package com.ivory.ivory

import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.ivory.ivory.R
import com.ivory.ivory.WaveView
import android.app.Notification
import android.content.res.Configuration
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat

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

    // ------------------------------------------------------------------------
    // Views & state
    // ------------------------------------------------------------------------
    private var wm: WindowManager? = null
    private var orb: ImageView? = null
    private var wave: WaveView? = null
    private var orbSize = 0
    private var orbWrapper: FrameLayout? = null
    private var inputCard: FrameLayout? = null
    private var removeZone: View? = null

    // cards
    private var originalInputCard: FrameLayout? = null
    private var thinkingCard: FrameLayout? = null
    private var responseCard: FrameLayout? = null
    private var responseScrollView: ScrollView? = null
    private var thinkingIvoryStar: ImageView? = null
    private var thinkingText: TextView? = null
    private var aiResponseText: TextView? = null
    private var aiResponseTitle: TextView? = null
    private var aiResponseIcon: ImageView? = null

    // input fields
    private var inputField: EditText? = null
    private var sendButton: ImageButton? = null
    private var miniInputField: EditText? = null
    private var miniSendButton: ImageButton? = null

    // original-input UI
    private var paperclipButton: ImageButton? = null
    private var micContainer: View? = null
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var voiceContainer: View? = null

    // mini-input UI
    private var miniInputContainer: FrameLayout? = null
    private var miniInputCard: FrameLayout? = null
    private var miniPaperclipButton: ImageButton? = null
    private var miniMicContainer: View? = null
    private var miniMicIcon: ImageView? = null
    private var miniVoiceContainer: View? = null

    // containers
    private var overlayContainer: View? = null

    // state
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    private var currentAnimator: ValueAnimator? = null
    private var lastImeHeight = 0
    private val uiHandler = Handler(Looper.getMainLooper())
    private var thinkingDotsRunnable: Runnable? = null

    private val dummyResponse =
        "Einstein's field equations are the core of Einstein's general theory of relativity. " +
                "They describe how matter and energy in the universe curve the fabric of spacetime. " +
                "Essentially, they tell us that the curvature of spacetime is directly related to " +
                "the energy and momentum of whatever is present. The equations are a set of ten " +
                "interrelated differential equations..."

    // drag
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // idle shrink
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable { shrinkOrb() }

    // window params
    private lateinit var orbParams: WindowManager.LayoutParams
    private var screenW = 0
    private var screenH = 0

    // ------------------------------------------------------------------------
    // Service lifecycle
    // ------------------------------------------------------------------------
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ivory_overlay_channel"
            val channelName = "Ivory Assistant Overlay"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Floating assistant button"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Ivory Assistant")
                .setContentText("Floating button is active")
                .setSmallIcon(R.drawable.ivorystar)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
            
            startForeground(1, notification)
        }
        
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else wm!!.defaultDisplay
        val size = Point()
        display?.getRealSize(size)
        screenW = size.x
        screenH = size.y

        createOverlay()
    }

    // ------------------------------------------------------------------------
    // Overlay creation
    // ------------------------------------------------------------------------
    private fun createOverlay() {
        // ----- ROOT (full-screen) -----
        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // ----- REMOVE ZONE -----
        removeZone = View(this).apply {
            isVisible = false
            setBackgroundResource(R.drawable.remove_zone_bg)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(120)
            ).apply { gravity = Gravity.BOTTOM }
            root.addView(this)
        }

        // ----- ORB -----
        val orbSize = dp(56)
        this.orbSize = orbSize
        orbWrapper = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize)
            clipToOutline = true
            elevation = 12f
        }

        wave = WaveView(this@IvoryOverlayService).apply {
            isVisible = false
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize)
        }
        orbWrapper!!.addView(wave)

        val border = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize)
            background = ContextCompat.getDrawable(this@IvoryOverlayService, R.drawable.gradient_border_light)
            clipToOutline = true
        }
        orbWrapper!!.addView(border)

        val star = ImageView(this).apply {
            setImageResource(R.drawable.ivorystar)
            layoutParams = FrameLayout.LayoutParams(dp(32), dp(32)).apply { gravity = Gravity.CENTER }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        orbWrapper!!.addView(star)
        orb = star

        orbParams = WindowManager.LayoutParams(
            orbSize, orbSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - orbSize - dp(16)
            y = screenH / 2
        }
        wm!!.addView(orbWrapper, orbParams)

        // ----- ORB TOUCH -----
        orbWrapper!!.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = orbParams.x
                    initialY = orbParams.y
                    initialTouchX = ev.rawX
                    initialTouchY = ev.rawY
                    idleHandler.removeCallbacks(idleRunnable)
                    expandOrb()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (ev.rawX - initialTouchX).toInt()
                    val dy = (ev.rawY - initialTouchY).toInt()
                    orbParams.x = initialX + dx
                    orbParams.y = initialY + dy
                    wm!!.updateViewLayout(orbWrapper, orbParams)
                    val bottomThreshold = screenH - dp(120)
                    removeZone?.isVisible = orbParams.y > bottomThreshold
                    isDragging = true
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        if (removeZone?.isVisible == true) {
                            stopSelf()
                            return@setOnTouchListener true
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

        // ----- INPUT CARD -----
        val inflater = LayoutInflater.from(this)
        inputCard = inflater.inflate(R.layout.assist_overlay, root, false) as FrameLayout

        // hide everything except the original card
        inputCard?.findViewById<FrameLayout>(R.id.originalInputCard)?.isVisible = true
        inputCard?.findViewById<FrameLayout>(R.id.responseCard)?.isVisible = false
        inputCard?.findViewById<FrameLayout>(R.id.thinkingCard)?.isVisible = false
        inputCard?.isVisible = false
        root.addView(inputCard)

        // ----- BIND VIEWS -----
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

        overlayContainer = inputCard?.findViewById(R.id.overlayContainer)
        miniInputContainer = inputCard?.findViewById(R.id.miniInputContainer)
        miniInputCard = inputCard?.findViewById(R.id.miniInputCard)

        paperclipButton = inputCard?.findViewById(R.id.paperclipButton)
        micContainer = inputCard?.findViewById(R.id.micContainer)
        micIcon = inputCard?.findViewById(R.id.micIcon)
        micBlurLayer = inputCard?.findViewById(R.id.micBlurLayer)
        voiceContainer = inputCard?.findViewById(R.id.voiceContainer)

        miniPaperclipButton = inputCard?.findViewById(R.id.miniPaperclipButton)
        miniMicContainer = inputCard?.findViewById(R.id.miniMicContainer)
        miniMicIcon = inputCard?.findViewById(R.id.miniMicIcon)
        miniVoiceContainer = inputCard?.findViewById(R.id.miniVoiceContainer)

        // ----- UI SETUP -----
        setupOverlayUi()
        setupImeInsetListener()
        applyTheme()
        applyGradientToTitle()

        sendButton?.setOnClickListener { onSend(inputField) }
        miniSendButton?.setOnClickListener { onSend(miniInputField) }

        // ----- ADD ROOT TO WINDOW -----
        wm!!.addView(
            root,
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                orbParams.type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
        )
    }

    // ------------------------------------------------------------------------
    // UI wiring (identical to AssistOverlayActivity)
    // ------------------------------------------------------------------------
    private fun setupOverlayUi() {
        // outside tap → hide card
        inputCard?.findViewById<View>(R.id.rootOverlay)
            ?.setOnClickListener { hideInputCard() }

        // prevent bubbling
        originalInputCard?.setOnClickListener { /* no-op */ }
        responseCard?.setOnClickListener { /* no-op */ }
        miniInputCard?.setOnClickListener { /* no-op */ }

        // ----- ORIGINAL INPUT -----
        paperclipButton?.setOnClickListener { Log.d("Overlay", "Paperclip tapped") }

        voiceContainer?.setOnClickListener {
            openMainApp(null)
            hideInputCard()
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
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
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

        // ----- MINI INPUT -----
        miniPaperclipButton?.setOnClickListener { Log.d("Overlay", "Mini paperclip tapped") }

        miniVoiceContainer?.setOnClickListener {
            openMainApp(null)
            hideInputCard()
        }

        miniMicContainer?.setOnClickListener { Log.d("Overlay", "Mini mic tapped") }

        miniInputField?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(miniInputField, InputMethodManager.SHOW_IMPLICIT)
            }
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

    // ------------------------------------------------------------------------
    // Keyboard handling
    // ------------------------------------------------------------------------
    private fun setupImeInsetListener() {
        inputCard?.findViewById<View>(R.id.rootOverlay)
            ?.setOnApplyWindowInsetsListener { _, insets ->
                val imeHeight = insets.getInsets(WindowInsets.Type.ime()).bottom
                if (imeHeight != lastImeHeight) {
                    lastImeHeight = imeHeight
                    animateOverlayForKeyboard(imeHeight)
                }
                insets
            }
    }

    private fun animateOverlayForKeyboard(imeHeight: Int) {
        currentAnimator?.cancel()
        val from = overlayContainer?.translationY ?: 0f
        val extraLift = (20 * resources.displayMetrics.density).toInt()
        val to = if (imeHeight > 0) -(imeHeight + extraLift).toFloat() else 0f

        currentAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = 250
            addUpdateListener { overlayContainer?.translationY = it.animatedValue as Float }
            start()
        }
    }

    // ------------------------------------------------------------------------
    // Theming
    // ------------------------------------------------------------------------
    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val iconTint = if (isDark) Color.WHITE else Color.parseColor("#333333")

        // text
        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        miniInputField?.setTextColor(textColor)
        miniInputField?.setHintTextColor(hintColor)
        thinkingText?.setTextColor(textColor)
        aiResponseText?.setTextColor(textColor)

        // icons
        paperclipButton?.setColorFilter(iconTint)
        sendButton?.setColorFilter(iconTint)
        micIcon?.setColorFilter(iconTint)
        miniPaperclipButton?.setColorFilter(iconTint)
        miniSendButton?.setColorFilter(iconTint)
        miniMicIcon?.setColorFilter(iconTint)

        // backgrounds
        val bg: Drawable? = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        )
        originalInputCard?.background = bg
        thinkingCard?.background = bg
        responseCard?.background = bg
        miniInputCard?.background = bg

        // gradient borders
        val gradientBorder: Drawable? = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        )
        voiceContainer?.background = gradientBorder
        miniVoiceContainer?.background = gradientBorder

        applyGradientToTitle()
    }

    // ------------------------------------------------------------------------
    // Gradient title / icon
    // ------------------------------------------------------------------------
    private fun applyGradientToTitle() {
        aiResponseTitle?.post {
            val w = aiResponseTitle?.width?.toFloat() ?: return@post
            val gradient = LinearGradient(
                0f, 0f, w, 0f,
                Color.parseColor("#e63946"),
                Color.parseColor("#4285f4"),
                Shader.TileMode.CLAMP
            )
            aiResponseTitle?.paint?.shader = gradient
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            aiResponseIcon?.post {
                val w = aiResponseIcon?.width?.toFloat() ?: return@post
                val gradient = LinearGradient(
                    0f, 0f, w, 0f,
                    Color.parseColor("#e63946"),
                    Color.parseColor("#4285f4"),
                    Shader.TileMode.CLAMP
                )
                aiResponseIcon?.setColorFilter(
                    BlendModeColorFilter(Color.parseColor("#e63946"), BlendMode.SRC_IN)
                )
            }
        } else {
            aiResponseIcon?.setColorFilter(Color.parseColor("#e63946"))
        }
    }

    // ------------------------------------------------------------------------
    // Mic listening animation
    // ------------------------------------------------------------------------
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

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private fun hideInputCard() {
        inputCard?.animate()
            ?.alpha(0f)
            ?.scaleX(0.3f)
            ?.scaleY(0.3f)
            ?.translationX(orbParams.x + orbSize / 2f - screenW / 2f)
            ?.translationY(orbParams.y + orbSize / 2f - dp(80))
            ?.setDuration(300)
            ?.withEndAction {
                inputCard?.isVisible = false
                scheduleIdleShrink()
            }?.start()
    }

    private fun openMainApp(query: String?) {
        val i = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(i)
    }

    private fun onSend(editText: EditText?) {
        val text = editText?.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return
        editText.text.clear()
        hideKeyboard()
        startThinkingPhase()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputField?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        miniInputField?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    // ------------------------------------------------------------------------
    // Thinking → Response flow (unchanged)
    // ------------------------------------------------------------------------
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
        thinkingIvoryStar?.let { star ->
            val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_indefinite)
            star.startAnimation(anim)
        }
    }

    // ------------------------------------------------------------------------
    // Orb tap / drag helpers
    // ------------------------------------------------------------------------
    private fun onOrbTap() {
        wave?.isVisible = true
        wave?.startAnimation()
        inputCard?.isVisible = true
        inputCard?.alpha = 0f
        inputCard?.scaleX = 0.3f
        inputCard?.scaleY = 0.3f
        inputCard?.translationX = (orbParams.x + orbSize / 2f - screenW / 2).toFloat()
        inputCard?.translationY = (orbParams.y + orbSize / 2f - dp(80)).toFloat()

        inputCard?.animate()
            ?.alpha(1f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.translationX(0f)
            ?.translationY(0f)
            ?.setDuration(350)
            ?.setInterpolator(DecelerateInterpolator())
            ?.withEndAction { originalInputCard?.visibility = View.VISIBLE }
            ?.start()

        applyTheme()   // refresh theme every time the card appears
    }

    private fun snapOrbToEdge() {
        val targetX = if (orbParams.x > screenW / 2) screenW - dp(72) else dp(16)
        val xHolder = PropertyValuesHolder.ofInt("x", orbParams.x, targetX)
        ObjectAnimator.ofPropertyValuesHolder(orbParams, xHolder).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                orbParams.x = it.getAnimatedValue("x") as Int
                wm!!.updateViewLayout(orbWrapper, orbParams)
            }
            start()
        }
    }

    private fun expandOrb() {
        orbWrapper?.animate()
            ?.scaleX(1.2f)
            ?.scaleY(1.2f)
            ?.alpha(1f)
            ?.setDuration(200)
            ?.start()
        wave?.isVisible = false
        scheduleIdleShrink()
    }

    private fun shrinkOrb() {
        orbWrapper?.animate()
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.alpha(0.6f)
            ?.setDuration(300)
            ?.start()
    }

    private fun scheduleIdleShrink() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, 3000)
    }

    // ------------------------------------------------------------------------
    // Service commands
    // ------------------------------------------------------------------------
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                orbWrapper?.visibility = View.VISIBLE
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
        currentAnimator?.cancel()
        thinkingIvoryStar?.clearAnimation()
        wm?.let {
            orbWrapper?.let { v -> it.removeView(v) }
        }
        super.onDestroy()
    }

    private fun dp(dp: Int) = (resources.displayMetrics.density * dp).toInt()
}