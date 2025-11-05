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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat

class AssistOverlayActivity : Activity() {

    private val TAG = "AssistOverlayActivity"

    // Views
    private var overlayContainer: View? = null
    private var overlayCard: View? = null
    private var inputField: EditText? = null
    private var paperclipButton: ImageButton? = null
    private var micContainer: View? = null
    private var micIcon: ImageView? = null
    private var micBlurLayer: ImageView? = null
    private var voiceContainer: View? = null
    private var sendButton: ImageButton? = null

    // State
    private var isListening = false
    private val stopListeningHandler = Handler(Looper.getMainLooper())
    private var currentAnimator: ValueAnimator? = null
    private var lastImeHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        // Transparent activity, no flicker
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        setContentView(R.layout.assist_overlay)

        // Outside taps dismisses the overlay
        findViewById<View>(R.id.rootOverlay).setOnClickListener { finishWithoutAnimation() }
        findViewById<View>(R.id.overlayCard).setOnClickListener { /* no-op */ }

        // Bind views
        overlayContainer = findViewById(R.id.overlayContainer)
        overlayCard = findViewById(R.id.overlayCard)
        inputField = findViewById(R.id.inputField)
        paperclipButton = findViewById(R.id.paperclipButton)
        micContainer = findViewById(R.id.micContainer)
        micIcon = findViewById(R.id.micIcon)
        micBlurLayer = findViewById(R.id.micBlurLayer)
        voiceContainer = findViewById(R.id.voiceContainer)
        sendButton = findViewById(R.id.sendButton)

        setupUi()
        setupImeInsetListener()
        applyTheme()
    }

    // UI SETUP
    private fun setupUi() {
        // Paper-clip
        paperclipButton?.setOnClickListener { Log.d(TAG, "Paperclip tapped") }

        // Voice (ivory star)
        voiceContainer?.setOnClickListener {
            openMainApp(null)
            finishWithoutAnimation()
        }

        // Send button
        sendButton?.setOnClickListener {
            val text = inputField?.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                openMainApp(text)
                finishWithoutAnimation()
            }
        }

        // Mic
        micContainer?.setOnClickListener {
            if (!isListening) {
                startListeningAnimation()
                stopListeningHandler.postDelayed({ stopListeningAnimation() }, 5000)
            } else {
                stopListeningAnimation()
            }
        }

        // EditText – show keyboard on focus + switch icons
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

        // Outside-tap dismisses the overlay
        findViewById<View>(R.id.rootOverlay).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                finishWithoutAnimation()
                true
            } else false
        }
    }

    // KEYBOARD INSET HANDLING
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

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
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

    // MIC LISTENING ANIMATIONS
    private fun startListeningAnimation() {
        isListening = true

        // Top mic: gradient + pulse
        micIcon?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            clearColorFilter()
            startAnimation(AnimationUtils.loadAnimation(this@AssistOverlayActivity, R.anim.mic_pulse))
        }

        // Bottom mic: blurred glow + pulse
        micBlurLayer?.apply {
            setImageResource(R.drawable.ic_mic_gradient)
            visibility = View.VISIBLE
            alpha = 0.7f

            // Android 12+ blur
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
            applyTheme() // re-tint white
        }, 100)
    }

    // THEME HANDLING
    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val iconTint = if (isDark) Color.WHITE else Color.parseColor("#333333")

        inputField?.setTextColor(textColor)
        inputField?.setHintTextColor(hintColor)

        paperclipButton?.setColorFilter(iconTint)
        sendButton?.setColorFilter(iconTint)
        micIcon?.setColorFilter(iconTint)

        overlayCard?.background = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.overlay_background_dark else R.drawable.overlay_background_light
        )
        voiceContainer?.background = ContextCompat.getDrawable(
            this,
            if (isDark) R.drawable.gradient_border_dark else R.drawable.gradient_border_light
        )
    }

    // NAVIGATION
    private fun openMainApp(query: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            query?.let { putExtra("query", it) }
        }
        startActivity(intent)
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
        currentAnimator?.cancel()
    }
}