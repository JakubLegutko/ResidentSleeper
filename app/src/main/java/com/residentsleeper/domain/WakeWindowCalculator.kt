package com.residentsleeper.domain

import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.EventType
import java.util.concurrent.TimeUnit

data class WakeWindowState(
    val isSleeping: Boolean,
    val babyAgeWeeks: Int,
    val recommendedWakeWindowMinutes: Int,
    val wakeStartTime: Long?,
    val wakeDurationMinutes: Long,
    val expectedWakeEndTime: Long?,
    val minutesUntilWakeEnd: Long?,
    val alert10MinTimestamp: Long?
)

object WakeWindowCalculator {

    /**
     * Determines wake window in minutes based on age in weeks according to pediatric guidelines:
     * 0-4 weeks: ~50 min (range 45-60)
     * 5-8 weeks: ~65 min (range 60-75)
     * 9-12 weeks: ~80 min (range 75-90)
     * 13-16 weeks: ~100 min (range 90-120)
     * 17+ weeks: ~120 min
     */
    fun calculateRecommendedWakeWindow(ageInWeeks: Int): Int {
        return when {
            ageInWeeks < 5 -> 50
            ageInWeeks < 9 -> 65
            ageInWeeks < 13 -> 80
            ageInWeeks < 17 -> 100
            else -> 120
        }
    }

    fun calculateAgeInWeeks(birthTimestamp: Long, currentTimestamp: Long = System.currentTimeMillis()): Int {
        val diffMillis = maxOf(0L, currentTimestamp - birthTimestamp)
        return (TimeUnit.MILLISECONDS.toDays(diffMillis) / 7).toInt()
    }

    fun computeState(
        profile: BabyProfile,
        latestSleep: BabyEvent?,
        ongoingSleep: BabyEvent?,
        currentTime: Long = System.currentTimeMillis()
    ): WakeWindowState {
        val ageWeeks = calculateAgeInWeeks(profile.birthTimestamp, currentTime)
        val recommendedMinutes = profile.customWakeWindowMinutes ?: calculateRecommendedWakeWindow(ageWeeks)

        if (ongoingSleep != null) {
            // Baby is currently sleeping
            return WakeWindowState(
                isSleeping = true,
                babyAgeWeeks = ageWeeks,
                recommendedWakeWindowMinutes = recommendedMinutes,
                wakeStartTime = null,
                wakeDurationMinutes = 0L,
                expectedWakeEndTime = null,
                minutesUntilWakeEnd = null,
                alert10MinTimestamp = null
            )
        }

        // Baby is awake
        // Wake start is either the end of the last sleep, or if none, fallback to latest event or currentTime
        val wakeStart = latestSleep?.endTime ?: latestSleep?.startTime ?: (currentTime - TimeUnit.MINUTES.toMillis(recommendedMinutes.toLong() / 2))
        val wakeDurationMillis = maxOf(0L, currentTime - wakeStart)
        val wakeDurationMinutes = TimeUnit.MILLISECONDS.toMinutes(wakeDurationMillis)

        val expectedWakeEnd = wakeStart + TimeUnit.MINUTES.toMillis(recommendedMinutes.toLong())
        val diffToWakeEnd = expectedWakeEnd - currentTime
        val minutesUntilWakeEnd = TimeUnit.MILLISECONDS.toMinutes(diffToWakeEnd)

        val alert10MinTimestamp = expectedWakeEnd - TimeUnit.MINUTES.toMillis(profile.notifyBeforeMinutes.toLong())

        return WakeWindowState(
            isSleeping = false,
            babyAgeWeeks = ageWeeks,
            recommendedWakeWindowMinutes = recommendedMinutes,
            wakeStartTime = wakeStart,
            wakeDurationMinutes = wakeDurationMinutes,
            expectedWakeEndTime = expectedWakeEnd,
            minutesUntilWakeEnd = minutesUntilWakeEnd,
            alert10MinTimestamp = alert10MinTimestamp
        )
    }
}
