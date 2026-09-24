package com.residentsleeper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baby_profile")
data class BabyProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "Baby",
    val birthTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val customWakeWindowMinutes: Int? = null, // null means use automatic age-based
    val feedingIntervalMinutes: Int = 150,    // Default 2.5 hours
    val selectedCalendarId: Long? = null,     // Synced Google Calendar ID
    val notifyBeforeMinutes: Int = 10,        // Push notifications X minutes before event
    val enableCalendarSync: Boolean = false,
    val enablePushNotifications: Boolean = true,
    val dayStartHour: Int = 7,                // Hour of starting the day (0..23, default 7:00 AM)
    val maxRecommendedNightFeeds: Int = 1,    // 1 feed recommended at night; subsequent feeds optional
    val notifyForOptionalNightFeeds: Boolean = false, // Whether to alert for optional night feeds
    val gender: Gender = Gender.UNSPECIFIED,
    val use12HourFormat: Boolean = false
)
