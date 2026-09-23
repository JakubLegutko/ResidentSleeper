package com.residentsleeper.domain

import com.google.common.truth.Truth.assertThat
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.EventType
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WakeWindowCalculatorTest {

    @Test
    fun calculateRecommendedWakeWindow_returnsLittleOnesMiddayWindowsForAge() {
        // 0-4 weeks -> 60 min midday
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(0)).isEqualTo(60)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(3)).isEqualTo(60)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(4)).isEqualTo(60)

        // 5-8 weeks -> 80 min midday
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(5)).isEqualTo(80)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(8)).isEqualTo(80)

        // 9-12 weeks -> 100 min midday
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(9)).isEqualTo(100)
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(12)).isEqualTo(100)

        // 13-16 weeks (4 months) -> 120 min midday
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(14)).isEqualTo(120)

        // 17-21 weeks (5 months) -> 135 min midday
        assertThat(WakeWindowCalculator.calculateRecommendedWakeWindow(20)).isEqualTo(135)
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
        assertThat(state.recommendationTitle).isEqualTo("Sleep In Progress")
    }

    @Test
    fun computeState_withShortNap_tightensWakeWindow() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        val birth = now - TimeUnit.DAYS.toMillis(16 * 7) // 16 weeks old (4 months)
        val profile = BabyProfile(birthTimestamp = birth, notifyBeforeMinutes = 10)

        val wakeStart = now - TimeUnit.MINUTES.toMillis(20)
        // Last nap was only 25 minutes (short nap < 40m)
        val lastShortSleep = BabyEvent(
            type = EventType.SLEEP,
            startTime = wakeStart - TimeUnit.MINUTES.toMillis(25),
            endTime = wakeStart
        )

        val state = WakeWindowCalculator.computeState(profile, lastShortSleep, null, now)

        assertThat(state.isSleeping).isFalse()
        // Window should be reduced by ~18% from base window
        assertThat(state.recommendedWakeWindowMinutes).isLessThan(state.baseWakeWindowMinutes)
        assertThat(state.recommendationReason).contains("short")
    }

    @Test
    fun computeState_withManualWakeWindowOverride_usesOverride() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
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

    @Test
    fun littleOnesDatabase_hasTriviaForEveryAgeGroup() {
        for (weeks in listOf(2, 6, 10, 15, 19, 24, 28, 32, 36, 41, 45, 50)) {
            val sched = LittleOnesSleepScheduleDatabase.getScheduleForAge(weeks)
            assertThat(sched.triviaTips).isNotEmpty()
            val trivia = LittleOnesSleepScheduleDatabase.getRandomTriviaForAge(weeks)
            assertThat(trivia).isNotEmpty()
        }
    }

    @Test
    fun statisticsCalculator_excludesMissingDataFromAverages() {
        // 3 days with real sleep, 4 days missing (0 sleep)
        val summaries = listOf(
            DailySummary(100, totalSleepMinutes = 720, daySleepMinutes = 180, nightSleepMinutes = 540, napCount = 3, feedingCount = 6, totalNursingDurationMinutes = 120, totalBottleMl = 0, diaperPeeCount = 5, diaperPooCount = 2, averageWakeWindowMinutes = 90),
            DailySummary(200, totalSleepMinutes = 800, daySleepMinutes = 200, nightSleepMinutes = 600, napCount = 3, feedingCount = 6, totalNursingDurationMinutes = 130, totalBottleMl = 0, diaperPeeCount = 6, diaperPooCount = 2, averageWakeWindowMinutes = 95),
            DailySummary(300, totalSleepMinutes = 760, daySleepMinutes = 190, nightSleepMinutes = 570, napCount = 3, feedingCount = 6, totalNursingDurationMinutes = 125, totalBottleMl = 0, diaperPeeCount = 4, diaperPooCount = 2, averageWakeWindowMinutes = 85),
            // Missing days (0 sleep, 0 feeds, 0 diapers)
            DailySummary(400, totalSleepMinutes = 0, daySleepMinutes = 0, nightSleepMinutes = 0, napCount = 0, feedingCount = 0, totalNursingDurationMinutes = 0, totalBottleMl = 0, diaperPeeCount = 0, diaperPooCount = 0, averageWakeWindowMinutes = 0),
            DailySummary(500, totalSleepMinutes = 0, daySleepMinutes = 0, nightSleepMinutes = 0, napCount = 0, feedingCount = 0, totalNursingDurationMinutes = 0, totalBottleMl = 0, diaperPeeCount = 0, diaperPooCount = 0, averageWakeWindowMinutes = 0),
            DailySummary(600, totalSleepMinutes = 0, daySleepMinutes = 0, nightSleepMinutes = 0, napCount = 0, feedingCount = 0, totalNursingDurationMinutes = 0, totalBottleMl = 0, diaperPeeCount = 0, diaperPooCount = 0, averageWakeWindowMinutes = 0),
            DailySummary(700, totalSleepMinutes = 0, daySleepMinutes = 0, nightSleepMinutes = 0, napCount = 0, feedingCount = 0, totalNursingDurationMinutes = 0, totalBottleMl = 0, diaperPeeCount = 0, diaperPooCount = 0, averageWakeWindowMinutes = 0)
        )

        val aggregated = StatisticsCalculator.aggregateSummaries("Past 7 Days", summaries)

        // Average should be (720 + 800 + 760) / 3 = 760, NOT divided by 7 (which would have been 325)
        assertThat(aggregated.avgTotalSleepMinutesPerDay).isEqualTo(760L)
        assertThat(aggregated.avgNapsPerDay).isEqualTo(3.0f)
        assertThat(aggregated.avgFeedingsPerDay).isEqualTo(6.0f)
        assertThat(aggregated.avgDiaperPeePerDay).isEqualTo(5.0f)
        assertThat(aggregated.avgDiaperPooPerDay).isEqualTo(2.0f)
    }
}
