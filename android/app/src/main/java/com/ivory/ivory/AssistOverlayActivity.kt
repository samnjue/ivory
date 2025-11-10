package com.ivory.ivory

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable

class AssistOverlayActivity : Activity() {
    private val TAG = "AssistOverlayActivity"

    // Views
    private var overlayContainer: View? = null
    private var originalInputCard: FrameLayout? = null
    private var thinkingCard: FrameLayout? = null
    private var responseCard: FrameLayout? = null
    private var responseScrollView: ScrollView? = null
    private var responseContent: LinearLayout? = null
    private var miniInputContainer: FrameLayout? = null
    private var miniInputCard: FrameLayout? = null

    // Original input
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
    private var miniMicBlurLayer: ImageView? = null
    private var miniVoiceContainer: View? = null
    private var miniSendButton: ImageButton? = null

    // State
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    private var currentAnimator: ValueAnimator? = null
    private var lastImeHeight = 0
    private val uiHandler = Handler(Looper.getMainLooper())
    private var thinkingDotsRunnable: Runnable? = null

    private val dummyResponse =
        "Einstein's field equations are the core of Einstein's general theory of relativity. They describe how matter and energy in the universe curve the fabric of spacetime. Essentially, they tell us that the curvature of spacetime is directly related to the energy and momentum of whatever is present. The equations are a set of ten interrelated differential equations..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(R.layout.assist_overlay)

