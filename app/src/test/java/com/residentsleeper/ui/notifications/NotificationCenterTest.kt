package com.residentsleeper.ui.notifications

import com.google.common.truth.Truth.assertThat
import com.residentsleeper.data.local.AppNotificationDao
import com.residentsleeper.data.local.BabyEventDao
import com.residentsleeper.data.local.BabyProfileDao
import com.residentsleeper.data.model.AppNotification
import com.residentsleeper.data.repository.BabyRepository
import com.residentsleeper.notifications.BabyAlarmScheduler
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class FakeAppNotificationDao : AppNotificationDao {
    private val notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private var nextId = 1L

    override fun getNotificationsSince(cutoffTimestamp: Long): Flow<List<AppNotification>> {
        return notifications.map { list ->
            list.filter { it.timestamp >= cutoffTimestamp }.sortedByDescending { it.timestamp }
        }
    }

    override fun getUnreadCountSince(cutoffTimestamp: Long): Flow<Int> {
        return notifications.map { list ->
            list.count { it.timestamp >= cutoffTimestamp && !it.isRead }
        }
    }

    override fun getAllNotifications(): Flow<List<AppNotification>> {
        return notifications.map { it.sortedByDescending { notif -> notif.timestamp } }
    }

    override suspend fun getAllNotificationsSync(): List<AppNotification> {
        return notifications.value.sortedByDescending { it.timestamp }
    }

    override suspend fun insert(notification: AppNotification): Long {
        val id = if (notification.id == 0L) nextId++ else notification.id
        val newNotification = notification.copy(id = id)
        val current = notifications.value.toMutableList()
        current.removeAll { it.id == id }
        current.add(newNotification)
        notifications.value = current
        return id
    }

    override suspend fun insertAll(notificationsList: List<AppNotification>) {
        notificationsList.forEach { insert(it) }
    }

    override suspend fun update(notification: AppNotification) {
        val current = notifications.value.toMutableList()
        val index = current.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            current[index] = notification
            notifications.value = current
        }
    }

    override suspend fun markAllAsRead() {
        notifications.value = notifications.value.map { it.copy(isRead = true) }
    }

    override suspend fun markAsRead(id: Long) {
        notifications.value = notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    override suspend fun deleteOlderThan(cutoffTimestamp: Long) {
        notifications.value = notifications.value.filter { it.timestamp >= cutoffTimestamp }
    }

    override suspend fun deleteById(id: Long) {
        notifications.value = notifications.value.filter { it.id != id }
    }

    override suspend fun clearAll() {
        notifications.value = emptyList()
    }
}

class NotificationCenterTest {

    private lateinit var fakeDao: FakeAppNotificationDao
    private lateinit var repository: BabyRepository

    @Before
    fun setUp() {
        fakeDao = FakeAppNotificationDao()
        val mockEventDao = mockk<BabyEventDao>(relaxed = true)
        val mockProfileDao = mockk<BabyProfileDao>(relaxed = true)
        repository = BabyRepository(mockEventDao, mockProfileDao, fakeDao)
    }

    @Test
    fun notifications_filtersToLast24HoursOnly() = runBlocking {
        val now = 1_000_000_000L
        val oneHourAgo = now - 3600 * 1000L
        val twentyThreeHoursAgo = now - 23 * 3600 * 1000L
        val twentyFiveHoursAgo = now - 25 * 3600 * 1000L
        val twoDaysAgo = now - 48 * 3600 * 1000L

        fakeDao.insert(
            AppNotification(
                id = 1,
                title = "Wake Window Ended",
                message = "Baby's wake window ends soon",
                type = BabyAlarmScheduler.ALERT_TYPE_WAKE_WINDOW,
                timestamp = oneHourAgo
            )
        )
        fakeDao.insert(
            AppNotification(
                id = 2,
                title = "Feeding Approaching",
                message = "Feeding in 10 minutes",
                type = BabyAlarmScheduler.ALERT_TYPE_FEEDING,
                timestamp = twentyThreeHoursAgo
            )
        )
        fakeDao.insert(
            AppNotification(
                id = 3,
                title = "Old Alert",
                message = "Should not appear in 24h center",
                type = BabyAlarmScheduler.ALERT_TYPE_WAKE_WINDOW,
                timestamp = twentyFiveHoursAgo
            )
        )
        fakeDao.insert(
            AppNotification(
                id = 4,
                title = "Very Old Alert",
                message = "Two days old",
                type = BabyAlarmScheduler.ALERT_TYPE_FEEDING,
                timestamp = twoDaysAgo
            )
        )

        val cutoff = now - 24 * 3600 * 1000L
        val last24h = repository.getNotificationsLast24HoursFlow(cutoff).first()

        assertThat(last24h).hasSize(2)
        assertThat(last24h[0].id).isEqualTo(1)
        assertThat(last24h[1].id).isEqualTo(2)
    }

