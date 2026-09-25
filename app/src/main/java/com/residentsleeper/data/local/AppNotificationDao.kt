package com.residentsleeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.residentsleeper.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {

    @Query("SELECT * FROM notifications WHERE timestamp >= :cutoffTimestamp ORDER BY timestamp DESC")
    fun getNotificationsSince(cutoffTimestamp: Long): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE timestamp >= :cutoffTimestamp AND isRead = 0")
    fun getUnreadCountSince(cutoffTimestamp: Long): Flow<Int>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAllNotificationsSync(): List<AppNotification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotification>)

    @Update
    suspend fun update(notification: AppNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
