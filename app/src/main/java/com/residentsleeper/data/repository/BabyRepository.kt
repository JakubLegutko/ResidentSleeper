package com.residentsleeper.data.repository

import com.residentsleeper.data.backup.BackupData
import com.residentsleeper.data.backup.DataBackupManager
import com.residentsleeper.data.local.BabyEventDao
import com.residentsleeper.data.local.BabyProfileDao
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import kotlinx.coroutines.flow.Flow

class BabyRepository(
    private val eventDao: BabyEventDao,
    private val profileDao: BabyProfileDao
) {
    val activeProfileFlow: Flow<BabyProfile?> = profileDao.getActiveProfileFlow()
    val allProfilesFlow: Flow<List<BabyProfile>> = profileDao.getAllProfiles()

    suspend fun getActiveProfile(): BabyProfile {
        val existing = profileDao.getActiveProfile()
        if (existing != null) return existing

        val all = profileDao.getAllProfilesSync()
        if (all.isNotEmpty()) {
            profileDao.setActiveProfile(all.first().id)
            return all.first().copy(isActive = true)
        }

        // Initialize default profile if database is completely fresh
        val defaultProfile = BabyProfile(
            name = "Baby",
            birthTimestamp = System.currentTimeMillis(),
            isActive = true
        )
        val id = profileDao.insert(defaultProfile)
        return defaultProfile.copy(id = id)
    }

    suspend fun switchActiveProfile(profileId: Long) {
        profileDao.setActiveProfile(profileId)
    }

    suspend fun createProfile(name: String, birthTimestamp: Long): Long {
        val newProfile = BabyProfile(
            name = name,
            birthTimestamp = birthTimestamp,
            isActive = false
        )
        val id = profileDao.insert(newProfile)
        switchActiveProfile(id)
        return id
    }

    suspend fun updateProfile(profile: BabyProfile) {
        profileDao.update(profile)
    }

    suspend fun deleteProfile(profileId: Long) {
        val profile = profileDao.getProfileById(profileId) ?: return
        profileDao.delete(profile)
        eventDao.deleteEventsForProfile(profileId)

        // If the deleted profile was active, activate the first remaining profile
        val remaining = profileDao.getAllProfilesSync()
        if (remaining.isNotEmpty()) {
            profileDao.setActiveProfile(remaining.first().id)
        } else {
            // Recreate default
            getActiveProfile()
        }
    }

    // --- Profile-filtered Event Queries ---

    fun getEventsForDay(profileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<BabyEvent>> {
        return eventDao.getEventsForDay(profileId, startOfDay, endOfDay)
    }

    fun getEventsInRange(profileId: Long, from: Long, to: Long): Flow<List<BabyEvent>> {
        return eventDao.getEventsInRange(profileId, from, to)
    }

    suspend fun getEventsInRangeSync(profileId: Long, from: Long, to: Long): List<BabyEvent> {
        return eventDao.getEventsInRangeSync(profileId, from, to)
    }

    fun getOngoingEventFlow(profileId: Long, type: EventType): Flow<BabyEvent?> {
        return eventDao.getOngoingEventFlow(profileId, type)
    }

    suspend fun getOngoingEvent(profileId: Long, type: EventType): BabyEvent? {
        return eventDao.getOngoingEvent(profileId, type)
    }

    fun getLatestEventFlow(profileId: Long, type: EventType): Flow<BabyEvent?> {
        return eventDao.getLatestEventFlow(profileId, type)
    }

    suspend fun getLatestEvent(profileId: Long, type: EventType): BabyEvent? {
        return eventDao.getLatestEvent(profileId, type)
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

    // --- High-level logging actions (bound to profile) ---

    suspend fun startSleep(profileId: Long, startTime: Long = System.currentTimeMillis()): Long {
        endOngoingSleep(profileId, startTime)
        val event = BabyEvent(
            babyProfileId = profileId,
            type = EventType.SLEEP,
            startTime = startTime,
            endTime = null
        )
        return eventDao.insert(event)
    }

    suspend fun endOngoingSleep(profileId: Long, endTime: Long = System.currentTimeMillis()) {
        val ongoing = eventDao.getOngoingEvent(profileId, EventType.SLEEP)
        if (ongoing != null) {
            val adjustedEnd = if (endTime < ongoing.startTime) ongoing.startTime else endTime
            eventDao.update(ongoing.copy(endTime = adjustedEnd))
        }
    }

    suspend fun logCompletedSleep(profileId: Long, startTime: Long, endTime: Long): Long {
        val event = BabyEvent(
            babyProfileId = profileId,
            type = EventType.SLEEP,
            startTime = startTime,
            endTime = maxOf(startTime, endTime)
        )
        return eventDao.insert(event)
    }

    suspend fun startNursing(
        profileId: Long,
        nursingType: NursingType,
        amountMl: Int? = null,
        startTime: Long = System.currentTimeMillis()
    ): Long {
        endOngoingNursing(profileId, startTime)
        val event = BabyEvent(
            babyProfileId = profileId,
            type = EventType.NURSING,
            startTime = startTime,
            endTime = null,
            nursingType = nursingType,
            amountMl = amountMl
        )
        return eventDao.insert(event)
    }

    suspend fun endOngoingNursing(profileId: Long, endTime: Long = System.currentTimeMillis()) {
        val ongoing = eventDao.getOngoingEvent(profileId, EventType.NURSING)
        if (ongoing != null) {
            val adjustedEnd = if (endTime < ongoing.startTime) ongoing.startTime else endTime
            eventDao.update(ongoing.copy(endTime = adjustedEnd))
        }
    }

    suspend fun logCompletedNursing(
        profileId: Long,
        startTime: Long,
        endTime: Long,
        nursingType: NursingType,
        amountMl: Int? = null
    ): Long {
        val event = BabyEvent(
            babyProfileId = profileId,
            type = EventType.NURSING,
            startTime = startTime,
            endTime = maxOf(startTime, endTime),
            nursingType = nursingType,
            amountMl = amountMl
        )
        return eventDao.insert(event)
    }

    suspend fun logDiaper(
        profileId: Long,
        diaperType: DiaperType,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val event = BabyEvent(
            babyProfileId = profileId,
            type = EventType.DIAPER,
            startTime = timestamp,
            endTime = timestamp,
            diaperType = diaperType
        )
        return eventDao.insert(event)
    }

    // --- Backup & Data Portability ---

    suspend fun exportJsonBackup(): String {
        val profiles = profileDao.getAllProfilesSync()
        val events = eventDao.getAllEvents()
        return DataBackupManager.exportToJson(profiles, events)
    }

    suspend fun exportCsvSpreadsheet(): String {
        val profiles = profileDao.getAllProfilesSync()
        val events = eventDao.getAllEvents()
        return DataBackupManager.exportToCsv(profiles, events)
    }

    suspend fun importBackup(backupData: BackupData, replaceAll: Boolean) {
        if (replaceAll) {
            eventDao.deleteAll()
            profileDao.deleteAll()
            profileDao.insertAll(backupData.profiles)
            eventDao.insertAll(backupData.events)
        } else {
            // Merge: insert or update profiles and events
            profileDao.insertAll(backupData.profiles)
            eventDao.insertAll(backupData.events)
        }

        // Ensure at least one profile is set as active
        val active = profileDao.getActiveProfile()
        if (active == null) {
            val all = profileDao.getAllProfilesSync()
            if (all.isNotEmpty()) {
                profileDao.setActiveProfile(all.first().id)
            }
        }
    }
}
