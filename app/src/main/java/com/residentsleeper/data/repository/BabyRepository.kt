package com.residentsleeper.data.repository

import com.residentsleeper.data.local.BabyEventDao
import com.residentsleeper.data.local.BabyProfileDao
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BabyRepository(
    private val eventDao: BabyEventDao,
    private val profileDao: BabyProfileDao
) {
    val profileFlow: Flow<BabyProfile?> = profileDao.getProfileFlow()

    suspend fun getProfile(): BabyProfile {
        return profileDao.getProfile() ?: BabyProfile().also {
            profileDao.insertOrUpdate(it)
        }
    }

    suspend fun updateProfile(profile: BabyProfile) {
        profileDao.insertOrUpdate(profile)
    }

    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<BabyEvent>> {
        return eventDao.getEventsForDay(startOfDay, endOfDay)
    }

    fun getEventsInRange(from: Long, to: Long): Flow<List<BabyEvent>> {
        return eventDao.getEventsInRange(from, to)
    }

    suspend fun getEventsInRangeSync(from: Long, to: Long): List<BabyEvent> {
        return eventDao.getEventsInRangeSync(from, to)
    }

    fun getOngoingEventFlow(type: EventType): Flow<BabyEvent?> {
        return eventDao.getOngoingEventFlow(type)
    }

    suspend fun getOngoingEvent(type: EventType): BabyEvent? {
        return eventDao.getOngoingEvent(type)
    }

    fun getLatestEventFlow(type: EventType): Flow<BabyEvent?> {
        return eventDao.getLatestEventFlow(type)
    }

    suspend fun getLatestEvent(type: EventType): BabyEvent? {
        return eventDao.getLatestEvent(type)
    }

    suspend fun insertEvent(event: BabyEvent): Long {
        return eventDao.insert(event)
    }

    suspend fun updateEvent(event: BabyEvent) {
        eventDao.update(event)
    }

    suspend fun deleteEvent(event: BabyEvent) {
        eventDao.delete(event)
    }

    // --- High-level actions ---

    suspend fun startSleep(startTime: Long = System.currentTimeMillis()): Long {
        // End any existing ongoing sleep if present
        endOngoingSleep(startTime)
        val event = BabyEvent(
            type = EventType.SLEEP,
            startTime = startTime,
            endTime = null
        )
        return eventDao.insert(event)
    }

    suspend fun endOngoingSleep(endTime: Long = System.currentTimeMillis()) {
        val ongoing = eventDao.getOngoingEvent(EventType.SLEEP)
        if (ongoing != null) {
            val adjustedEnd = if (endTime < ongoing.startTime) ongoing.startTime else endTime
            eventDao.update(ongoing.copy(endTime = adjustedEnd))
        }
    }

    suspend fun logCompletedSleep(startTime: Long, endTime: Long): Long {
        val event = BabyEvent(
            type = EventType.SLEEP,
            startTime = startTime,
            endTime = maxOf(startTime, endTime)
        )
        return eventDao.insert(event)
    }

    suspend fun startNursing(
        nursingType: NursingType,
        amountMl: Int? = null,
        startTime: Long = System.currentTimeMillis()
    ): Long {
        endOngoingNursing(startTime)
        val event = BabyEvent(
            type = EventType.NURSING,
            startTime = startTime,
            endTime = null,
            nursingType = nursingType,
            amountMl = amountMl
        )
        return eventDao.insert(event)
    }

    suspend fun endOngoingNursing(endTime: Long = System.currentTimeMillis()) {
        val ongoing = eventDao.getOngoingEvent(EventType.NURSING)
        if (ongoing != null) {
            val adjustedEnd = if (endTime < ongoing.startTime) ongoing.startTime else endTime
            eventDao.update(ongoing.copy(endTime = adjustedEnd))
        }
    }

    suspend fun logCompletedNursing(
        startTime: Long,
        endTime: Long,
        nursingType: NursingType,
        amountMl: Int? = null
    ): Long {
        val event = BabyEvent(
            type = EventType.NURSING,
            startTime = startTime,
            endTime = maxOf(startTime, endTime),
            nursingType = nursingType,
            amountMl = amountMl
        )
        return eventDao.insert(event)
    }

    suspend fun logDiaper(
        diaperType: DiaperType,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val event = BabyEvent(
            type = EventType.DIAPER,
            startTime = timestamp,
            endTime = timestamp,
            diaperType = diaperType
        )
        return eventDao.insert(event)
    }
}
