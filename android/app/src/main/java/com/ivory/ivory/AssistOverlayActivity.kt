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

class AssistOverlayActivity : Activity() {
    private val TAG = "AssistOverlayActivity"

    // Views
    private var overlayContainer: View? = null
    private var originalInputCard: FrameLayout? = null
    private var thinkingCard: FrameLayout? = null
    private var responseCard: FrameLayout? = null
    private var responseScrollView: ScrollView? = null
    private var miniInputContainer: FrameLayout? = null
    private var miniInputCard: FrameLayout? = null
    
    // Original input views
    private var inputField: EditText? = null
    private var paperclipButton: ImageButton? = null
    private var micContainer: View? = null
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var voiceContainer: View? = null
    private var sendButton: ImageButton? = null
    
    // Thinking views
    private var thinkingIvoryStar: ImageView? = null
    private var thinkingText: TextView? = null
    
    // Response views
    private var aiResponseText: TextView? = null
    private var aiResponseTitle: TextView? = null
    private var aiResponseIcon: ImageView? = null
    
    // Mini input views
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
    private var lastImeHeight = 0
    private val uiHandler = Handler(Looper.getMainLooper())
    private var thinkingDotsRunnable: Runnable? = null

    // Dummy response
    private val dummyResponse =
        "Einstein's field equations are the core of Einstein's general theory of relativity. They describe how matter and energy in the universe curve the fabric of spacetime. Essentially, they tell us that the curvature of spacetime is directly related to the energy and momentum of whatever is present. The equations are a set of ten interrelated differential equations..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        setContentView(R.layout.assist_overlay)

        // Bind views
        overlayContainer = findViewById(R.id.overlayContainer)
        originalInputCard = findViewById(R.id.originalInputCard)
        thinkingCard = findViewById(R.id.thinkingCard)
        responseCard = findViewById(R.id.responseCard)
        responseScrollView = findViewById(R.id.responseScrollView)
        miniInputContainer = findViewById(R.id.miniInputContainer)
        miniInputCard = findViewById(R.id.miniInputCard)
        
        // Original input
        inputField = findViewById(R.id.inputField)
        paperclipButton = findViewById(R.id.paperclipButton)
        micContainer = findViewById(R.id.micContainer)
        micIcon = findViewById(R.id.micIcon)
        micBlurLayer = findViewById(R.id.micBlurLayer)
        voiceContainer = findViewById(R.id.voiceContainer)
        sendButton = findViewById(R.id.sendButton)
        
        // Thinking
        thinkingIvoryStar = findViewById(R.id.thinkingIvoryStar)
        thinkingText = findViewById(R.id.thinkingText)
        
        // Response
        aiResponseText = findViewById(R.id.aiResponseText)
        aiResponseTitle = findViewById(R.id.aiResponseTitle)
        aiResponseIcon = findViewById(R.id.aiResponseIcon)
        
        // Mini input
        miniInputField = findViewById(R.id.miniInputField)
        miniPaperclipButton = findViewById(R.id.miniPaperclipButton)
        miniMicContainer = findViewById(R.id.miniMicContainer)
        miniMicIcon = findViewById(R.id.miniMicIcon)
        miniVoiceContainer = findViewById(R.id.miniVoiceContainer)
        miniSendButton = findViewById(R.id.miniSendButton)

