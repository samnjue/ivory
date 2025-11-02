package com.ivory.ivory

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import androidx.core.app.NotificationCompat
import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactRootView

class SystemOverlayService : Service() {

    companion object {
        private const val NOTIF_ID = 777
        private const val CHANNEL_ID = "ivory_overlay"

        fun show(ctx: Context) {
            // Check overlay permission before showing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
                android.util.Log.e("SystemOverlayService", "No overlay permission")
                return
            }
            val i = Intent(ctx, SystemOverlayService::class.java).apply { action = "SHOW" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun hide(ctx: Context) {
            ctx.stopService(Intent(ctx, SystemOverlayService::class.java))
        }
    }

    private var wm: WindowManager? = null
    private var rootView: ReactRootView? = null

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
        try {
            val reactInstanceManager = (application as MainApplication).reactNativeHost.reactInstanceManager
            val view = ReactRootView(this).apply {
                startReactApplication(reactInstanceManager, "main", null)
            }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 20
            }

            view.setOnTouchListener { _, ev ->
                if (ev.action == MotionEvent.ACTION_OUTSIDE) {
                    hide(this@SystemOverlayService)
                    true
                } else false
            }

            wm?.addView(view, params)
            rootView = view
        } catch (e: Exception) {
            android.util.Log.e("SystemOverlayService", "Error creating overlay", e)
            stopSelf()
        }
    }

    private fun removeOverlay() {
        try {
            rootView?.let { 
                wm?.removeView(it)
                rootView = null
            }
        } catch (e: Exception) {
            android.util.Log.e("SystemOverlayService", "Error removing overlay", e)
        } finally {
            stopSelf()
        }
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel(
                CHANNEL_ID, 
                "Ivory Assistant", 
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(c)
        }
    }

    private fun buildNotif() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Ivory Assistant")
        .setContentText("Running in background")
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setOngoing(true)
        .build()
}