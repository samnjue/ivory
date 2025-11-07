package com.ivory.ivory

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.animation.DecelerateInterpolator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import com.ivory.ivory.R
import com.ivory.ivory.WaveView

class IvoryOverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.ivory.ivory.SHOW_ORB"
        const val ACTION_HIDE = "com.ivory.ivory.HIDE_ORB"

        fun start(context: android.content.Context) {
            val i = Intent(context, IvoryOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: android.content.Context) {
            val i = Intent(context, IvoryOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(i)
        }
    }

    private var wm: WindowManager? = null
    private var orb: ImageView? = null
    private var wave: WaveView? = null
    private var orbWrapper: FrameLayout? = null  
    private var inputCard: FrameLayout? = null
    private var removeZone: View? = null

    // drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // idle shrink
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable { shrinkOrb() }

    // params
    private lateinit var orbParams: WindowManager.LayoutParams
    private var screenW = 0
    private var screenH = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            wm!!.defaultDisplay
        }
        val size = android.graphics.Point()
        display?.getRealSize(size)
        screenW = size.x
        screenH = size.y

        createOverlay()
        startForeground(1, android.app.Notification()) 
    }

    private fun createOverlay() {
        // ----- ROOT container (covers whole screen) -----
        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // ----- REMOVE ZONE (bottom) -----
        removeZone = View(this).apply {
            isVisible = false
            setBackgroundResource(R.drawable.remove_zone_bg) // red strip
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(120)
            ).apply { gravity = Gravity.BOTTOM }
            root.addView(this)
        }

        // ----- ORB: wrapper → wave → border → star -----
        val orbSize = dp(56)

        orbWrapper = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize)
            clipToOutline = true
            elevation = 12f
        }

        // Wave
        wave = WaveView(this@IvoryOverlayService).apply {
            isVisible = false
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize)
        }
        orbWrapper!!.addView(wave)

        // Gradient border
        val border = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize)
            background = ContextCompat.getDrawable(this@IvoryOverlayService, R.drawable.gradient_border_light)
            clipToOutline = true
        }
        orbWrapper!!.addView(border)

        // Star (on top)
        val star = ImageView(this).apply {
            setImageResource(R.drawable.ivorystar)
            layoutParams = FrameLayout.LayoutParams(dp(32), dp(32)).apply {
                gravity = Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        orbWrapper!!.addView(star)
        orb = star

        // Window params
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

        // Touch on wrapper
        orbWrapper!!.setOnTouchListener { v, ev -> ... }

        // ----- INPUT CARD  -----
        val inflater = LayoutInflater.from(this)
        inputCard = inflater.inflate(R.layout.assist_overlay, root, false) as FrameLayout
        // hide everything except the originalInputCard
        inputCard?.findViewById<FrameLayout>(R.id.originalInputCard)?.isVisible = true
        inputCard?.findViewById<FrameLayout>(R.id.responseCard)?.isVisible = false
        inputCard?.findViewById<FrameLayout>(R.id.thinkingCard)?.isVisible = false
        inputCard?.isVisible = false
        root.addView(inputCard)

        // ----- Add everything to WindowManager -----
        // Root covers full screen (for inputCard + removeZone)
        wm!!.addView(root, WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            orbParams.type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ))

        // Orb is added ONCE — directly
        wm!!.addView(orbWrapper, orbParams)

        // ----- Touch handling (on orbWrapper) -----
        orbWrapper!!.setOnTouchListener { v, ev ->
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

                    // Update the orbWrapper directly
                    wm!!.updateViewLayout(orbWrapper, orbParams)

                    // Show/hide remove zone
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
    }

    private fun onOrbTap() {
        // Show wave *inside* the orb
        wave?.isVisible = true
        wave?.startAnimation()

        // Animate input card out from orb center
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
            ?.start()
    }

    private fun snapOrbToEdge() {
        val targetX = if (orbParams.x > screenW / 2) screenW - dp(72) else dp(16)

        val xHolder = PropertyValuesHolder.ofInt("x", orbParams.x, targetX)

        val animX = ObjectAnimator.ofPropertyValuesHolder(orbParams, xHolder).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                orbParams.x = it.getAnimatedValue("x") as Int
                wm!!.updateViewLayout(orbWrapper, orbParams)
            }
        }
        animX.start()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                // already created in onCreate()
            }
            ACTION_HIDE -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        idleHandler.removeCallbacksAndMessages(null)
        wm?.let {
            orbWrapper?.let { v -> it.removeView(v) }
            // root is removed automatically
        }
        super.onDestroy()
    }

    private fun dp(dp: Int) = (resources.displayMetrics.density * dp).toInt()
}