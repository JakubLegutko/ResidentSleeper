package com.residentsleeper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baby_profile")
data class BabyProfile(
    @PrimaryKey
    val id: Int = 1,
    val birthTimestamp: Long = System.currentTimeMillis(),
    val customWakeWindowMinutes: Int? = null, // null means use automatic age-based
    val feedingIntervalMinutes: Int = 150,    // Default 2.5 hours
    val selectedCalendarId: Long? = null,     // Synced Google Calendar ID
    val notifyBeforeMinutes: Int = 10,        // Push notifications X minutes before event
    val enableCalendarSync: Boolean = false,
    val enablePushNotifications: Boolean = true
)
