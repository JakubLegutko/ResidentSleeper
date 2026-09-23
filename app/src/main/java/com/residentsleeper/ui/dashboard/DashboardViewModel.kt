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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val profile: BabyProfile = BabyProfile()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BabyRepository
    private val selectedDayMillis = MutableStateFlow(getStartOfToday())
    private val ticker = MutableStateFlow(System.currentTimeMillis())

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BabyRepository(db.babyEventDao(), db.babyProfileDao())

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

    val uiState: StateFlow<DashboardUiState> = combine(
        selectedDayMillis,
        repository.profileFlow,
        repository.getOngoingEventFlow(EventType.SLEEP),
        repository.getOngoingEventFlow(EventType.NURSING),
        repository.getLatestEventFlow(EventType.SLEEP),
        repository.getLatestEventFlow(EventType.NURSING),
        ticker
    ) { params ->
        val dayStart = params[0] as Long
        val profile = (params[1] as? BabyProfile) ?: BabyProfile()
        val ongoingSleep = params[2] as? BabyEvent
        val ongoingNursing = params[3] as? BabyEvent
        val latestSleep = params[4] as? BabyEvent
        val latestNursing = params[5] as? BabyEvent
        val now = params[6] as Long

        val dayEnd = getEndOfDay(dayStart)
        val isToday = dayStart == getStartOfToday()

        val wakeState = WakeWindowCalculator.computeState(profile, latestSleep, ongoingSleep, now)
        val feedingState = FeedingPredictor.computeState(profile, latestNursing, ongoingNursing, now)

        DashboardUiState(
            selectedDayStart = dayStart,
            selectedDayEnd = dayEnd,
            isToday = isToday,
            events = repository.getEventsInRangeSync(dayStart, dayEnd),
            ongoingSleep = ongoingSleep,
            ongoingNursing = ongoingNursing,
            wakeWindowState = wakeState,
            feedingState = feedingState,
            profile = profile
        )
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
            val context = getApplication<Application>()

            if (state.ongoingSleep != null) {
                // End sleep -> baby wakes up -> start wake window
                repository.endOngoingSleep(now)

                // Reschedule wake alerts
                val profile = repository.getProfile()
                val latestSleep = repository.getLatestEvent(EventType.SLEEP)
                val wakeState = WakeWindowCalculator.computeState(profile, latestSleep, null, now)

                if (profile.enablePushNotifications && wakeState.alert10MinTimestamp != null) {
                    BabyAlarmScheduler.scheduleWakeWindowAlert(context, wakeState.alert10MinTimestamp)
                }

                // Sync to Google Calendar
                if (profile.enableCalendarSync && profile.selectedCalendarId != null && wakeState.expectedWakeEndTime != null) {
                    CalendarSyncManager.createCalendarEventWithReminder(
                        context = context,
                        calendarId = profile.selectedCalendarId,
                        title = "Baby Activity Cycle End",
                        description = "Recommended end of wake window for nap time",
                        startTimeMillis = wakeState.expectedWakeEndTime,
                        endTimeMillis = wakeState.expectedWakeEndTime + 30 * 60 * 1000,
                        reminderMinutes = profile.notifyBeforeMinutes
                    )
                }
            } else {
                // Start sleep -> baby fell asleep -> cancel wake alerts
                repository.startSleep(now)
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
            val context = getApplication<Application>()

            if (state.ongoingNursing != null) {
                // End nursing
                repository.endOngoingNursing(now)
            } else {
                // Start nursing
                repository.startNursing(nursingType, amountMl, now)

                // Schedule next feeding alerts
                val profile = repository.getProfile()
                val latestNursing = repository.getLatestEvent(EventType.NURSING)
                val feedState = FeedingPredictor.computeState(profile, latestNursing, null, now)

                if (profile.enablePushNotifications && feedState.alert10MinTimestamp != null) {
                    BabyAlarmScheduler.scheduleFeedingAlert(context, feedState.alert10MinTimestamp)
                }

                // Sync to Google Calendar
                if (profile.enableCalendarSync && profile.selectedCalendarId != null && feedState.nextFeedEstimateTime != null) {
                    CalendarSyncManager.createCalendarEventWithReminder(
                        context = context,
                        calendarId = profile.selectedCalendarId,
                        title = "Baby Feeding Time",
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
            repository.logDiaper(diaperType, time)
            refreshData()
        }
    }

    private fun refreshData() {
        ticker.value = System.currentTimeMillis()
    }
}
