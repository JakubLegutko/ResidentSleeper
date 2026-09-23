package com.residentsleeper.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.residentsleeper.calendar.CalendarSyncManager
import com.residentsleeper.data.local.AppDatabase
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import com.residentsleeper.data.repository.BabyRepository
import com.residentsleeper.domain.FeedingPredictor
import com.residentsleeper.domain.FeedingState
import com.residentsleeper.domain.WakeWindowCalculator
import com.residentsleeper.domain.WakeWindowState
import com.residentsleeper.notifications.BabyAlarmScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val selectedDayStart: Long,
    val selectedDayEnd: Long,
    val isToday: Boolean,
    val events: List<BabyEvent> = emptyList(),
    val ongoingSleep: BabyEvent? = null,
    val ongoingNursing: BabyEvent? = null,
    val wakeWindowState: WakeWindowState,
    val feedingState: FeedingState,
    val activeProfile: BabyProfile = BabyProfile(),
    val allProfiles: List<BabyProfile> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BabyRepository
    private val selectedDayMillis = MutableStateFlow(getStartOfToday())
    private val ticker = MutableStateFlow(System.currentTimeMillis())

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BabyRepository(db.babyEventDao(), db.babyProfileDao())

        // Ensure active profile exists on startup
        viewModelScope.launch {
            repository.getActiveProfile()
        }

        // Background ticker updating every 30 seconds for live countdowns
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                ticker.value = System.currentTimeMillis()
            }
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun getStartOfToday(): Long = getStartOfDay(System.currentTimeMillis())

    val uiState: StateFlow<DashboardUiState> = repository.activeProfileFlow.flatMapLatest { profile ->
        val safeProfile = profile ?: BabyProfile()
        val profileId = safeProfile.id

        combine(
            selectedDayMillis,
            repository.allProfilesFlow,
            repository.getOngoingEventFlow(profileId, EventType.SLEEP),
            repository.getOngoingEventFlow(profileId, EventType.NURSING),
            repository.getLatestEventFlow(profileId, EventType.SLEEP),
            repository.getLatestEventFlow(profileId, EventType.NURSING),
            ticker
        ) { params ->
            val dayStart = params[0] as Long
            val allProfiles = (params[1] as? List<*>)?.filterIsInstance<BabyProfile>() ?: emptyList()
            val ongoingSleep = params[2] as? BabyEvent
            val ongoingNursing = params[3] as? BabyEvent
            val latestSleep = params[4] as? BabyEvent
            val latestNursing = params[5] as? BabyEvent
            val now = params[6] as Long

            val dayEnd = getEndOfDay(dayStart)
            val isToday = dayStart == getStartOfToday()

            val wakeState = WakeWindowCalculator.computeState(safeProfile, latestSleep, ongoingSleep, now)
            val feedingState = FeedingPredictor.computeState(safeProfile, latestNursing, ongoingNursing, now)
            val dayEvents = repository.getEventsInRangeSync(profileId, dayStart, dayEnd)

            DashboardUiState(
                selectedDayStart = dayStart,
                selectedDayEnd = dayEnd,
                isToday = isToday,
                events = dayEvents,
                ongoingSleep = ongoingSleep,
                ongoingNursing = ongoingNursing,
                wakeWindowState = wakeState,
                feedingState = feedingState,
                activeProfile = safeProfile,
                allProfiles = allProfiles
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState(
            selectedDayStart = getStartOfToday(),
            selectedDayEnd = getEndOfDay(getStartOfToday()),
            isToday = true,
            wakeWindowState = WakeWindowCalculator.computeState(BabyProfile(), null, null),
            feedingState = FeedingPredictor.computeState(BabyProfile(), null, null)
        )
    )

    fun switchProfile(profileId: Long) {
        viewModelScope.launch {
            repository.switchActiveProfile(profileId)
            refreshData()
        }
    }

    fun addProfile(name: String, birthDate: Long) {
        viewModelScope.launch {
            repository.createProfile(name, birthDate)
            refreshData()
        }
    }

    fun previousDay() {
        selectedDayMillis.value = selectedDayMillis.value - (24 * 60 * 60 * 1000)
    }

    fun nextDay() {
        val next = selectedDayMillis.value + (24 * 60 * 60 * 1000)
        if (next <= getStartOfToday()) {
            selectedDayMillis.value = next
        }
    }

    fun goToToday() {
        selectedDayMillis.value = getStartOfToday()
    }

    // --- Actions ---

    fun onSleepButtonClick(adjustedTime: Long? = null) {
        viewModelScope.launch {
            val now = adjustedTime ?: System.currentTimeMillis()
            val state = uiState.value
            val profile = state.activeProfile
            val profileId = profile.id
            val context = getApplication<Application>()

            if (state.ongoingSleep != null) {
                // End sleep -> baby wakes up -> start wake window
                repository.endOngoingSleep(profileId, now)

                // Reschedule wake alerts
                val latestSleep = repository.getLatestEvent(profileId, EventType.SLEEP)
                val wakeState = WakeWindowCalculator.computeState(profile, latestSleep, null, now)

                if (profile.enablePushNotifications && wakeState.alert10MinTimestamp != null) {
                    BabyAlarmScheduler.scheduleWakeWindowAlert(context, wakeState.alert10MinTimestamp)
                }

                // Sync to Google Calendar
                if (profile.enableCalendarSync && profile.selectedCalendarId != null && wakeState.expectedWakeEndTime != null) {
                    CalendarSyncManager.createCalendarEventWithReminder(
                        context = context,
                        calendarId = profile.selectedCalendarId,
                        title = "${profile.name} - Activity Cycle End",
                        description = "Recommended end of wake window for nap time",
                        startTimeMillis = wakeState.expectedWakeEndTime,
                        endTimeMillis = wakeState.expectedWakeEndTime + 30 * 60 * 1000,
                        reminderMinutes = profile.notifyBeforeMinutes
                    )
                }
            } else {
                // Start sleep -> baby fell asleep -> cancel wake alerts
                repository.startSleep(profileId, now)
                BabyAlarmScheduler.cancelWakeWindowAlert(context)
            }
            refreshData()
        }
    }

    fun onNursingButtonClick(
        nursingType: NursingType = NursingType.LEFT_BREAST,
        amountMl: Int? = null,
        adjustedTime: Long? = null
    ) {
        viewModelScope.launch {
            val now = adjustedTime ?: System.currentTimeMillis()
            val state = uiState.value
            val profile = state.activeProfile
            val profileId = profile.id
            val context = getApplication<Application>()

            if (state.ongoingNursing != null) {
                // End nursing
                repository.endOngoingNursing(profileId, now)
            } else {
                // Start nursing
                repository.startNursing(profileId, nursingType, amountMl, now)

                // Schedule next feeding alerts
                val latestNursing = repository.getLatestEvent(profileId, EventType.NURSING)
                val feedState = FeedingPredictor.computeState(profile, latestNursing, null, now)

                if (profile.enablePushNotifications && feedState.alert10MinTimestamp != null) {
                    BabyAlarmScheduler.scheduleFeedingAlert(context, feedState.alert10MinTimestamp)
                }

                // Sync to Google Calendar
                if (profile.enableCalendarSync && profile.selectedCalendarId != null && feedState.nextFeedEstimateTime != null) {
                    CalendarSyncManager.createCalendarEventWithReminder(
                        context = context,
                        calendarId = profile.selectedCalendarId,
                        title = "${profile.name} - Feeding Time",
                        description = "Approximated next feeding schedule",
                        startTimeMillis = feedState.nextFeedEstimateTime,
                        endTimeMillis = feedState.nextFeedEstimateTime + 30 * 60 * 1000,
                        reminderMinutes = profile.notifyBeforeMinutes
                    )
                }
            }
            refreshData()
        }
    }

    fun onDiaperClick(diaperType: DiaperType, adjustedTime: Long? = null) {
        viewModelScope.launch {
            val time = adjustedTime ?: System.currentTimeMillis()
            repository.logDiaper(uiState.value.activeProfile.id, diaperType, time)
            refreshData()
        }
    }

    private fun refreshData() {
        ticker.value = System.currentTimeMillis()
    }
}
