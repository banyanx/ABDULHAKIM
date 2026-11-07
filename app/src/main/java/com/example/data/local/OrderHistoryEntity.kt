package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_history")
data class OrderHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String,
    val restaurantDistanceKm: Double,
    val customerDistanceKm: Double,
    val totalDistanceKm: Double,
    val itemCount: Int,
    val deliveryFeeSar: Double?,
    val decision: String,
    val reasonEn: String,
    val reasonAr: String,
    val processingTimeMs: Long,
    val timestamp: Long
)