        bindViews()
        setupUi()
        setupImeInsetListener()
        applyTheme()
        applyGradientToTitle()
    }

    private fun bindViews() {
        overlayContainer = findViewById(R.id.overlayContainer)
        originalInputCard = findViewById(R.id.originalInputCard)
        thinkingCard = findViewById(R.id.thinkingCard)
        responseCard = findViewById(R.id.responseCard)
        responseScrollView = findViewById(R.id.responseScrollView)
        responseContent = findViewById(R.id.responseContent)
        miniInputContainer = findViewById(R.id.miniInputContainer)
        miniInputCard = findViewById(R.id.miniInputCard)

        inputField = findViewById(R.id.inputField)
        paperclipButton = findViewById(R.id.paperclipButton)
        micContainer = findViewById(R.id.micContainer)
        micIcon = findViewById(R.id.micIcon)
        micBlurLayer = findViewById(R.id.micBlurLayer)
        voiceContainer = findViewById(R.id.voiceContainer)
        sendButton = findViewById(R.id.sendButton)

        thinkingIvoryStar = findViewById(R.id.thinkingIvoryStar)
        thinkingText = findViewById(R.id.thinkingText)

        aiResponseText = findViewById(R.id.aiResponseText)
        aiResponseTitle = findViewById(R.id.aiResponseTitle)
        aiResponseIcon = findViewById(R.id.aiResponseIcon)
        
        miniInputField = findViewById(R.id.miniInputField)
        miniPaperclipButton = findViewById(R.id.miniPaperclipButton)
        miniMicContainer = findViewById(R.id.miniMicContainer)
        miniMicIcon = findViewById(R.id.miniMicIcon)
        miniMicBlurLayer = findViewById(R.id.miniMicBlurLayer)
        miniVoiceContainer = findViewById(R.id.miniVoiceContainer)
        miniSendButton = findViewById(R.id.miniSendButton)

        setupUi()
        setupImeInsetListener()
        applyTheme()
        applyGradientToTitle()
    }

    private fun setupUi() {
        findViewById<View>(R.id.rootOverlay).setOnClickListener { finishWithoutAnimation() }
        originalInputCard?.setOnClickListener { /* no-op */ }
        responseCard?.setOnClickListener { /* no-op */ }
        miniInputCard?.setOnClickListener { /* no-op */ }

        paperclipButton?.setOnClickListener { Log.d(TAG, "Paperclip tapped") }
        voiceContainer?.setOnClickListener { openMainApp(null); finishWithoutAnimation() }
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
                startListeningAnimation(true)
                stopListeningHandler.postDelayed({ stopListeningAnimation(true) }, 5000)
            } else {
                stopListeningAnimation(true)
            }
        }

        inputField?.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showKeyboard(inputField) }
        inputField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                voiceContainer?.visibility = if (hasText) View.GONE else View.VISIBLE
                sendButton?.visibility = if (hasText) View.VISIBLE else View.GONE
                if (hasText && isListening) stopListeningAnimation(true)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        miniPaperclipButton?.setOnClickListener { Log.d(TAG, "Mini paperclip tapped") }
        miniVoiceContainer?.setOnClickListener { openMainApp(null); finishWithoutAnimation() }
        miniSendButton?.setOnClickListener {
            val text = miniInputField?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                hideKeyboard()
                miniInputField?. OPERtext?.clear()
                startThinkingPhase()
            }
        }

        miniMicContainer?.setOnClickListener {
            if (!isListening) {
                startListeningAnimation(false)
                stopListeningHandler.postDelayed({ stopListeningAnimation(false) }, 5000)
            } else {
                stopListeningAnimation(false)
            }
        }
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
        val maxHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
        responseContent?.measure(
            View.MeasureSpec.makeMeasureSpec(responseScrollView?.width ?: 0, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )
        
        val contentHeight = responseContent?.measuredHeight ?: 0
        val miniHeight = miniInputContainer?.height ?: 0
        val gap = dpToPx(8)
        val total = contentHeight + miniHeight + gap
        val targetHeight = total.coerceAtMost(maxHeight)

        responseScrollView?.layoutParams?.height = targetHeight
        (responseContent?.layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = gap
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

    private fun startListeningAnimation(isOriginal: Boolean) {
        isListening = true
        val micIconView = if (isOriginal) micIcon else miniMicIcon
        val micBlurView = if (isOriginal) micBlurLayer else miniMicBlurLayer
        micIconView?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            clearColorFilter()
            setColorFilter(Color.parseColor("#FF3B30"), PorterDuff.Mode.SRC_IN)
            startAnimation(AnimationUtils.loadAnimation(this@AssistOverlayActivity, R.anim.mic_pulse))
        }
        micBlurView?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            setColorFilter(Color.parseColor("#FF3B30"), PorterDuff.Mode.SRC_IN)
            visibility = View.VISIBLE
            alpha = 0.7f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
            }
            startAnimation(AnimationUtils.loadAnimation(this@AssistOverlayActivity, R.anim.mic_blur_pulse))
        }
    }

    private fun stopListeningAnimation(isOriginal: Boolean) {
        stopListeningHandler.removeCallbacksAndMessages(null)
        isListening = false
        val micIconView = if (isOriginal) micIcon else miniMicIcon
        val micBlurView = if (isOriginal) micBlurLayer else miniMicBlurLayer
        micIconView?.clearAnimation()
        micBlurView?.apply {
            clearAnimation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRenderEffect(null)
            visibility = View.GONE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            micIconView?.setImageResource(R.drawable.ic_mic)
            micIconView?.clearColorFilter()
            applyTheme()
        }, 100)
    }

    private fun setupImeInsetListener() {
        findViewById<View>(R.id.rootOverlay)
            .setOnApplyWindowInsetsListener { _, insets ->
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
            addUpdateListener {
                overlayContainer?.translationY = it.animatedValue as Float
            }
            start()
        }
    }

    private fun applyGradientToTitle() {
        aiResponseTitle?.post {
            val width = aiResponseTitle?.width?.toFloat() ?: 0f
            if (width > 0) {
                val gradient = LinearGradient(
                    0f, 0f, width, 0f,
                    Color.parseColor("#e63946"),
                    Color.parseColor("#4285f4"),
                    Shader.TileMode.CLAMP
                )
                aiResponseTitle?.paint?.shader = gradient
                aiResponseTitle?.invalidate()
            }
        }
    }

    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val iconTint = if (isDark) Color.WHITE else Color.parseColor("#333333")

        // Text
        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        miniInputField?.setTextColor(textColor)
        miniInputField?.setHintTextColor(hintColor)
        thinkingText?.setTextColor(textColor)
        aiResponseText?.setTextColor(textColor)

        // Icons
        listOf(paperclipButton, sendButton, micIcon, miniPaperclipButton, miniSendButton, miniMicIcon).forEach {
            it?.setColorFilter(iconTint)
        }

        // Backgrounds
        val bgRes = if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        val bg = ContextCompat.getDrawable(this, bgRes)
        listOf(originalInputCard, thinkingCard, responseCard).forEach { it?.background = bg }

        // Gradient borders
        val borderRes = if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        val border = ContextCompat.getDrawable(this, borderRes)
        voiceContainer?.background = border
        miniVoiceContainer?.background = border

        // Mini Input Card (now matches Service)
        val cardColor = if (isDark) Color.parseColor("#1E1E1E") else Color.WHITE
        val miniBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(24).toFloat()
            setColor(cardColor)
        }
        miniInputCard?.background = miniBg

        applyGradientToTitle()
    }

    private fun applyCardFixes() {
        originalInputCard?.post {
            (originalInputCard?.getChildAt(0) as? LinearLayout)?.apply {
                clipToPadding = false
                clipChildren = false
            }
            originalInputCard?.requestLayout()
            originalInputCard?.invalidate()
        }

        miniInputCard?.post {
            (miniInputCard?.getChildAt(0) as? LinearLayout)?.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            miniInputCard?.clipToOutline = true
            miniInputCard?.outlineProvider = ViewOutlineProvider.BACKGROUND
            miniInputCard?.requestLayout()
            miniInputCard?.invalidate()
        }

        listOf(thinkingCard, responseCard).forEach {
            it?.post { it?.requestLayout(); it?.invalidate() }
        }
    }

    private fun openMainApp(query: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(intent)
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

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
        applyCardFixes()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListeningHandler.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacksAndMessages(null)
        currentAnimator?.cancel()
        thinkingIvoryStar?.clearAnimation()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}