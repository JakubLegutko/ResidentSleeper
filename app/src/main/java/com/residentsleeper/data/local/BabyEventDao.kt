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
    @Query("SELECT * FROM baby_events ORDER BY startTime DESC")
    fun getAllEvents(): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE (startTime <= :endOfDay AND (endTime IS NULL OR endTime >= :startOfDay)) ORDER BY startTime ASC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE startTime >= :fromTimestamp AND startTime <= :toTimestamp ORDER BY startTime ASC")
    fun getEventsInRange(fromTimestamp: Long, toTimestamp: Long): Flow<List<BabyEvent>>

    @Query("SELECT * FROM baby_events WHERE startTime >= :fromTimestamp AND startTime <= :toTimestamp ORDER BY startTime ASC")
    suspend fun getEventsInRangeSync(fromTimestamp: Long, toTimestamp: Long): List<BabyEvent>

    @Query("SELECT * FROM baby_events WHERE type = :type ORDER BY startTime DESC LIMIT 1")
    fun getLatestEventFlow(type: EventType): Flow<BabyEvent?>

    @Query("SELECT * FROM baby_events WHERE type = :type ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestEvent(type: EventType): BabyEvent?

    @Query("SELECT * FROM baby_events WHERE type = :type AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun getOngoingEventFlow(type: EventType): Flow<BabyEvent?>

    @Query("SELECT * FROM baby_events WHERE type = :type AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOngoingEvent(type: EventType): BabyEvent?

    @Query("SELECT * FROM baby_events WHERE id = :id")
    suspend fun getEventById(id: Long): BabyEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BabyEvent): Long

    @Update
    suspend fun update(event: BabyEvent)

    @Delete
    suspend fun delete(event: BabyEvent)

    @Query("DELETE FROM baby_events")
    suspend fun deleteAll()
}
