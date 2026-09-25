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

import com.residentsleeper.domain.FeedingIntervalCalculationResult
import com.residentsleeper.domain.FeedingPredictor
import com.residentsleeper.domain.WakeWindowCalculationResult
import com.residentsleeper.domain.WakeWindowCalculator
import java.util.concurrent.TimeUnit

data class SettingsUiState(
    val activeProfile: BabyProfile = BabyProfile(),
    val allProfiles: List<BabyProfile> = emptyList(),
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val backupMessage: String? = null,
    val feedingCalculationResult: FeedingIntervalCalculationResult? = null,
    val autoFeedingIntervalPreview: FeedingIntervalCalculationResult? = null,
    val wakeWindowCalculationResult: WakeWindowCalculationResult? = null,
    val autoWakeWindowPreview: WakeWindowCalculationResult? = null
)

class SettingsViewModel(
    application: Application,
    customRepository: BabyRepository? = null
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, null)

    private val repository: BabyRepository = customRepository ?: run {
        val db = AppDatabase.getDatabase(application)
        BabyRepository(db.babyEventDao(), db.babyProfileDao(), db.appNotificationDao())
    }
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val active = repository.getActiveProfile()
            val all = dbProfiles()
            val context = getApplication<Application>()
            val hasPerm = CalendarSyncManager.hasCalendarPermission(context)
            val calendars = if (hasPerm) CalendarSyncManager.getAvailableCalendars(context) else emptyList()

            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
            val recentEvents = repository.getEventsInRangeSync(active.id, sevenDaysAgo, now)
            val autoFeedPreview = FeedingPredictor.calculateOptimalInterval(active, recentEvents, now)
            val autoWakePreview = WakeWindowCalculator.calculateOptimalWakeWindow(active, recentEvents, now)

            _uiState.value = _uiState.value.copy(
                activeProfile = active,
                allProfiles = all,
                availableCalendars = calendars,
                hasCalendarPermission = hasPerm,
                autoFeedingIntervalPreview = autoFeedPreview,
                autoWakeWindowPreview = autoWakePreview
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

    fun updateFeedingInterval(minutes: Int?) {
        updateActiveProfile {
            if (minutes != null) {
                it.copy(
                    customFeedingIntervalMinutes = minutes,
                    feedingIntervalMinutes = minutes
                )
            } else {
                it.copy(customFeedingIntervalMinutes = null)
            }
        }
    }

    fun showFeedingCalculationDetails() {
        viewModelScope.launch {
            val profile = repository.getActiveProfile()
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
            val recentEvents = repository.getEventsInRangeSync(profile.id, sevenDaysAgo, now)
            val result = FeedingPredictor.calculateOptimalInterval(profile, recentEvents, now)

            _uiState.value = _uiState.value.copy(
                feedingCalculationResult = result
            )
        }
    }

    fun applyAutoFeedingInterval() {
        updateFeedingInterval(null)
        dismissFeedingCalculationDialog()
    }

    fun dismissFeedingCalculationDialog() {
        _uiState.value = _uiState.value.copy(feedingCalculationResult = null)
    }

    fun showWakeWindowCalculationDetails() {
        viewModelScope.launch {
            val profile = repository.getActiveProfile()
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
            val recentEvents = repository.getEventsInRangeSync(profile.id, sevenDaysAgo, now)
            val result = WakeWindowCalculator.calculateOptimalWakeWindow(profile, recentEvents, now)

            _uiState.value = _uiState.value.copy(
                wakeWindowCalculationResult = result
            )
        }
    }

    fun applyAutoWakeWindow() {
        updateWakeWindow(null)
        dismissWakeWindowCalculationDialog()
    }

    fun dismissWakeWindowCalculationDialog() {
        _uiState.value = _uiState.value.copy(wakeWindowCalculationResult = null)
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

    fun updateTimeFormat(use12Hour: Boolean) {
        updateActiveProfile { it.copy(use12HourFormat = use12Hour) }
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