        setupUi()
        setupImeInsetListener()
        applyTheme()
        applyGradientToTitle()
    }

    private fun setupUi() {
        // Outside tap dismisses
        findViewById<View>(R.id.rootOverlay).setOnClickListener { finishWithoutAnimation() }
        originalInputCard?.setOnClickListener { /* no-op */ }
        responseCard?.setOnClickListener { /* no-op */ }
        miniInputCard?.setOnClickListener { /* no-op */ }

        // Original input setup
        paperclipButton?.setOnClickListener { Log.d(TAG, "Paperclip tapped") }
        
        voiceContainer?.setOnClickListener {
            openMainApp(null)
            finishWithoutAnimation()
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

        // Mini input setup
        miniPaperclipButton?.setOnClickListener { Log.d(TAG, "Mini paperclip tapped") }
        
        miniVoiceContainer?.setOnClickListener {
            openMainApp(null)
            finishWithoutAnimation()
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

    private fun startThinkingPhase() {
        // Stop any existing animations
        thinkingDotsRunnable?.let { uiHandler.removeCallbacks(it) }
        
        // Fade out whatever is showing
        originalInputCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            originalInputCard?.visibility = View.GONE
            originalInputCard?.alpha = 1f
        }?.start()
        
        responseCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            responseCard?.visibility = View.GONE
            responseCard?.alpha = 1f
        }?.start()
        
        // Show thinking card
        uiHandler.postDelayed({
            thinkingCard?.visibility = View.VISIBLE
            thinkingCard?.alpha = 0f
            thinkingCard?.animate()?.alpha(1f)?.setDuration(200)?.start()
            
            startSpinningAnimation()
            animateThinkingDots()
            
            // After 5 seconds, show response
            uiHandler.postDelayed({ showResponsePhase() }, 5000)
        }, 200)
    }

    private fun showResponsePhase() {
        // Stop thinking animations
        thinkingDotsRunnable?.let { uiHandler.removeCallbacks(it) }
        thinkingIvoryStar?.clearAnimation()
        
        // Fade out thinking
        thinkingCard?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            thinkingCard?.visibility = View.GONE
            thinkingCard?.alpha = 1f
            
            // Show response card
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
                
                // Dynamically adjust scroll view height as content grows
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
        
        // Measure the actual content height
        aiResponseText?.measure(
            View.MeasureSpec.makeMeasureSpec(responseScrollView?.width ?: 0, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.UNSPECIFIED
        )
        
        val contentHeight = (aiResponseText?.measuredHeight ?: 0) + 
                           (16 * resources.displayMetrics.density * 2).toInt() + // padding
                           (miniInputContainer?.height ?: 0) + 
                           100 // extra space for title
        
        val targetHeight = Math.min(contentHeight, maxHeight)
        
        val layoutParams = responseScrollView?.layoutParams
        layoutParams?.height = targetHeight
        responseScrollView?.layoutParams = layoutParams
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
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        
        // Apply gradient to title text
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
            }
        }
        
        // Apply gradient to icon (for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            aiResponseIcon?.post {
                val width = aiResponseIcon?.width?.toFloat() ?: 0f
                if (width > 0) {
                    val gradient = LinearGradient(
                        0f, 0f, width, 0f,
                        Color.parseColor("#e63946"),
                        Color.parseColor("#4285f4"),
                        Shader.TileMode.CLAMP
                    )
                    aiResponseIcon?.setColorFilter(
                        android.graphics.BlendModeColorFilter(
                            Color.parseColor("#e63946"),
                            BlendMode.SRC_IN
                        )
                    )
                }
            }
        } else {
            // Fallback for older devices - use solid color
            aiResponseIcon?.setColorFilter(Color.parseColor("#e63946"))
        }
    }

    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val iconTint = if (isDark) Color.WHITE else Color.parseColor("#333333")

        // Text colors
        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)
        miniInputField?.setTextColor(textColor)
        miniInputField?.setHintTextColor(hintColor)
        thinkingText?.setTextColor(textColor)
        aiResponseText?.setTextColor(textColor)

        // Icon tints
        paperclipButton?.setColorFilter(iconTint)
        sendButton?.setColorFilter(iconTint)
        micIcon?.setColorFilter(iconTint)
        miniPaperclipButton?.setColorFilter(iconTint)
        miniSendButton?.setColorFilter(iconTint)
        miniMicIcon?.setColorFilter(iconTint)

        // Backgrounds
        val bg = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        )
        originalInputCard?.background = bg
        thinkingCard?.background = bg
        responseCard?.background = bg
        
        // Mini input background
        val miniBg = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.mini_input_background_dark else R.drawable.mini_input_background_light
        )
        miniInputCard?.background = miniBg

        // Gradient borders
        val gradientBorder = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        )
        voiceContainer?.background = gradientBorder
        miniVoiceContainer?.background = gradientBorder
        
        // Re-apply gradient to title after theme change
        applyGradientToTitle()
    }

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
        thinkingIvoryStar?.clearAnimation()
    }
}