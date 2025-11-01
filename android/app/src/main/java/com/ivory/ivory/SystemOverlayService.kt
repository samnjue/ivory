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
import com.facebook.react.modules.core.DeviceEventManagerModule

class SystemOverlayService : Service() {

    companion object {
        private const val NOTIF_ID = 42
        private const val CHANNEL_ID = "overlay_channel"

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
    private var overlayView: ReactRootView? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW" -> if (overlayView == null) createOverlay()
            "HIDE" -> removeOverlay()
        }
        return START_STICKY
    }

    private fun createOverlay() {
        // ---- ReactRootView that renders OverlayInputBar ----
        val reactInstanceManager: ReactInstanceManager = (application as MainApplication).reactNativeHost.reactInstanceManager
        val rootView = ReactRootView(this).apply {
            // "OverlayInputBar" must be the exact name you registered with registerRootComponent
            startReactApplication(reactInstanceManager, "OverlayInputBar", null)
        }

        // ---- WindowManager params (pill at bottom) ----
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

        // ---- Click-outside to dismiss ----
        rootView.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                hide(this@SystemOverlayService)
                true
            } else false
        }

        // ---- Bridge callbacks from JS ----
        rootView.setEventListener { event, params ->
            when (event) {
                "SEND" -> {
                    val text = params?.getString("text") ?: ""
                    AssistantModule(reactApplicationContext).sendTextToJS(text)
                    hide(this@SystemOverlayService)
                }
                "IVORY_STAR" -> {
                    AssistantModule(reactApplicationContext).openMainApp()
                    hide(this@SystemOverlayService)
                }
            }
        }

        wm?.addView(rootView, params)
        overlayView = rootView
    }

    private fun removeOverlay() {
        overlayView?.let {
            wm?.removeView(it)
            overlayView = null
        }
        stopSelf()
    }

    // ---------- Notification (required for foreground service) ----------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, "Overlay", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(chan)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
}