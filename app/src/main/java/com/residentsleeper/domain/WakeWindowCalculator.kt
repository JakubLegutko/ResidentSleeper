package com.residentsleeper.domain

import android.content.Context
import com.residentsleeper.R
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.EventType
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class WakeWindowState(
    val isSleeping: Boolean,
    val babyAgeWeeks: Int,
    val recommendedWakeWindowMinutes: Int,
    val baseWakeWindowMinutes: Int,
    val wakeStartTime: Long?,
    val wakeDurationMinutes: Long,
    val expectedWakeEndTime: Long?,
    val minutesUntilWakeEnd: Long?,
    val alert10MinTimestamp: Long?,
    val nextSleepCategory: SleepCategory,
    val recommendedSleepDuration: String,
    val recommendationTitle: String,
    val recommendationReason: String,
    val triviaTip: String,
    val napsCompletedToday: Int,
    val targetNapsToday: Int,
    val daySleepAccumulatedMinutes: Long
)

object WakeWindowCalculator {

    fun calculateAgeInWeeks(birthTimestamp: Long, currentTimestamp: Long = System.currentTimeMillis()): Int {
        val diffMillis = maxOf(0L, currentTimestamp - birthTimestamp)
        return (TimeUnit.MILLISECONDS.toDays(diffMillis) / 7).toInt()
    }

    /**
     * Backward-compatible helper for basic age-based wake window calculation.
     */
    fun calculateRecommendedWakeWindow(ageInWeeks: Int): Int {
        val schedule = LittleOnesSleepScheduleDatabase.getScheduleForAge(ageInWeeks)
        return schedule.middayWakeWindowMin
    }

