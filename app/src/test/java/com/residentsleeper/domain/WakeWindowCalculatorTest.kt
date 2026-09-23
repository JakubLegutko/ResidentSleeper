package com.residentsleeper.domain

import com.google.common.truth.Truth.assertThat
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.EventType
import org.junit.Test
import java.util.concurrent.TimeUnit

class WakeWindowCalculatorTest {

    @Test
    fun calculateRecommendedWakeWindow_returnsCorrectWindowsForAge() {
        // 0-4 weeks -> 50 min
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(0)).isEqualTo(50)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(3)).isEqualTo(50)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(4)).isEqualTo(50)

        // 5-8 weeks -> 65 min
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(5)).isEqualTo(65)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(8)).isEqualTo(65)

        // 9-12 weeks -> 80 min
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(9)).isEqualTo(80)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(12)).isEqualTo(80)

        // 13-16 weeks -> 100 min
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(14)).isEqualTo(100)

        // 17+ weeks -> 120 min
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(20)).isEqualTo(120)
    }

    @Test
    fun computeState_whenBabyIsSleeping_returnsSleepingState() {
        val now = 1_000_000_000L
        val birth = now - TimeUnit.DAYS.toMillis(28) // 4 weeks old
        val profile = BabyProfile(birthTimestamp = birth)

        val ongoingSleep = BabyEvent(
            type = EventType.SLEEP,
            startTime = now - TimeUnit.MINUTES.toMillis(30),
            endTime = null
        )

        val state = WakeWindowCalculator.computeState(profile, null, ongoingSleep, now)

        assertThat(state.isSleeping).isTrue()
        assertThat(state.expectedWakeEndTime).isNull()
        assertThat(state.alert10MinTimestamp).isNull()
        assertThat(state.babyAgeWeeks).isEqualTo(4)
        assertThat(state.recommendedWakeWindowMinutes).isEqualTo(50)
    }

    @Test
    fun computeState_whenBabyIsAwake_calculatesRemainingWindowAndAlert() {
        val now = 2_000_000_000L
        val birth = now - TimeUnit.DAYS.toMillis(42) // 6 weeks old -> 65 min window
        val profile = BabyProfile(birthTimestamp = birth, notifyBeforeMinutes = 10)

        val wakeStart = now - TimeUnit.MINUTES.toMillis(25) // Awake for 25 min
        val lastSleep = BabyEvent(
            type = EventType.SLEEP,
            startTime = wakeStart - TimeUnit.HOURS.toMillis(1),
            endTime = wakeStart
        )

        val state = WakeWindowCalculator.computeState(profile, lastSleep, null, now)

        assertThat(state.isSleeping).isFalse()
        assertThat(state.wakeDurationMinutes).isEqualTo(25L)
        assertThat(state.recommendedWakeWindowMinutes).isEqualTo(65)

        // Expected end = wakeStart + 65 min
        val expectedEnd = wakeStart + TimeUnit.MINUTES.toMillis(65)
        assertThat(state.expectedWakeEndTime).isEqualTo(expectedEnd)

        // Remaining = 65 - 25 = 40 min
        assertThat(state.minutesUntilWakeEnd).isEqualTo(40L)

        // Alert is 10 min before expected end
        val expectedAlert = expectedEnd - TimeUnit.MINUTES.toMillis(10)
        assertThat(state.alert10MinTimestamp).isEqualTo(expectedAlert)
    }

    @Test
    fun computeState_withManualWakeWindowOverride_usesOverride() {
        val now = 3_000_000_000L
        val birth = now - TimeUnit.DAYS.toMillis(14)
        val profile = BabyProfile(
            birthTimestamp = birth,
            customWakeWindowMinutes = 75,
            notifyBeforeMinutes = 10
        )

        val wakeStart = now - TimeUnit.MINUTES.toMillis(30)
        val lastSleep = BabyEvent(
            type = EventType.SLEEP,
            startTime = wakeStart - TimeUnit.HOURS.toMillis(1),
            endTime = wakeStart
        )

        val state = WakeWindowCalculator.computeState(profile, lastSleep, null, now)

        assertThat(state.recommendedWakeWindowMinutes).isEqualTo(75)
        assertThat(state.minutesUntilWakeEnd).isEqualTo(45L) // 75 - 30 = 45 min
    }
}
