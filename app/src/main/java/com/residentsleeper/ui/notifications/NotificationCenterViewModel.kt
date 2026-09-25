package com.residentsleeper.ui.notifications

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.residentsleeper.data.local.AppDatabase
import com.residentsleeper.data.model.AppNotification
import com.residentsleeper.data.repository.BabyRepository
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

data class NotificationCenterUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCenterViewModel(
    application: Application,
    customRepository: BabyRepository? = null
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, null)

    private val repository: BabyRepository = customRepository ?: run {
        val db = AppDatabase.getDatabase(application)
        BabyRepository(db.babyEventDao(), db.babyProfileDao(), db.appNotificationDao())
    }

    private val ticker = MutableStateFlow(System.currentTimeMillis())

    init {
        // Periodic ticker to automatically expire items older than 24h
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                ticker.value = System.currentTimeMillis()
                repository.pruneOldNotifications()
            }
        }
    }

    val uiState: StateFlow<NotificationCenterUiState> = ticker.flatMapLatest { now ->
        val cutoff = now - 24 * 60 * 60 * 1000L
        combine(
            repository.getNotificationsLast24HoursFlow(cutoff),
            repository.getUnreadNotificationsCountFlow(cutoff)
        ) { notifs, unreadCount ->
            NotificationCenterUiState(
                notifications = notifs,
                unreadCount = unreadCount,
                isLoading = false
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NotificationCenterUiState(isLoading = true)
    )

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun sendTestNotification(context: Context) {
        BabyAlarmScheduler.triggerTestAlert(context)
    }

    fun addNotification(title: String, message: String, type: String = BabyAlarmScheduler.ALERT_TYPE_TEST) {
        viewModelScope.launch {
            repository.insertNotification(
                AppNotification(
                    title = title,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
