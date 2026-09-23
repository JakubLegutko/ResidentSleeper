package com.residentsleeper.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.residentsleeper.calendar.CalendarSyncManager
import com.residentsleeper.calendar.DeviceCalendar
import com.residentsleeper.data.local.AppDatabase
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.repository.BabyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: BabyProfile = BabyProfile(),
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val hasCalendarPermission: Boolean = false
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
            val profile = repository.getProfile()
            val context = getApplication<Application>()
            val hasPerm = CalendarSyncManager.hasCalendarPermission(context)
            val calendars = if (hasPerm) CalendarSyncManager.getAvailableCalendars(context) else emptyList()

            _uiState.value = SettingsUiState(
                profile = profile,
                availableCalendars = calendars,
                hasCalendarPermission = hasPerm
            )
        }
    }

    fun updateBirthDate(timestamp: Long) {
        updateProfile { it.copy(birthTimestamp = timestamp) }
    }

    fun updateWakeWindow(customMinutes: Int?) {
        updateProfile { it.copy(customWakeWindowMinutes = customMinutes) }
    }

    fun updateFeedingInterval(minutes: Int) {
        updateProfile { it.copy(feedingIntervalMinutes = minutes) }
    }

    fun toggleCalendarSync(enabled: Boolean) {
        updateProfile { it.copy(enableCalendarSync = enabled) }
    }

    fun selectCalendar(calendarId: Long) {
        updateProfile { it.copy(selectedCalendarId = calendarId) }
    }

    fun togglePushNotifications(enabled: Boolean) {
        updateProfile { it.copy(enablePushNotifications = enabled) }
    }

    private fun updateProfile(block: (BabyProfile) -> BabyProfile) {
        viewModelScope.launch {
            val updated = block(_uiState.value.profile)
            repository.updateProfile(updated)
            _uiState.value = _uiState.value.copy(profile = updated)
        }
    }
}
