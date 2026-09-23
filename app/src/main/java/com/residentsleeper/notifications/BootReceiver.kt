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
                    val profile = db.babyProfileDao().getProfile()
                    if (profile != null && profile.enablePushNotifications) {
                        val latestSleep = db.babyEventDao().getLatestEvent(EventType.SLEEP)
                        val ongoingSleep = db.babyEventDao().getOngoingEvent(EventType.SLEEP)
                        val wakeState = WakeWindowCalculator.computeState(profile, latestSleep, ongoingSleep)

                        if (!wakeState.isSleeping && wakeState.alert10MinTimestamp != null) {
                            BabyAlarmScheduler.scheduleWakeWindowAlert(context, wakeState.alert10MinTimestamp)
                        }

                        val latestNursing = db.babyEventDao().getLatestEvent(EventType.NURSING)
                        val ongoingNursing = db.babyEventDao().getOngoingEvent(EventType.NURSING)
                        val feedState = FeedingPredictor.computeState(profile, latestNursing, ongoingNursing)

                        if (!feedState.isNursingNow && feedState.alert10MinTimestamp != null) {
                            BabyAlarmScheduler.scheduleFeedingAlert(context, feedState.alert10MinTimestamp)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
