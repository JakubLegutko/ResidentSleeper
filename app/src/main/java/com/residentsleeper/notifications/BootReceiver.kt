package com.residentsleeper.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.residentsleeper.data.local.AppDatabase
import com.residentsleeper.data.model.EventType
import com.residentsleeper.domain.FeedingPredictor
import com.residentsleeper.domain.WakeWindowCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val profile = db.babyProfileDao().getActiveProfile()
                    if (profile != null && profile.enablePushNotifications) {
                        val latestSleep = db.babyEventDao().getLatestEvent(profile.id, EventType.SLEEP)
                        val ongoingSleep = db.babyEventDao().getOngoingEvent(profile.id, EventType.SLEEP)
                        val wakeState = WakeWindowCalculator.computeState(profile, latestSleep, ongoingSleep, context = context)

                        if (!wakeState.isSleeping && wakeState.alert10MinTimestamp != null) {
                            val notifTitle = "${profile.name}: ${wakeState.recommendationTitle}"
                            val notifBody = "${wakeState.recommendationReason}\n\n💡 Tip: ${wakeState.triviaTip}"
                            BabyAlarmScheduler.scheduleWakeWindowAlert(context, wakeState.alert10MinTimestamp, notifTitle, notifBody)
                        }

                        val latestNursing = db.babyEventDao().getLatestEvent(profile.id, EventType.NURSING)
                        val ongoingNursing = db.babyEventDao().getOngoingEvent(profile.id, EventType.NURSING)
                        val now = System.currentTimeMillis()
                        val sevenDaysAgo = now - java.util.concurrent.TimeUnit.DAYS.toMillis(7)
                        val recentEvents = db.babyEventDao().getEventsInRangeSync(profile.id, sevenDaysAgo, now)
                        val feedState = FeedingPredictor.computeState(profile, latestNursing, ongoingNursing, now, recentEvents)

                        if (!feedState.isNursingNow && feedState.alert10MinTimestamp != null) {
                            if (!feedState.isOptionalNightFeed || profile.notifyForOptionalNightFeeds) {
                                BabyAlarmScheduler.scheduleFeedingAlert(context, feedState.alert10MinTimestamp)
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