    /**
     * Computes the complete dynamic, context-aware wake window state using Little Ones pediatric schedules.
     */
    fun computeState(
        profile: BabyProfile,
        latestSleep: BabyEvent?,
        ongoingSleep: BabyEvent?,
        currentTime: Long = System.currentTimeMillis(),
        dayEvents: List<BabyEvent> = emptyList(),
        context: Context? = null
    ): WakeWindowState {
        val ageWeeks = calculateAgeInWeeks(profile.birthTimestamp, currentTime)
        val schedule = LittleOnesSleepScheduleDatabase.getScheduleForAge(ageWeeks)
        val trivia = schedule.triviaTips.firstOrNull() ?: LittleOnesSleepScheduleDatabase.getRandomTriviaForAge(ageWeeks)

        // Completed naps today
        val completedSleepsToday = dayEvents.filter {
            it.type == EventType.SLEEP && it.endTime != null && it != ongoingSleep
        }
        val napsCount = completedSleepsToday.size
        val daySleepAccumulatedMillis = completedSleepsToday.sumOf {
            maxOf(0L, (it.endTime ?: it.startTime) - it.startTime)
        }
        val daySleepMinutes = TimeUnit.MILLISECONDS.toMinutes(daySleepAccumulatedMillis)

        // Time of day in minutes
        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinutesOfDay = hour * 60 + minute
        val bedtimeStartMinutes = schedule.bedtimeStartHour * 60 + schedule.bedtimeStartMinute

        // Determine next sleep category
        val nextCategory = when {
            napsCount >= schedule.targetNapsCount && currentMinutesOfDay >= (16 * 60) -> {
                SleepCategory.BEDTIME
            }
            currentMinutesOfDay >= (bedtimeStartMinutes - 60) -> {
                SleepCategory.BEDTIME
            }
            napsCount == 0 -> {
                SleepCategory.MORNING_NAP
            }
            napsCount == 1 -> {
                if (schedule.targetNapsCount == 1) SleepCategory.BEDTIME else SleepCategory.MIDDAY_NAP
            }
            napsCount == 2 -> {
                if (schedule.targetNapsCount >= 3) SleepCategory.BRIDGE_CATNAP else SleepCategory.BEDTIME
            }
            else -> {
                if (currentMinutesOfDay >= 16 * 60 + 30) SleepCategory.BEDTIME else SleepCategory.BRIDGE_CATNAP
            }
        }

        // Base wake window for this category
        val baseWindow = if (profile.customWakeWindowMinutes != null && profile.customWakeWindowMinutes > 0) {
            profile.customWakeWindowMinutes
        } else {
            when (nextCategory) {
                SleepCategory.MORNING_NAP -> schedule.morningWakeWindowMin
                SleepCategory.MIDDAY_NAP -> schedule.middayWakeWindowMin
                SleepCategory.BRIDGE_CATNAP -> schedule.afternoonWakeWindowMin
                SleepCategory.BEDTIME -> schedule.preBedtimeWakeWindowMin
            }
        }

        // Dynamic adjustment based on last nap duration and daily sleep pressure
        var adjustedWindow = baseWindow
        var reason = ""

        val lastNapDurationMinutes = latestSleep?.let {
            val end = it.endTime ?: it.startTime
            TimeUnit.MILLISECONDS.toMinutes(maxOf(0L, end - it.startTime))
        } ?: 0L

        if (profile.customWakeWindowMinutes == null || profile.customWakeWindowMinutes == 0) {
            if (nextCategory == SleepCategory.BEDTIME) {
                val bedStart = String.format("%02d:%02d", schedule.bedtimeStartHour, schedule.bedtimeStartMinute)
                val bedEnd = String.format("%02d:%02d", schedule.bedtimeEndHour, schedule.bedtimeEndMinute)
                reason = if (context != null) {
                    context.getString(R.string.rec_reason_bedtime, bedStart, bedEnd)
                } else {
                    "Bedtime window approaching ($bedStart–$bedEnd). Longer wake window builds overnight sleep pressure."
                }
            } else if (lastNapDurationMinutes in 1..39) {
                // Short nap penalty: reduce window by 15-20% to prevent cortisol surge
                val reduction = (baseWindow * 0.18f).toInt()
                adjustedWindow = maxOf(35, baseWindow - reduction)
                reason = if (context != null) {
                    context.getString(R.string.rec_reason_short_nap, lastNapDurationMinutes, reduction)
                } else {
                    "Last nap was short (${lastNapDurationMinutes}m). Wake window shortened by ${reduction}m to prevent overtiredness."
                }
            } else if (lastNapDurationMinutes >= 90) {
                // Restorative nap: full window supported
                reason = if (context != null) {
                    context.getString(R.string.rec_reason_restorative_nap, lastNapDurationMinutes)
                } else {
                    "Last nap was restorative (${lastNapDurationMinutes}m). Full age-appropriate wake window supported."
                }
            } else {
                reason = if (context != null) {
                    when (nextCategory) {
                        SleepCategory.MORNING_NAP -> context.getString(R.string.rec_reason_morning)
                        SleepCategory.MIDDAY_NAP -> context.getString(R.string.rec_reason_midday)
                        SleepCategory.BRIDGE_CATNAP -> context.getString(R.string.rec_reason_bridge)
                        SleepCategory.BEDTIME -> context.getString(R.string.rec_reason_bedtime_final)
                    }
                } else {
                    when (nextCategory) {
                        SleepCategory.MORNING_NAP -> "Morning wake window is naturally shorter as circadian alertness ramps up."
                        SleepCategory.MIDDAY_NAP -> "Midday sleep window builds pressure for the core restorative nap."
                        SleepCategory.BRIDGE_CATNAP -> "Bridge catnap to prevent overtiredness before evening bedtime."
                        SleepCategory.BEDTIME -> "Final wake window before night sleep."
                    }
                }
            }
        } else {
            reason = if (context != null) {
                context.getString(R.string.rec_reason_custom_window, profile.customWakeWindowMinutes)
            } else {
                "Using custom wake window setting (${profile.customWakeWindowMinutes}m)."
            }
        }

        // Recommended sleep duration text
        val recDurationText = if (context != null) {
            when (nextCategory) {
                SleepCategory.MORNING_NAP -> context.getString(R.string.rec_duration_morning_nap, schedule.morningNapDurationMin)
                SleepCategory.MIDDAY_NAP -> {
                    val h = schedule.middayNapDurationMin / 60
                    val m = schedule.middayNapDurationMin % 60
                    if (m == 0) {
                        context.getString(R.string.rec_duration_midday_nap_hours, h)
                    } else {
                        context.getString(R.string.rec_duration_midday_nap_hours_mins, h, m)
                    }
                }
                SleepCategory.BRIDGE_CATNAP -> context.getString(R.string.rec_duration_bridge_catnap, schedule.catnapDurationMin)
                SleepCategory.BEDTIME -> context.getString(R.string.rec_duration_bedtime, schedule.totalNightSleepTargetHours.toInt())
            }
        } else {
            when (nextCategory) {
                SleepCategory.MORNING_NAP -> "${schedule.morningNapDurationMin} min (Morning Nap)"
                SleepCategory.MIDDAY_NAP -> {
                    val h = schedule.middayNapDurationMin / 60
                    val m = schedule.middayNapDurationMin % 60
                    if (m == 0) "$h hours (Core Restorative Nap)" else "${h}h ${m}m (Core Restorative Nap)"
                }
                SleepCategory.BRIDGE_CATNAP -> "${schedule.catnapDurationMin} min (Bridge Catnap — end by 5:00 PM)"
                SleepCategory.BEDTIME -> "${schedule.totalNightSleepTargetHours.toInt()} hours (Overnight Consolidated Sleep)"
            }
        }

        val recTitle = if (context != null) {
            when (nextCategory) {
                SleepCategory.MORNING_NAP -> context.getString(R.string.rec_title_morning_nap)
                SleepCategory.MIDDAY_NAP -> context.getString(R.string.rec_title_midday_nap)
                SleepCategory.BRIDGE_CATNAP -> context.getString(R.string.rec_title_bridge_catnap)
                SleepCategory.BEDTIME -> context.getString(R.string.rec_title_bedtime)
            }
        } else {
            when (nextCategory) {
                SleepCategory.MORNING_NAP -> "Next: Morning Nap"
                SleepCategory.MIDDAY_NAP -> "Next: Restorative Midday Nap"
                SleepCategory.BRIDGE_CATNAP -> "Next: Bridge Catnap"
                SleepCategory.BEDTIME -> "Next: Bedtime Ritual"
            }
        }

        if (ongoingSleep != null) {
            val sleepDurMillis = maxOf(0L, currentTime - ongoingSleep.startTime)
            val sleepDurMinutes = TimeUnit.MILLISECONDS.toMinutes(sleepDurMillis)
            val ongoingTitle = if (context != null) context.getString(R.string.rec_title_sleep_in_progress) else "Sleep In Progress"
            val ongoingReason = if (context != null) context.getString(R.string.rec_reason_sleep_target, recDurationText) else "Target duration: $recDurationText"
            return WakeWindowState(
                isSleeping = true,
                babyAgeWeeks = ageWeeks,
                recommendedWakeWindowMinutes = adjustedWindow,
                baseWakeWindowMinutes = baseWindow,
                wakeStartTime = null,
                wakeDurationMinutes = sleepDurMinutes,
                expectedWakeEndTime = null,
                minutesUntilWakeEnd = null,
                alert10MinTimestamp = null,
                nextSleepCategory = nextCategory,
                recommendedSleepDuration = recDurationText,
                recommendationTitle = ongoingTitle,
                recommendationReason = ongoingReason,
                triviaTip = trivia,
                napsCompletedToday = napsCount,
                targetNapsToday = schedule.targetNapsCount,
                daySleepAccumulatedMinutes = daySleepMinutes
            )
        }

        // Baby is awake
        val wakeStart = latestSleep?.endTime ?: latestSleep?.startTime ?: (currentTime - TimeUnit.MINUTES.toMillis(adjustedWindow.toLong() / 2))
        val wakeDurationMillis = maxOf(0L, currentTime - wakeStart)
        val wakeDurationMinutes = TimeUnit.MILLISECONDS.toMinutes(wakeDurationMillis)

        val expectedWakeEnd = wakeStart + TimeUnit.MINUTES.toMillis(adjustedWindow.toLong())
        val diffToWakeEnd = expectedWakeEnd - currentTime
        val minutesUntilWakeEnd = TimeUnit.MILLISECONDS.toMinutes(diffToWakeEnd)

        val notifyBefore = profile.notifyBeforeMinutes.takeIf { it > 0 } ?: 10
        val alert10MinTimestamp = expectedWakeEnd - TimeUnit.MINUTES.toMillis(notifyBefore.toLong())

        return WakeWindowState(
            isSleeping = false,
            babyAgeWeeks = ageWeeks,
            recommendedWakeWindowMinutes = adjustedWindow,
            baseWakeWindowMinutes = baseWindow,
            wakeStartTime = wakeStart,
            wakeDurationMinutes = wakeDurationMinutes,
            expectedWakeEndTime = expectedWakeEnd,
            minutesUntilWakeEnd = minutesUntilWakeEnd,
            alert10MinTimestamp = alert10MinTimestamp,
            nextSleepCategory = nextCategory,
            recommendedSleepDuration = recDurationText,
            recommendationTitle = recTitle,
            recommendationReason = reason,
            triviaTip = trivia,
            napsCompletedToday = napsCount,
            targetNapsToday = schedule.targetNapsCount,
            daySleepAccumulatedMinutes = daySleepMinutes
        )
    }
}
