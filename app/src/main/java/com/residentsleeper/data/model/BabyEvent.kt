package com.residentsleeper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "baby_events",
    indices = [
        Index(value = ["babyProfileId", "startTime"]),
        Index(value = ["babyProfileId", "type"])
    ]
)
data class BabyEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val babyProfileId: Long = 1,    // Associated child profile ID
    val type: EventType,
    val startTime: Long,            // Epoch milliseconds
    val endTime: Long? = null,      // Null if event is ongoing or instantaneous
    val nursingType: NursingType? = null,
    val diaperType: DiaperType? = null,
    val amountMl: Int? = null,      // For bottle feeding
    val note: String? = null,
    val calendarEventId: Long? = null // Sync ID with Google Calendar if synced
)
