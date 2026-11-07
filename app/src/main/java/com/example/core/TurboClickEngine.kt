package com.example.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Turbo Click Engine implementing Dual Execution (Physical Hardware Touch + Programmatic Fallback)
 * with 3x Burst Micro-Tapping at 0ms, +35ms, and +75ms to bypass animation delays in modern UI frameworks.
 */
object TurboClickEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            // Ignore tone initialization failure
        }
    }

    /**
     * Executes the ultra-fast burst tap sequence on target button coordinates.
     */
    fun executeBurstClick(
        service: AccessibilityService,
        bounds: Rect,
        node: AccessibilityNodeInfo? = null,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true
    ) {
        if (bounds.isEmpty) return

        // 1. Check cooldown deduplication
        if (ServiceStateHolder.isThrottled(bounds)) {
            return
        }
        ServiceStateHolder.markActionDispatched(bounds)

        val cx = bounds.centerX().toFloat()
        val cy = bounds.centerY().toFloat()

        // 2. Hardware Gesture Tap #1 (0ms)
        dispatchHardwareTap(service, cx, cy)

        // 3. Fallback programmatic accessibility click (simultaneous)
        try {
            node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            // Safe fallback
        }

        // 4. Burst Taps (+35ms and +75ms) to penetrate fade-in / slide-in animations
        mainHandler.postDelayed({
            dispatchHardwareTap(service, cx, cy)
        }, 35)

        mainHandler.postDelayed({
            dispatchHardwareTap(service, cx, cy)
        }, 75)

        // 5. Driver feedback
        if (vibrateEnabled) {
            triggerHapticFeedback(service)
        }
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Dispatches a single ultra-short 5ms hardware touch event at (x, y)
     */
    private fun dispatchHardwareTap(service: AccessibilityService, x: Float, y: Float) {
        if (x <= 0f || y <= 0f) return
        try {
            val clickPath = Path().apply {
                moveTo(x, y)
                lineTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(clickPath, 0L, 5L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            service.dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            // Gesture dispatch failed on restricted window or screen off
        }
    }

    private fun triggerHapticFeedback(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(50)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }
}
