package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.ServiceStateHolder
import com.example.data.model.OrderDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Floating Action Overlay Service operating via WindowManager.
 * Displays real-time driver co-pilot status, instant drag physics, and auto-dismiss on license expiry.
 */
class OverlayService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "locate_overlay_channel"
        const val ACTION_START = "com.example.locate.ACTION_START"
        const val ACTION_STOP = "com.example.locate.ACTION_STOP"
        const val ACTION_TOGGLE = "com.example.locate.ACTION_TOGGLE"

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var expirationRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ServiceStateHolder.init(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> {
                val isActive = ServiceStateHolder.toggleMonitoring(applicationContext)
                updateOverlayVisuals(isActive)
                updateNotification(isActive)
            }
            else -> {
                val notification = createNotification(ServiceStateHolder.rules.isMonitoringActive)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                    } else {
                        startForeground(NOTIFICATION_ID, notification, 0)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                if (Settings.canDrawOverlays(this)) {
                    showOverlayWidget()
                    scheduleAutoDismissOnExpiration()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlayWidget() {
        if (overlayView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 280
        }
        windowLayoutParams = params

        // Create high performance floating layout
        val container = FrameLayout(this).apply {
            setPadding(12, 12, 12, 12)
        }

        // Inner circular card
        val card = FrameLayout(this).apply {
            val size = (64 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setColorFilter(Color.parseColor("#E5B84B"))
            val iconSize = (28 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
        }

        val statusBadge = TextView(this).apply {
            text = "⚡"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val badgeSize = (20 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#38EF7D"))
            }
        }

        card.addView(icon)
        card.addView(statusBadge)
        container.addView(card)

        // Dragging & Click touch physics
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = windowLayoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                            isClick = false
                        }
                        currentParams.x = initialX + dx
                        currentParams.y = initialY + dy
                        windowManager?.updateViewLayout(container, currentParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            val active = ServiceStateHolder.toggleMonitoring(applicationContext)
                            updateOverlayVisuals(active)
                            updateNotification(active)
                        }
                        return true
                    }
                }
                return false
            }
        })

        overlayView = container
        windowManager?.addView(container, params)
        ServiceStateHolder.isOverlayActive = true

        updateOverlayVisuals(ServiceStateHolder.rules.isMonitoringActive)

        // Observe evaluated orders for flash animations
        serviceScope.launch {
            ServiceStateHolder.lastEvaluationFlow.collectLatest { eval ->
                flashStatusOnEvent(eval.decision)
            }
        }
    }

    private fun updateOverlayVisuals(isActive: Boolean) {
        val root = overlayView as? FrameLayout ?: return
        val card = root.getChildAt(0) as? FrameLayout ?: return
        val icon = card.getChildAt(0) as? ImageView ?: return
        val badge = card.getChildAt(1) as? TextView ?: return

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#161E2E"))
            setStroke(
                (2.5 * resources.displayMetrics.density).toInt(),
                if (isActive) Color.parseColor("#38EF7D") else Color.parseColor("#64748B")
            )
        }
        card.background = bg

        if (isActive) {
            icon.setImageResource(android.R.drawable.ic_media_play)
            icon.setColorFilter(Color.parseColor("#38EF7D"))
            badge.text = "⚡"
            (badge.background as? GradientDrawable)?.setColor(Color.parseColor("#38EF7D"))
        } else {
            icon.setImageResource(android.R.drawable.ic_media_pause)
            icon.setColorFilter(Color.parseColor("#94A3B8"))
            badge.text = "💤"
            (badge.background as? GradientDrawable)?.setColor(Color.parseColor("#64748B"))
        }
    }

    private fun flashStatusOnEvent(decision: OrderDecision) {
        val root = overlayView as? FrameLayout ?: return
        val card = root.getChildAt(0) as? FrameLayout ?: return

        val color = when (decision) {
            OrderDecision.ACCEPT -> Color.parseColor("#38EF7D")
            OrderDecision.REJECT -> Color.parseColor("#FF4B4B")
            OrderDecision.MANUAL -> Color.parseColor("#E5B84B")
        }

        val flashBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke((3 * resources.displayMetrics.density).toInt(), Color.WHITE)
        }
        card.background = flashBg

        mainHandler.postDelayed({
            updateOverlayVisuals(ServiceStateHolder.rules.isMonitoringActive)
        }, 600)
    }

    /**
     * Auto-dismiss on trial/subscription expiration using zero-overhead Handler.postDelayed
     */
    private fun scheduleAutoDismissOnExpiration() {
        expirationRunnable?.let { mainHandler.removeCallbacks(it) }

        val license = ServiceStateHolder.license
        val remainingMs = license.expiresAt - System.currentTimeMillis()

        if (remainingMs <= 0) {
            stopSelf()
            return
        }

        val runnable = Runnable {
            stopSelf()
        }
        expirationRunnable = runnable
        mainHandler.postDelayed(runnable, remainingMs)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Locate Assistant Co-pilot",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating widget status and quick driver toggles"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(isActive: Boolean): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingToggle = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = if (isActive) "Active - Monitoring Delivery Orders" else "Paused - Tap to Resume"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Locate Assistant Co-Pilot")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingAppIntent)
            .setOngoing(true)
            .addAction(
                if (isActive) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isActive) "Pause" else "Start",
                pendingToggle
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Widget", pendingStop)
            .build()
    }

    private fun updateNotification(isActive: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(isActive))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        expirationRunnable?.let { mainHandler.removeCallbacks(it) }

        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
        ServiceStateHolder.isOverlayActive = false
    }
}
