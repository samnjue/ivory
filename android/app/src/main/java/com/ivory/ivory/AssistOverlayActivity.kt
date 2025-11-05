package com.ivory.ivory

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
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

class AssistOverlayActivity : Activity() {
    private val TAG = "AssistOverlayActivity"

    // ==== ORIGINAL VIEWS ====
    private var overlayContainer: View? = null
    private var overlayCard: View? = null
    private var inputField: EditText? = null
    private var paperclipButton: ImageButton? = null
    private var micContainer: View? = null
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var voiceContainer: View? = null
    private var sendButton: ImageButton? = null

    // ==== NEW VIEWS (AI flow) ====
    private var responseCard: FrameLayout? = null
    private var thinkingCard: FrameLayout? = null
    private var thinkingIvoryStar: ImageView? = null
    private var thinkingText: TextView? = null
    private var responseScrollView: ScrollView? = null
    private var aiResponseText: TextView? = null
    private var miniInputBar: View? = null
    private var miniInputField: EditText? = null
    private var miniSendButton: ImageButton? = null

    // ==== STATE ====
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    private var currentAnimator: ValueAnimator? = null
    private var lastImeHeight = 0
    private val uiHandler = Handler(Looper.getMainLooper())

    // ==== DUMMY RESPONSE ====
    private val dummyResponse =
        "Einstein’s field equations are the core of Einstein’s general theory of relativity. They describe how matter and energy in the universe curve the fabric of spacetime. Essentially, they tell us that the curvature of spacetime is directly related to the energy and momentum of whatever is present. The equations are a set of ten interrelated differential equations..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(R.layout.assist_overlay)

        // ==== ORIGINAL BINDING ====
        overlayContainer = findViewById(R.id.overlayContainer)
        overlayCard = findViewById(R.id.originalInputCard)
        inputField = findViewById(R.id.inputField)
        paperclipButton = findViewById(R.id.paperclipButton)
        micContainer = findViewById(R.id.micContainer)
        micIcon = findViewById(R.id.micIcon)
        micBlurLayer = findViewById(R.id.micBlurLayer)
        voiceContainer = findViewById(R.id.voiceContainer)
        sendButton = findViewById(R.id.sendButton)

        // ==== NEW BINDING ====
        responseCard = findViewById(R.id.responseCard)
        thinkingCard = findViewById(R.id.thinkingCard)
        thinkingIvoryStar = findViewById(R.id.thinkingIvoryStar)
        thinkingText = findViewById(R.id.thinkingText)
        responseScrollView = findViewById(R.id.responseScrollView)
        aiResponseText = findViewById(R.id.aiResponseText)
        miniInputBar = findViewById(R.id.miniInputBar)
        miniInputField = miniInputBar?.findViewById(R.id.miniInputField)
        miniSendButton = miniInputBar?.findViewById(R.id.miniSendButton)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val maxResponseHeight = (screenHeight * 0.8).toInt()  // 80% of screen
        val maxHeightLayoutParams = responseCard?.layoutParams as? LinearLayout.LayoutParams
        maxHeightLayoutParams?.height = maxHeightLayoutParams?.height?.coerceAtMost(maxResponseHeight) ?: maxResponseHeight
        responseCard?.layoutParams = maxHeightLayoutParams

