package com.ivory.ivory

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private var amplitude = 0f
    private var phase = 0f
    private var isListening = false

    fun setListening(listening: Boolean) {
        isListening = listening
        amplitude = if (listening) 25f else 0f
        invalidate()
        if (listening) {
            post(animateRunnable)
        } else {
            removeCallbacks(animateRunnable)
        }
    }

    private val animateRunnable = object : Runnable {
        override fun run() {
            phase += 0.1f
            invalidate()
            postDelayed(this, 16) // ~60fps
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isListening) return

        // Gradient similar to ChatScreen
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.parseColor("#4285f4"), Color.parseColor("#6ea8fe")),
            null,
            Shader.TileMode.CLAMP
        )

        val path = Path()
        val baseY = height * 0.5f
        val wavelength = width / 1.0f

        path.moveTo(0f, baseY)
        for (x in 0..width step 10) {
            val y = baseY + amplitude * sin((2 * Math.PI * x / wavelength + phase).toFloat())
            path.lineTo(x.toFloat(), y)
        }
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

        canvas.drawPath(path, paint)
    }
}