    @Test
    fun unreadCount_calculatesAccuratelyForLast24Hours() = runBlocking {
        val now = 2_000_000_000L
        val cutoff = now - 24 * 3600 * 1000L

        fakeDao.insert(
            AppNotification(
                id = 1,
                title = "Alert 1",
                message = "Message 1",
                type = BabyAlarmScheduler.ALERT_TYPE_WAKE_WINDOW,
                timestamp = now - 1000L,
                isRead = false
            )
        )
        fakeDao.insert(
            AppNotification(
                id = 2,
                title = "Alert 2",
                message = "Message 2",
                type = BabyAlarmScheduler.ALERT_TYPE_FEEDING,
                timestamp = now - 2000L,
                isRead = true
            )
        )
        fakeDao.insert(
            AppNotification(
                id = 3,
                title = "Alert 3",
                message = "Message 3",
                type = BabyAlarmScheduler.ALERT_TYPE_WAKE_WINDOW,
                timestamp = now - 3000L,
                isRead = false
            )
        )
        // Outside 24h: even if unread, should not count towards current 24h unread count
        fakeDao.insert(
            AppNotification(
                id = 4,
                title = "Expired Alert",
                message = "Message 4",
                type = BabyAlarmScheduler.ALERT_TYPE_WAKE_WINDOW,
                timestamp = now - 25 * 3600 * 1000L,
                isRead = false
            )
        )

        val unreadCount = repository.getUnreadNotificationsCountFlow(cutoff).first()
        assertThat(unreadCount).isEqualTo(2)
    }

    @Test
    fun markAsRead_updatesSingleNotification() = runBlocking {
        val now = 1_000_000L
        val id = fakeDao.insert(
            AppNotification(
                title = "Alert",
                message = "Body",
                type = BabyAlarmScheduler.ALERT_TYPE_WAKE_WINDOW,
                timestamp = now,
                isRead = false
            )
        )

        repository.markNotificationAsRead(id)

        val list = fakeDao.getAllNotificationsSync()
        assertThat(list.first().isRead).isTrue()
    }

    @Test
    fun markAllAsRead_marksAllNotificationsRead() = runBlocking {
        val now = 1_000_000L
        fakeDao.insert(AppNotification(id = 1, title = "A1", message = "M1", type = "T", timestamp = now, isRead = false))
        fakeDao.insert(AppNotification(id = 2, title = "A2", message = "M2", type = "T", timestamp = now, isRead = false))

        repository.markAllNotificationsAsRead()

        val list = fakeDao.getAllNotificationsSync()
        assertThat(list).hasSize(2)
        assertThat(list.all { it.isRead }).isTrue()
    }

    @Test
    fun deleteNotification_removesSpecificNotification() = runBlocking {
        val now = 1_000_000L
        val id1 = fakeDao.insert(AppNotification(title = "A1", message = "M1", type = "T", timestamp = now))
        val id2 = fakeDao.insert(AppNotification(title = "A2", message = "M2", type = "T", timestamp = now))

        repository.deleteNotification(id1)

        val list = fakeDao.getAllNotificationsSync()
        assertThat(list).hasSize(1)
        assertThat(list.first().id).isEqualTo(id2)
    }

    @Test
    fun clearAllNotifications_emptiesStoredNotifications() = runBlocking {
        val now = 1_000_000L
        fakeDao.insert(AppNotification(title = "A1", message = "M1", type = "T", timestamp = now))
        fakeDao.insert(AppNotification(title = "A2", message = "M2", type = "T", timestamp = now))

        repository.clearAllNotifications()

        val list = fakeDao.getAllNotificationsSync()
        assertThat(list).isEmpty()
    }

    @Test
    fun pruneOldNotifications_deletesRecordsOlderThan24Hours() = runBlocking {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600 * 1000L
        val thirtyHoursAgo = now - 30 * 3600 * 1000L

        fakeDao.insert(AppNotification(id = 1, title = "Recent", message = "M1", type = "T", timestamp = oneHourAgo))
        fakeDao.insert(AppNotification(id = 2, title = "Old", message = "M2", type = "T", timestamp = thirtyHoursAgo))

        repository.pruneOldNotifications()

        val remaining = fakeDao.getAllNotificationsSync()
        assertThat(remaining).hasSize(1)
        assertThat(remaining.first().id).isEqualTo(1)
    }
}
