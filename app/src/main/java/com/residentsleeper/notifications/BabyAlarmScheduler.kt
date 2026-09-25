package com.residentsleeper.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object BabyAlarmScheduler {

    const val EXTRA_ALERT_TYPE = "EXTRA_ALERT_TYPE"
    const val EXTRA_ALERT_TITLE = "EXTRA_ALERT_TITLE"
    const val EXTRA_ALERT_BODY = "EXTRA_ALERT_BODY"
    const val ALERT_TYPE_WAKE_WINDOW = "ALERT_TYPE_WAKE_WINDOW"
    const val ALERT_TYPE_FEEDING = "ALERT_TYPE_FEEDING"
    const val ALERT_TYPE_TEST = "ALERT_TYPE_TEST"

    private const val REQUEST_CODE_WAKE = 1001
    private const val REQUEST_CODE_FEED = 1002

    fun triggerTestAlert(context: Context, customTitle: String? = null, customBody: String? = null) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALERT_TYPE, ALERT_TYPE_TEST)
            if (customTitle != null) putExtra(EXTRA_ALERT_TITLE, customTitle)
            if (customBody != null) putExtra(EXTRA_ALERT_BODY, customBody)
        }
        context.sendBroadcast(intent)
    }

    fun scheduleWakeWindowAlert(
        context: Context,
        triggerTimeMillis: Long,
        customTitle: String? = null,
        customBody: String? = null
    ) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return
        scheduleAlarm(context, triggerTimeMillis, ALERT_TYPE_WAKE_WINDOW, REQUEST_CODE_WAKE, customTitle, customBody)
    }

    fun cancelWakeWindowAlert(context: Context) {
        cancelAlarm(context, ALERT_TYPE_WAKE_WINDOW, REQUEST_CODE_WAKE)
    }

    fun scheduleFeedingAlert(context: Context, triggerTimeMillis: Long) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return
        scheduleAlarm(context, triggerTimeMillis, ALERT_TYPE_FEEDING, REQUEST_CODE_FEED)
    }

    fun cancelFeedingAlert(context: Context) {
        cancelAlarm(context, ALERT_TYPE_FEEDING, REQUEST_CODE_FEED)
    }

    private fun scheduleAlarm(
        context: Context,
        triggerTimeMillis: Long,
        alertType: String,
        requestCode: Int,
        customTitle: String? = null,
        customBody: String? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALERT_TYPE, alertType)
            if (customTitle != null) putExtra(EXTRA_ALERT_TITLE, customTitle)
            if (customBody != null) putExtra(EXTRA_ALERT_BODY, customBody)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(context: Context, alertType: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALERT_TYPE, alertType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