        setupUi()
        setupImeInsetListener()
        applyTheme()
    }

    // ==================== ORIGINAL UI SETUP ====================
    private fun setupUi() {
        // Outside tap
        findViewById<View>(R.id.rootOverlay).setOnClickListener { finishWithoutAnimation() }
        findViewById<View>(R.id.overlayCard).setOnClickListener { /* no-op */ }

        // Paper-clip
        paperclipButton?.setOnClickListener { Log.d(TAG, "Paperclip tapped") }

        // Ivory-star (voice)
        voiceContainer?.setOnClickListener {
            openMainApp(null)
            finishWithoutAnimation()
        }

        // ==== ORIGINAL SEND (kept) ====
        sendButton?.setOnClickListener {
            val text = inputField?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                // **New behaviour** – show AI flow instead of opening main app
                hideKeyboard()
                startThinkingPhase()
                uiHandler.postDelayed({ showResponsePhase() }, 5000)
            }
        }

        // ==== MINI SEND (new) ====
        miniSendButton?.setOnClickListener {
            val text = miniInputField?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                hideKeyboard()
                miniInputField?.text?.clear()
                startThinkingPhase()
                uiHandler.postDelayed({ showResponsePhase() }, 5000)
            }
        }

        // Mic listening
        micContainer?.setOnClickListener {
            if (!isListening) {
                startListeningAnimation()
                stopListeningHandler.postDelayed({ stopListeningAnimation() }, 5000)
            } else {
                stopListeningAnimation()
            }
        }

        // Input focus + icon switch
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

        // Mini input text watcher
        miniInputField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                miniSendButton?.visibility = if (!s.isNullOrEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Outside-tap dismiss
        findViewById<View>(R.id.rootOverlay).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                finishWithoutAnimation()
                true
            } else false
        }

        // Start spinning star for thinking card (once)
        thinkingIvoryStar?.let { startSpinningAnimation(it) }
    }

    // ==================== THINKING → RESPONSE FLOW ====================
    private fun startThinkingPhase() {
        // Fade out original input
        overlayCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            overlayCard?.visibility = View.GONE
        }?.start()

        // Show thinking card
        thinkingCard?.visibility = View.VISIBLE
        thinkingCard?.alpha = 0f
        thinkingCard?.animate()?.alpha(1f)?.setDuration(200)?.start()

        animateThinkingDots()
    }

    private fun showResponsePhase() {
        // Fade out thinking
        thinkingCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            thinkingCard?.visibility = View.GONE

            // Show response + mini input
            responseCard?.visibility = View.VISIBLE
            responseCard?.alpha = 0f
            responseCard?.animate()?.alpha(1f)?.setDuration(300)?.start()

            miniInputBar?.visibility = View.VISIBLE
            miniInputBar?.alpha = 0f
            miniInputBar?.animate()?.alpha(1f)?.setDuration(300)?.start()

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
        val runnable = object : Runnable {
            override fun run() {
                thinkingText?.text = "Thinking${dots[idx]}"
                idx = (idx + 1) % dots.size
                uiHandler.postDelayed(this, 500)
            }
        }
        uiHandler.post(runnable)
    }

    private fun startSpinningAnimation(view: ImageView) {
        val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_indefinite)
        view.startAnimation(anim)
    }

    // ==================== ORIGINAL MIC ANIMATIONS ====================
    private fun startListeningAnimation() {
        isListening = true
        micIcon?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            clearColorFilter()
            startAnimation(AnimationUtils.loadAnimation(this@AssistOverlayActivity, R.anim.mic_pulse))
        }
        micBlurLayer?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            visibility = View.VISIBLE
            alpha = 0.7f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
            }
            startAnimation(AnimationUtils.loadAnimation(this@AssistOverlayActivity, R.anim.mic_blur_pulse))
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

    // ==================== KEYBOARD INSET ====================
    private fun setupImeInsetListener() {
        findViewById<View>(R.id.rootOverlay)
            .setOnApplyWindowInsetsListener { _, insets ->
                val imeHeight = insets.getInsets(WindowInsets.Type.ime()).bottom
                if (imeHeight != lastImeHeight) {
                    lastImeHeight = imeHeight
                    animateOverlay(imeHeight)
                }
                insets
            }
    }

    private fun animateOverlay(imeHeight: Int) {
        currentAnimator?.cancel()
        val from = overlayContainer?.translationY ?: 0f
        val extraLift = 28.dpToPx()
        val to = if (imeHeight > 0) -(imeHeight + extraLift).toFloat() else 0f
        currentAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = 250
            addUpdateListener {
                overlayContainer?.translationY = it.animatedValue as Float
            }
            start()
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // ==================== THEME ====================
    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val iconTint = if (isDark) Color.WHITE else Color.parseColor("#333333")

        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        miniInputField?.setTextColor(textColor)
        miniInputField?.setHintTextColor(hintColor)

        paperclipButton?.setColorFilter(iconTint)
        sendButton?.setColorFilter(iconTint)
        miniSendButton?.setColorFilter(iconTint)
        findViewById<ImageButton>(R.id.miniPaperclipButton)?.setColorFilter(iconTint)

        val bg = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        )
        overlayCard?.background = bg
        responseCard?.background = bg
        thinkingCard?.background = bg
        miniInputBar?.background = bg

        voiceContainer?.background = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        )
    }

    // ==================== NAVIGATION ====================
    private fun openMainApp(query: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(intent)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListeningHandler.removeCallbacksAndMessages(null)
        uiHandler.removeCallbacksAndMessages(null)
        currentAnimator?.cancel()
    }
}