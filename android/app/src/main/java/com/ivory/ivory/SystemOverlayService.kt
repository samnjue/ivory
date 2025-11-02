package com.ivory.ivory

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView

class SystemOverlayService : Service() {

    companion object {
        private const val NOTIF_ID = 777
        private const val CHANNEL_ID = "ivory_overlay"

        fun show(ctx: Context) {
            val i = Intent(ctx, SystemOverlayService::class.java).apply { action = "SHOW" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun hide(ctx: Context) {
            val i = Intent(ctx, SystemOverlayService::class.java).apply { action = "HIDE" }
            ctx.startService(i)
        }
    }

    private var wm: WindowManager? = null
    private var rootView: ReactRootView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW" -> if (rootView == null) createOverlay()
            "HIDE" -> removeOverlay()
        }
        return START_STICKY
    }

    private fun createOverlay() {
        val reactInstanceManager = (application as MainApplication).reactNativeHost.reactInstanceManager
        val view = ReactRootView(this).apply {
            // Must match the registered entry file
            startReactApplication(reactInstanceManager, "OverlayInputBar", null)
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 20
        }

        // Tap-outside dismiss
        view.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                hide(this@SystemOverlayService)
                true
            } else false
        }

        wm?.addView(view, params)
        rootView = view
    }

    private fun removeOverlay() {
        rootView?.let { wm?.removeView(it) }
        rootView = null
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel(CHANNEL_ID, "Ivory Assistant", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
    }

    private fun buildNotif() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("")
        .setContentText("")
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setPriority(NotificationCompat.PRIMARY_MIN)
        .build()
}