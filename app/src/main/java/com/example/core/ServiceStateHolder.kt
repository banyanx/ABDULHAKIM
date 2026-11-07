package com.example.core

import android.content.Context
import android.graphics.Rect
import com.example.data.local.AppDatabase
import com.example.data.local.OrderHistoryEntity
import com.example.data.model.DriverRules
import com.example.data.model.LicenseInfo
import com.example.data.model.OrderDecision
import com.example.data.model.OrderEvaluationResult
import com.example.data.model.SessionStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance, zero-blocking in-memory state manager.
 * Keeps volatile driver rules, license flags, and atomic session statistics.
 * Guarantees 0ms disk I/O on the accessibility event thread.
 */
object ServiceStateHolder {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var database: AppDatabase? = null

    // Volatile state accessed by accessibility thread in under 1 microsecond
    @Volatile
    var rules: DriverRules = DriverRules()
        private set

    @Volatile
    var license: LicenseInfo = LicenseInfo()
        private set

    @Volatile
    var isAccessibilityActive: Boolean = false

    @Volatile
    var isOverlayActive: Boolean = false

    // Fast deduplication & throttle state
    private val lastActionTime = AtomicLong(0L)
    @Volatile
    private var lastActionCenterX: Int = -1
    @Volatile
    private var lastActionCenterY: Int = -1

    // Atomic session metrics
    private val totalEvaluatedCount = AtomicInteger(0)
    private val acceptedCount = AtomicInteger(0)
    private val rejectedCount = AtomicInteger(0)
    private val manualCount = AtomicInteger(0)
    private val totalLatencyNs = AtomicLong(0L)

    // Reactive StateFlows for UI and Overlay
    private val _rulesFlow = MutableStateFlow(rules)
    val rulesFlow: StateFlow<DriverRules> = _rulesFlow.asStateFlow()

    private val _licenseFlow = MutableStateFlow(license)
    val licenseFlow: StateFlow<LicenseInfo> = _licenseFlow.asStateFlow()

    private val _sessionStatsFlow = MutableStateFlow(SessionStats())
    val sessionStatsFlow: StateFlow<SessionStats> = _sessionStatsFlow.asStateFlow()

    private val _lastEvaluationFlow = MutableSharedFlow<OrderEvaluationResult>(extraBufferCapacity = 64)
    val lastEvaluationFlow: SharedFlow<OrderEvaluationResult> = _lastEvaluationFlow.asSharedFlow()

