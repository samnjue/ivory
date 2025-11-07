package com.ivory.ivory

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*

class WaveView(context: Context) : View(context) {

    private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 400f, 400f,
            intArrayOf(Color.parseColor("#4285f4"), Color.parseColor("#6ea8fe")),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        alpha = (0.8f * 255).toInt()
    }

    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, 400f, 400f,
            intArrayOf(Color.parseColor("#EE3585"), Color.parseColor("#ff7b89")),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        alpha = (0.8f * 255).toInt()
    }

    private var phase = 0f
    private var amplitude = 0f
    private var running = false

    fun startAnimation() {
        amplitude = 0f
        phase = 0f
        running = true
        post(animationRunnable)
    }

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            phase += 0.08f
            amplitude = (amplitude + 0.04f).coerceAtMost(1f)
            invalidate()
            postDelayed(this, 16)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        running = false
        removeCallbacks(animationRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val A = 25f * amplitude
        val λ = w / 1.0f
        val baseY = lerp(250f, 80f, amplitude)

        fun drawWave(paint: Paint, offset: Float) {
            val path = Path()
            path.moveTo(0f, baseY)
            var x = 0f
            while (x <= w) {
                val y = baseY + A * sin((2f * PI.toFloat() * x) / λ + phase + offset)
                path.lineTo(x, y)
                x += 10f
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            canvas.drawPath(path, paint)
        }

        drawWave(paint1, 0f)
        drawWave(paint2, PI.toFloat() / 2f)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}