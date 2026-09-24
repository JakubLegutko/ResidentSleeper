package com.residentsleeper.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.residentsleeper.calendar.CalendarSyncManager
import com.residentsleeper.calendar.DeviceCalendar
import com.residentsleeper.data.backup.DataBackupManager
import com.residentsleeper.data.local.AppDatabase
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.Gender
import com.residentsleeper.data.repository.BabyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val activeProfile: BabyProfile = BabyProfile(),
    val allProfiles: List<BabyProfile> = emptyList(),
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val backupMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BabyRepository
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BabyRepository(db.babyEventDao(), db.babyProfileDao())
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val active = repository.getActiveProfile()
            val all = dbProfiles()
            val context = getApplication<Application>()
            val hasPerm = CalendarSyncManager.hasCalendarPermission(context)
            val calendars = if (hasPerm) CalendarSyncManager.getAvailableCalendars(context) else emptyList()

            _uiState.value = _uiState.value.copy(
                activeProfile = active,
                allProfiles = all,
                availableCalendars = calendars,
                hasCalendarPermission = hasPerm
            )
        }
    }

    private suspend fun dbProfiles(): List<BabyProfile> {
        val db = AppDatabase.getDatabase(getApplication())
        return db.babyProfileDao().getAllProfilesSync()
    }

    private var nameDebounceJob: Job? = null

    fun switchProfile(profileId: Long) {
        flushPendingNameSave()
        viewModelScope.launch {
            repository.switchActiveProfile(profileId)
            loadProfile()
        }
    }

    fun addProfile(name: String, birthDate: Long, gender: Gender = Gender.UNSPECIFIED) {
        flushPendingNameSave()
        viewModelScope.launch {
            repository.createProfile(name, birthDate, gender)
            loadProfile()
        }
    }

    fun updateActiveProfileGender(gender: Gender) {
        updateActiveProfile { it.copy(gender = gender) }
    }

    fun updateProfileGender(profileId: Long, gender: Gender) {
        viewModelScope.launch {
            val profile = repository.getProfileById(profileId) ?: return@launch
            repository.updateProfile(profile.copy(gender = gender))
            loadProfile()
        }
    }

    fun deleteProfile(profileId: Long) {
        flushPendingNameSave()
        viewModelScope.launch {
            repository.deleteProfile(profileId)
            loadProfile()
        }
    }

    fun updateActiveProfileName(name: String) {
        val current = _uiState.value.activeProfile
        if (current.name == name) return
        val updated = current.copy(name = name)
        val updatedAll = _uiState.value.allProfiles.map {
            if (it.id == updated.id) updated else it
        }
        _uiState.value = _uiState.value.copy(activeProfile = updated, allProfiles = updatedAll)

        nameDebounceJob?.cancel()
        nameDebounceJob = viewModelScope.launch {
            delay(350)
            repository.updateProfile(updated)
        }
    }

    fun flushPendingNameSave() {
        val job = nameDebounceJob
        if (job != null && job.isActive) {
            job.cancel()
            val current = _uiState.value.activeProfile
            viewModelScope.launch {
                repository.updateProfile(current)
            }
        }
    }

    fun updateBirthDate(timestamp: Long) {
        updateActiveProfile { it.copy(birthTimestamp = timestamp) }
    }

    fun updateWakeWindow(customMinutes: Int?) {
        updateActiveProfile { it.copy(customWakeWindowMinutes = customMinutes) }
    }

    fun updateFeedingInterval(minutes: Int) {
        updateActiveProfile { it.copy(feedingIntervalMinutes = minutes) }
    }

    fun updateDayStartHour(hour: Int) {
        updateActiveProfile { it.copy(dayStartHour = hour) }
    }

    fun updateMaxRecommendedNightFeeds(count: Int) {
        updateActiveProfile { it.copy(maxRecommendedNightFeeds = count) }
    }

    fun toggleNotifyForOptionalNightFeeds(enabled: Boolean) {
        updateActiveProfile { it.copy(notifyForOptionalNightFeeds = enabled) }
    }

    fun toggleCalendarSync(enabled: Boolean) {
        updateActiveProfile { it.copy(enableCalendarSync = enabled) }
    }

    fun selectCalendar(calendarId: Long) {
        updateActiveProfile { it.copy(selectedCalendarId = calendarId) }
    }

    fun togglePushNotifications(enabled: Boolean) {
        updateActiveProfile { it.copy(enablePushNotifications = enabled) }
    }

    private fun updateActiveProfile(block: (BabyProfile) -> BabyProfile) {
        viewModelScope.launch {
            val current = _uiState.value.activeProfile
            val updated = block(current)
            repository.updateProfile(updated)
            _uiState.value = _uiState.value.copy(activeProfile = updated)
            loadProfile()
        }
    }

    // --- Export & Import ---

    suspend fun getJsonExportData(): String {
        return repository.exportJsonBackup()
    }

    suspend fun getCsvExportData(): String {
        return repository.exportCsvSpreadsheet()
    }

    fun importBackupData(jsonString: String, replaceAll: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = DataBackupManager.importFromJson(jsonString)
                repository.importBackup(backupData, replaceAll)
                loadProfile()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun clearBackupMessage() {
        _uiState.value = _uiState.value.copy(backupMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        if (nameDebounceJob?.isActive == true) {
            nameDebounceJob?.cancel()
            val current = _uiState.value.activeProfile
            CoroutineScope(Dispatchers.IO).launch {
                repository.updateProfile(current)
            }
        }
    }
}
