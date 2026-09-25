package com.residentsleeper.domain

import android.content.Context
import com.residentsleeper.R
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.EventType
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class WakeWindowCalculationResult(
    val recommendedWakeWindowMinutes: Int,
    val ageSchedule: AgeSleepSchedule,
    val ageWeeks: Int,
    val observedMedianMinutes: Int?,
    val sampleCount: Int,
    val usedHistoricalData: Boolean
)

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
    val daySleepAccumulatedMinutes: Long,
    val recommendedSleepDurationMinutes: Int = 60,
    val isAutoWakeWindow: Boolean = true,
    val calculationResult: WakeWindowCalculationResult? = null
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
     * Calculates the optimal wake window based on Little Ones pediatric clinical guidelines and
     * a rolling median of completed wake cycles from the last 7 days.
     */
    fun calculateOptimalWakeWindow(
        profile: BabyProfile,
        recentSleepEvents: List<BabyEvent>,
        currentTime: Long = System.currentTimeMillis()
    ): WakeWindowCalculationResult {
        val ageWeeks = calculateAgeInWeeks(profile.birthTimestamp, currentTime)
        val schedule = LittleOnesSleepScheduleDatabase.getScheduleForAge(ageWeeks)

        val sevenDaysAgo = currentTime - TimeUnit.DAYS.toMillis(7)
        val completedSleeps = recentSleepEvents
            .filter { it.type == EventType.SLEEP && it.startTime >= sevenDaysAgo && it.startTime <= currentTime && it.endTime != null }
            .sortedBy { it.startTime }

        val validWakeWindowsMinutes = mutableListOf<Int>()
        for (i in 0 until completedSleeps.size - 1) {
            val wakeStart = completedSleeps[i].endTime!!
            val nextSleepStart = completedSleeps[i + 1].startTime
            val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(nextSleepStart - wakeStart).toInt()
            // Filter realistic awake windows: exclude micro-breaks < 25m or overnight/missing data > 360m (6h)
            if (diffMinutes in 25..360) {
                validWakeWindowsMinutes.add(diffMinutes)
            }
        }

        if (validWakeWindowsMinutes.size >= 3) {
            val sortedIntervals = validWakeWindowsMinutes.sorted()
            val medianMinutes = if (sortedIntervals.size % 2 == 1) {
                sortedIntervals[sortedIntervals.size / 2]
            } else {
                (sortedIntervals[sortedIntervals.size / 2 - 1] + sortedIntervals[sortedIntervals.size / 2]) / 2
            }

            // Clamp to clinical pediatric bounds for safety
            val clamped = medianMinutes.coerceIn(schedule.minWakeWindowMin, schedule.maxWakeWindowMin)
            // Round to nearest 5 minutes
            val rounded = ((clamped + 2) / 5) * 5

            return WakeWindowCalculationResult(
                recommendedWakeWindowMinutes = rounded,
                ageSchedule = schedule,
                ageWeeks = ageWeeks,
                observedMedianMinutes = medianMinutes,
                sampleCount = validWakeWindowsMinutes.size,
                usedHistoricalData = true
            )
        }

        // Insufficient historical data: fall back to normative pediatric baseline for age
        return WakeWindowCalculationResult(
            recommendedWakeWindowMinutes = schedule.middayWakeWindowMin,
            ageSchedule = schedule,
            ageWeeks = ageWeeks,
            observedMedianMinutes = null,
            sampleCount = validWakeWindowsMinutes.size,
            usedHistoricalData = false
        )
    }

    /**
     * Computes the complete dynamic, context-aware wake window state using Little Ones pediatric schedules
     * and personalized historical sleep data.
     */
    fun computeState(
        profile: BabyProfile,
        latestSleep: BabyEvent?,
        ongoingSleep: BabyEvent?,
        currentTime: Long = System.currentTimeMillis(),
        dayEvents: List<BabyEvent> = emptyList(),
        context: Context? = null,
        recentWeekEvents: List<BabyEvent> = emptyList()
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

        val isAuto = profile.customWakeWindowMinutes == null || profile.customWakeWindowMinutes <= 0
        val calculationResult = if (isAuto) {
            calculateOptimalWakeWindow(profile, recentWeekEvents, currentTime)
        } else {
            null
        }

        // Base wake window for this category
        val baseWindow = if (!isAuto) {
            profile.customWakeWindowMinutes!!
        } else if (calculationResult != null && calculationResult.usedHistoricalData) {
            // Child's individual baseline shifts diurnal wake windows relative to schedule
            val delta = calculationResult.recommendedWakeWindowMinutes - schedule.middayWakeWindowMin
            when (nextCategory) {
                SleepCategory.MORNING_NAP -> (schedule.morningWakeWindowMin + delta).coerceIn(schedule.minWakeWindowMin, schedule.maxWakeWindowMin)
                SleepCategory.MIDDAY_NAP -> calculationResult.recommendedWakeWindowMinutes
                SleepCategory.BRIDGE_CATNAP -> (schedule.afternoonWakeWindowMin + delta).coerceIn(schedule.minWakeWindowMin, schedule.maxWakeWindowMin)
                SleepCategory.BEDTIME -> (schedule.preBedtimeWakeWindowMin + delta).coerceIn(schedule.minWakeWindowMin, schedule.maxWakeWindowMin)
            }
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

        if (isAuto) {
            // Early bedtime check: Borbély Process S sleep debt compensation
            // When daytime sleep deficit >= 45 minutes, bring bedtime forward by 25 min to prevent cortisol spike
            val targetDaySleepMinutes = (schedule.totalDaySleepTargetHours * 60).toInt()
            val sleepDeficitMinutes = targetDaySleepMinutes - daySleepMinutes.toInt()

            if (nextCategory == SleepCategory.BEDTIME && sleepDeficitMinutes >= 45) {
                val reduction = 25
                adjustedWindow = maxOf(35, baseWindow - reduction)
                val targetHoursStr = if (schedule.totalDaySleepTargetHours % 1f == 0f) {
                    schedule.totalDaySleepTargetHours.toInt().toString()
                } else {
                    schedule.totalDaySleepTargetHours.toString()
                }
                reason = if (context != null) {
                    context.getString(R.string.rec_reason_early_bedtime, sleepDeficitMinutes, targetHoursStr)
                } else {
                    "Daytime sleep deficit (${sleepDeficitMinutes}m vs ${targetHoursStr}h target). Earlier bedtime recommended to prevent overtiredness."
                }
            } else if (nextCategory == SleepCategory.BEDTIME) {
                val bedStart = String.format("%02d:%02d", schedule.bedtimeStartHour, schedule.bedtimeStartMinute)
                val bedEnd = String.format("%02d:%02d", schedule.bedtimeEndHour, schedule.bedtimeEndMinute)
                reason = if (context != null) {
                    context.getString(R.string.rec_reason_bedtime, bedStart, bedEnd)
                } else {
                    "Bedtime window approaching ($bedStart–$bedEnd). Longer wake window builds overnight sleep pressure."
                }
            } else if (lastNapDurationMinutes in 1..39) {
                // Short nap penalty: reduce window by 20% to prevent cortisol surge
                val reduction = (baseWindow * 0.20f).toInt()
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

        val recDurationMinutes = when (nextCategory) {
            SleepCategory.MORNING_NAP -> schedule.morningNapDurationMin
            SleepCategory.MIDDAY_NAP -> schedule.middayNapDurationMin
            SleepCategory.BRIDGE_CATNAP -> schedule.catnapDurationMin
            SleepCategory.BEDTIME -> (schedule.totalNightSleepTargetHours * 60).toInt()
        }.coerceAtLeast(15)

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
                daySleepAccumulatedMinutes = daySleepMinutes,
                recommendedSleepDurationMinutes = recDurationMinutes,
                isAutoWakeWindow = isAuto,
                calculationResult = calculationResult
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
            daySleepAccumulatedMinutes = daySleepMinutes,
            recommendedSleepDurationMinutes = recDurationMinutes,
            isAutoWakeWindow = isAuto,
            calculationResult = calculationResult
        )
    }
}