    fun init(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
            // Load saved settings if any in background
            loadPreferences(context)
        }
    }

    private fun loadPreferences(context: Context) {
        backgroundScope.launch {
            try {
                val prefs = context.getSharedPreferences("locate_prefs", Context.MODE_PRIVATE)
                val maxRest = prefs.getFloat("max_rest_dist", 3.5f).toDouble()
                val maxCust = prefs.getFloat("max_cust_dist", 5.0f).toDouble()
                val maxQty = prefs.getInt("max_qty", 2)
                val autoAccept = prefs.getBoolean("auto_accept", true)
                val autoReject = prefs.getBoolean("auto_reject", false)
                val burstTap = prefs.getBoolean("burst_tap", true)
                val sound = prefs.getBoolean("sound", true)
                val vibrate = prefs.getBoolean("vibrate", true)
                val monitoring = prefs.getBoolean("monitoring", true)

                rules = DriverRules(
                    maxRestaurantDistanceKm = maxRest,
                    maxCustomerDistanceKm = maxCust,
                    maxQuantity = maxQty,
                    autoAcceptEnabled = autoAccept,
                    autoRejectEnabled = autoReject,
                    burstTapEnabled = burstTap,
                    soundNotification = sound,
                    vibrateOnDecision = vibrate,
                    isMonitoringActive = monitoring
                )
                _rulesFlow.value = rules

                // Load license
                val code = prefs.getString("license_code", "DEMO2026") ?: "DEMO2026"
                val expiresAt = prefs.getLong("license_expires", System.currentTimeMillis() + 7L * 24 * 3600 * 1000)
                val user = prefs.getString("license_user", "كابتن تجريبي (Demo)") ?: "كابتن تجريبي (Demo)"
                val active = expiresAt > System.currentTimeMillis()

                license = LicenseInfo(
                    code = code,
                    userName = user,
                    isActive = active,
                    expiresAt = expiresAt
                )
                _licenseFlow.value = license
            } catch (e: Exception) {
                // Ignore load failures and use defaults
            }
        }
    }

    fun updateRules(context: Context, newRules: DriverRules) {
        rules = newRules
        _rulesFlow.value = newRules
        // Asynchronously persist without blocking caller
        backgroundScope.launch {
            try {
                context.getSharedPreferences("locate_prefs", Context.MODE_PRIVATE).edit().apply {
                    putFloat("max_rest_dist", newRules.maxRestaurantDistanceKm.toFloat())
                    putFloat("max_cust_dist", newRules.maxCustomerDistanceKm.toFloat())
                    putInt("max_qty", newRules.maxQuantity)
                    putBoolean("auto_accept", newRules.autoAcceptEnabled)
                    putBoolean("auto_reject", newRules.autoRejectEnabled)
                    putBoolean("burst_tap", newRules.burstTapEnabled)
                    putBoolean("sound", newRules.soundNotification)
                    putBoolean("vibrate", newRules.vibrateOnDecision)
                    putBoolean("monitoring", newRules.isMonitoringActive)
                    apply()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleMonitoring(context: Context): Boolean {
        val updated = rules.copy(isMonitoringActive = !rules.isMonitoringActive)
        updateRules(context, updated)
        return updated.isMonitoringActive
    }

    fun updateLicense(context: Context, newLicense: LicenseInfo) {
        license = newLicense
        _licenseFlow.value = newLicense
        backgroundScope.launch {
            try {
                context.getSharedPreferences("locate_prefs", Context.MODE_PRIVATE).edit().apply {
                    putString("license_code", newLicense.code)
                    putString("license_user", newLicense.userName)
                    putLong("license_expires", newLicense.expiresAt)
                    apply()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Checks if coordinates were tapped within cooldown (1500ms)
     */
    fun isThrottled(bounds: Rect): Boolean {
        val now = System.currentTimeMillis()
        val cx = bounds.centerX()
        val cy = bounds.centerY()

        val lastTime = lastActionTime.get()
        if (now - lastTime < 1500L) {
            // Check if within 50px delta of last tap
            val dx = kotlin.math.abs(cx - lastActionCenterX)
            val dy = kotlin.math.abs(cy - lastActionCenterY)
            if (dx < 50 && dy < 50) {
                return true
            }
        }
        return false
    }

    fun markActionDispatched(bounds: Rect) {
        lastActionTime.set(System.currentTimeMillis())
        lastActionCenterX = bounds.centerX()
        lastActionCenterY = bounds.centerY()
    }

    /**
     * Records evaluation result into atomic stats and queues async persistence to Room
     */
    fun recordEvaluation(result: OrderEvaluationResult) {
        val total = totalEvaluatedCount.incrementAndGet()
        when (result.decision) {
            OrderDecision.ACCEPT -> acceptedCount.incrementAndGet()
            OrderDecision.REJECT -> rejectedCount.incrementAndGet()
            OrderDecision.MANUAL -> manualCount.incrementAndGet()
        }
        totalLatencyNs.addAndGet(result.processingTimeMs)

        val avg = if (total > 0) totalLatencyNs.get().toDouble() / total else 1.5

        _sessionStatsFlow.value = SessionStats(
            totalEvaluated = total,
            acceptedCount = acceptedCount.get(),
            rejectedCount = rejectedCount.get(),
            manualCount = manualCount.get(),
            avgLatencyMs = String.format(java.util.Locale.US, "%.1f", avg).toDoubleOrNull() ?: 1.5
        )

        _lastEvaluationFlow.tryEmit(result)

        // Asynchronously persist to Room without delaying the main flow
        backgroundScope.launch {
            try {
                val entity = OrderHistoryEntity(
                    orderId = result.order.id,
                    restaurantDistanceKm = result.order.restaurantDistanceKm,
                    customerDistanceKm = result.order.customerDistanceKm,
                    totalDistanceKm = result.order.totalDistanceKm,
                    itemCount = result.order.itemCount,
                    deliveryFeeSar = result.order.deliveryFeeSar,
                    decision = result.decision.name,
                    reasonEn = result.reasonEn,
                    reasonAr = result.reasonAr,
                    processingTimeMs = result.processingTimeMs,
                    timestamp = result.timestamp
                )
                database?.orderHistoryDao()?.insertOrder(entity)
            } catch (e: Exception) {
                // Ignore DB insertion error
            }
        }
    }

    fun clearStats() {
        totalEvaluatedCount.set(0)
        acceptedCount.set(0)
        rejectedCount.set(0)
        manualCount.set(0)
        totalLatencyNs.set(0L)
        _sessionStatsFlow.value = SessionStats()
    }
}
