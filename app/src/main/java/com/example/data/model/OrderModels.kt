package com.example.data.model

import android.graphics.Rect

enum class OrderDecision {
    ACCEPT,
    REJECT,
    MANUAL
}

data class ParsedOrder(
    val id: String = System.currentTimeMillis().toString(),
    val rawText: String,
    val restaurantDistanceKm: Double,
    val customerDistanceKm: Double,
    val totalDistanceKm: Double,
    val itemCount: Int,
    val deliveryFeeSar: Double?,
    val storeName: String? = null,
    val destinationName: String? = null,
    val acceptButtonBounds: Rect? = null,
    val rejectButtonBounds: Rect? = null,
    val packageName: String = "",
    val detectedAtTimestamp: Long = System.currentTimeMillis()
)

data class OrderEvaluationResult(
    val order: ParsedOrder,
    val decision: OrderDecision,
    val reasonEn: String,
    val reasonAr: String,
    val processingTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class DriverRules(
    val maxRestaurantDistanceKm: Double = 3.5,
    val maxCustomerDistanceKm: Double = 5.0,
    val maxQuantity: Int = 2,
    val minDeliveryFeeSar: Double = 12.0,
    val autoAcceptEnabled: Boolean = true,
    val autoRejectEnabled: Boolean = false,
    val burstTapEnabled: Boolean = true,
    val soundNotification: Boolean = true,
    val vibrateOnDecision: Boolean = true,
    val isMonitoringActive: Boolean = true
)

data class LicenseInfo(
    val code: String = "DEMO2026",
    val userName: String = "كابتن تجريبي (Demo)",
    val phoneNumber: String = "+966545135644",
    val isActive: Boolean = true,
    val expiresAt: Long = System.currentTimeMillis() + (7L * 24 * 3600 * 1000), // 7 days default
    val deviceId: String = "",
    val isDeviceLocked: Boolean = false,
    val isGracePeriod: Boolean = false
)

data class SessionStats(
    val totalEvaluated: Int = 0,
    val acceptedCount: Int = 0,
    val rejectedCount: Int = 0,
    val manualCount: Int = 0,
    val avgLatencyMs: Double = 1.6
)
