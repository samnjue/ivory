package com.ivory.ivory

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton

class SystemOverlayManager : Service() {
    private val TAG = "SystemOverlayManager"
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "overlay_channel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SystemOverlayManager service created")
        startForegroundService()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Assistant Overlay Channel",
                NotificationManager.IMPORTANCE_MIN // Minimize visibility
            ).apply {
                description = "Channel for assistant overlay service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Assistant Overlay")
                .setContentText("Running assistant overlay")
                .setSmallIcon(android.R.drawable.ic_menu_info_details) // Replace with your app's icon if available
                .setPriority(Notification.PRIORITY_MIN)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_OVERLAY) {
            showOverlay()
        } else if (intent?.action == ACTION_HIDE_OVERLAY) {
            hideOverlay()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showOverlay() {
        if (overlayView != null) {
            Log.d(TAG, "Overlay already visible")
            return
        }

        Log.d(TAG, "Showing system overlay")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayView = LayoutInflater.from(this).inflate(R.layout.assist_overlay, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM
        params.y = 20

        // Find views
        val inputField = overlayView?.findViewById<EditText>(R.id.inputField)
        val paperclipButton = overlayView?.findViewById<ImageButton>(R.id.paperclipButton)
        val voiceContainer = overlayView?.findViewById<View>(R.id.voiceContainer)
        val sendButton = overlayView?.findViewById<ImageButton>(R.id.sendButton)

        // Setup text watcher for input
        inputField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                voiceContainer?.visibility = if (hasText) View.GONE else View.VISIBLE
                sendButton?.visibility = if (hasText) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Paperclip button (attachment - placeholder functionality)
        paperclipButton?.setOnClickListener {
            Log.d(TAG, "Paperclip clicked - attachment functionality")
            // Implement attachment if needed
        }

        // Voice button click
        voiceContainer?.setOnClickListener {
            Log.d(TAG, "Voice button clicked")
            openMainApp(null)
            hideOverlay()
        }

        // Send button click
        sendButton?.setOnClickListener {
            val query = inputField?.text.toString().trim()
            if (query.isNotEmpty()) {
                Log.d(TAG, "Sending query: $query")
                openMainApp(query)
                hideOverlay()
            }
        }

        // Handle outside touches to close
        overlayView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideOverlay()
                true
            } else {
                false
            }
        }

        windowManager?.addView(overlayView, params)
        Log.d(TAG, "Overlay view added to window manager")
    }

    private fun hideOverlay() {
        Log.d(TAG, "Hiding overlay")
        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
        stopSelf()
    }

    private fun openMainApp(query: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("fromOverlay", true)
            if (query != null) {
                putExtra("query", query)
            }
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        Log.d(TAG, "Service destroyed")
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.ivory.ivory.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.ivory.ivory.HIDE_OVERLAY"

        fun show(context: Context) {
            val intent = Intent(context, SystemOverlayManager::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hide(context: Context) {
            val intent = Intent(context, SystemOverlayManager::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
        }
    }
}