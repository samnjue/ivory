package com.ivory.ivory

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat

class AssistOverlayActivity : Activity() {
    private val TAG = "AssistOverlayActivity"
    private var rootView: View? = null
    private var overlayCard: View? = null
    private var micIcon: ImageView? = null
    private var sendButton: ImageButton? = null
    private var voiceContainer: View? = null
    private var isListening = false
    private var currentAnimator: ValueAnimator? = null
    private var lastInset = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "AssistOverlayActivity created")

        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window.setGravity(Gravity.BOTTOM)
        setContentView(R.layout.assist_overlay)
        rootView = findViewById(android.R.id.content)
        overlayCard = findViewById(R.id.overlayCard)

        micIcon = findViewById(R.id.micIcon)
        sendButton = findViewById(R.id.sendButton)
        voiceContainer = findViewById(R.id.voiceContainer)

        setupUi()
        setupImeInsetListener()
    }

    private fun setupUi() {
        val inputField = findViewById<EditText>(R.id.inputField)
        val paperclipButton = findViewById<ImageButton>(R.id.paperclipButton)

        paperclipButton.setOnClickListener {
            Log.d(TAG, "Paperclip tapped")
        }

        sendButton?.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                openMainApp(text)
                finishWithoutAnimation()
            }
        }

        voiceContainer?.setOnClickListener {
            Log.d(TAG, "Voice tapped")
            openMainApp(null)
            finishWithoutAnimation()
        }

        micIcon?.setOnClickListener {
            Log.d(TAG, "Mic tapped")
            toggleListening()
        }

        inputField.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        applyTheme()
    }

    private fun setupImeInsetListener() {
        rootView?.setOnApplyWindowInsetsListener { view, insets ->
            val imeVisible = insets.isVisible(WindowInsets.Type.ime())
            val imeHeight = insets.getInsets(WindowInsets.Type.ime()).bottom
            Log.d(TAG, "IME visible=$imeVisible height=$imeHeight")

            if (imeHeight != lastInset) {
                lastInset = imeHeight
                animateOverlay(imeHeight)
            }
            insets
        }
    }

    private fun animateOverlay(imeHeight: Int) {
        currentAnimator?.cancel()
        val from = (overlayCard?.translationY ?: 0f)
        val to = if (imeHeight > 0) -imeHeight.toFloat() else 0f
        currentAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = 250
            addUpdateListener {
                overlayCard?.translationY = it.animatedValue as Float
            }
            start()
        }
    }

    private fun applyTheme() {
        val isDark = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#333333")
        val hintColor = if (isDark) Color.parseColor("#88FFFFFF") else Color.parseColor("#88333333")
        val inputField = findViewById<EditText>(R.id.inputField)
        val paperclipBtn = findViewById<ImageButton>(R.id.paperclipButton)
        val sendBtn = findViewById<ImageButton>(R.id.sendButton)

        inputField.setTextColor(textColor)
        inputField.setHintTextColor(hintColor)

        val tint = if (isDark) Color.WHITE else Color.parseColor("#333333")
        paperclipBtn.setColorFilter(tint)
        sendBtn.setColorFilter(tint)
        micIcon?.setColorFilter(tint)
    }

    private fun toggleListening() {
        isListening = !isListening
        micIcon?.setColorFilter(
            if (isListening)
                ContextCompat.getColor(this, R.color.colorPrimary)
            else
                ContextCompat.getColor(this, android.R.color.white)
        )
    }

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
}
