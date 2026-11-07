package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.core.DecisionEngine
import com.example.core.OrderParser
import com.example.core.ServiceStateHolder
import com.example.core.TurboClickEngine
import com.example.data.model.OrderDecision

/**
 * Ultra-low latency Accessibility Service for automated delivery order detection and execution.
 * Configured with 0ms buffering to achieve immediate sub-3ms evaluation.
 */
class LocateAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LocateA11yService"

        // Packages to ignore immediately to save CPU cycles
        private val IGNORED_PACKAGES = hashSetOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.android.settings",
            "com.whatsapp",
            "com.facebook.katana",
            "com.instagram.android",
            "com.google.android.youtube",
            "com.google.android.apps.maps"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Locate Accessibility Service Connected")

        ServiceStateHolder.isAccessibilityActive = true

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 0 // Critical: 0ms buffering for instant event dispatch
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Check in-memory volatile rules and license in zero-overhead time
        val rules = ServiceStateHolder.rules
        if (!rules.isMonitoringActive) return

        val license = ServiceStateHolder.license
        if (!license.isActive || license.expiresAt <= System.currentTimeMillis()) {
            return
        }

        val pkgName = event.packageName?.toString() ?: ""
        if (pkgName.isNotBlank() && IGNORED_PACKAGES.contains(pkgName)) {
            return
        }

        val startTimeNs = System.nanoTime()

        try {
            val rootNode = rootInActiveWindow ?: return

            // 2. High-speed parse of the window hierarchy
            val parsedOrder = OrderParser.parseWindow(rootNode, pkgName) ?: return

            // 3. Sub-millisecond rule evaluation
            val result = DecisionEngine.evaluate(parsedOrder, rules, startTimeNs)

            // 4. Turbo click execution
            when (result.decision) {
                OrderDecision.ACCEPT -> {
                    parsedOrder.acceptButtonBounds?.let { bounds ->
                        TurboClickEngine.executeBurstClick(
                            service = this,
                            bounds = bounds,
                            node = null,
                            soundEnabled = rules.soundNotification,
                            vibrateEnabled = rules.vibrateOnDecision
                        )
                    }
                }
                OrderDecision.REJECT -> {
                    if (rules.autoRejectEnabled) {
                        parsedOrder.rejectButtonBounds?.let { bounds ->
                            TurboClickEngine.executeBurstClick(
                                service = this,
                                bounds = bounds,
                                node = null,
                                soundEnabled = rules.soundNotification,
                                vibrateEnabled = rules.vibrateOnDecision
                            )
                        }
                    }
                }
                OrderDecision.MANUAL -> {
                    // Manual review required
                }
            }

            // 5. Update atomic stats & log
            ServiceStateHolder.recordEvaluation(result)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing event", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Locate Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceStateHolder.isAccessibilityActive = false
        Log.i(TAG, "Locate Accessibility Service Destroyed")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        ServiceStateHolder.isAccessibilityActive = false
        return super.onUnbind(intent)
    }
}
