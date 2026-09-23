package com.residentsleeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.EventType
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyEventDao {
    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId ORDER BY startTime DESC")
    fun getAllEventsForProfile(profileId: Long): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId AND (startTime <= :endOfDay AND (endTime IS NULL OR endTime >= :startOfDay)) ORDER BY startTime ASC")
    fun getEventsForDay(profileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId AND startTime <= :toTimestamp AND (endTime IS NULL OR endTime >= :fromTimestamp) ORDER BY startTime ASC")
    fun getEventsInRange(profileId: Long, fromTimestamp: Long, toTimestamp: Long): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId AND startTime <= :toTimestamp AND (endTime IS NULL OR endTime >= :fromTimestamp) ORDER BY startTime ASC")
    suspend fun getEventsInRangeSync(profileId: Long, fromTimestamp: Long, toTimestamp: Long): List<BabyEvent>

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId AND type = :type ORDER BY startTime DESC LIMIT 1")
    fun getLatestEventFlow(profileId: Long, type: EventType): Flow<BabyEvent?>

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId AND type = :type ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestEvent(profileId: Long, type: EventType): BabyEvent?

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId AND type = :type AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun getOngoingEventFlow(profileId: Long, type: EventType): Flow<BabyEvent?>

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId AND type = :type AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOngoingEvent(profileId: Long, type: EventType): BabyEvent?

    @Query("SELECT * FROM baby_events WHERE id = :id")
    suspend fun getEventById(id: Long): BabyEvent?

    // Global queries for export & import
    @Query("SELECT * FROM baby_events ORDER BY startTime ASC")
    suspend fun getAllEvents(): List<BabyEvent>

    @Query("SELECT * FROM baby_events WHERE babyProfileId = :profileId ORDER BY startTime ASC")
    suspend fun getAllEventsForProfileSync(profileId: Long): List<BabyEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BabyEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<BabyEvent>)

    @Update
    suspend fun update(event: BabyEvent)

    @Delete
    suspend fun delete(event: BabyEvent)

    @Query("DELETE FROM baby_events WHERE babyProfileId = :profileId")
    suspend fun deleteEventsForProfile(profileId: Long)

    @Query("DELETE FROM baby_events")
    suspend fun deleteAll()
}
