package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderHistoryDao {
    @Query("SELECT * FROM order_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllOrders(): Flow<List<OrderHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderHistoryEntity): Long

    @Query("DELETE FROM order_history")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM order_history")
    suspend fun getOrderCount(): Int
}
