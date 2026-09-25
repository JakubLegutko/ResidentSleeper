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

    @Test
    fun calculateOptimalWakeWindow_withNoHistory_returnsNormativeBaselineForAge() {
        val now = 1_000_000_000L

        // Newborn 2 weeks old -> 60 min midday baseline
        val p1 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(14))
        val res1 = WakeWindowCalculator.calculateOptimalWakeWindow(p1, emptyList(), now)
        assertThat(res1.usedHistoricalData).isFalse()
        assertThat(res1.recommendedWakeWindowMinutes).isEqualTo(60)
        assertThat(res1.observedMedianMinutes).isNull()
        assertThat(res1.sampleCount).isEqualTo(0)

        // 6 weeks old -> 80 min midday baseline
        val p2 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(42))
        val res2 = WakeWindowCalculator.calculateOptimalWakeWindow(p2, emptyList(), now)
        assertThat(res2.usedHistoricalData).isFalse()
        assertThat(res2.recommendedWakeWindowMinutes).isEqualTo(80)

        // 14 weeks old (~3.5 months) -> 120 min midday baseline
        val p3 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(98))
        val res3 = WakeWindowCalculator.calculateOptimalWakeWindow(p3, emptyList(), now)
        assertThat(res3.usedHistoricalData).isFalse()
        assertThat(res3.recommendedWakeWindowMinutes).isEqualTo(120)

        // 28 weeks old (~7 months) -> 165 min midday baseline
        val p4 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(196))
        val res4 = WakeWindowCalculator.calculateOptimalWakeWindow(p4, emptyList(), now)
        assertThat(res4.usedHistoricalData).isFalse()
        assertThat(res4.recommendedWakeWindowMinutes).isEqualTo(165)
    }

    @Test
    fun calculateOptimalWakeWindow_withHistoricalData_calculatesRollingMedianAccurately() {
        val now = 1_000_000_000L
        val birth = now - TimeUnit.DAYS.toMillis(14 * 7) // 14 weeks old (midday base = 120m, min 75, max 140)
        val profile = BabyProfile(birthTimestamp = birth)

        val t0 = now - TimeUnit.HOURS.toMillis(20)
        val t0End = t0 + TimeUnit.MINUTES.toMillis(60)

        // Wake 1: 110 min
        val t1 = t0End + TimeUnit.MINUTES.toMillis(110)
        val t1End = t1 + TimeUnit.MINUTES.toMillis(60)

        // Wake 2: 125 min
        val t2 = t1End + TimeUnit.MINUTES.toMillis(125)
        val t2End = t2 + TimeUnit.MINUTES.toMillis(60)

        // Wake 3: 120 min
        val t3 = t2End + TimeUnit.MINUTES.toMillis(120)
        val t3End = t3 + TimeUnit.MINUTES.toMillis(60)

        val sleeps = listOf(
            BabyEvent(type = EventType.SLEEP, startTime = t0, endTime = t0End),
            BabyEvent(type = EventType.SLEEP, startTime = t1, endTime = t1End),
            BabyEvent(type = EventType.SLEEP, startTime = t2, endTime = t2End),
            BabyEvent(type = EventType.SLEEP, startTime = t3, endTime = t3End)
        )

        val result = WakeWindowCalculator.calculateOptimalWakeWindow(profile, sleeps, now)

        assertThat(result.usedHistoricalData).isTrue()
        assertThat(result.sampleCount).isEqualTo(3)
        // Median of [110, 120, 125] is 120
        assertThat(result.observedMedianMinutes).isEqualTo(120)
        assertThat(result.recommendedWakeWindowMinutes).isEqualTo(120)
    }

    @Test
    fun calculateOptimalWakeWindow_filtersOutMicroBreaksAndLongOvernightGaps() {
        val now = 1_000_000_000L
        val birth = now - TimeUnit.DAYS.toMillis(14 * 7)
        val profile = BabyProfile(birthTimestamp = birth)

        val t0 = now - TimeUnit.HOURS.toMillis(30)
        val t0End = t0 + TimeUnit.MINUTES.toMillis(45)

        // Filtered out: micro break 15m (< 25m)
        val t1 = t0End + TimeUnit.MINUTES.toMillis(15)
        val t1End = t1 + TimeUnit.MINUTES.toMillis(45)

        // Valid 1: 100m
        val t2 = t1End + TimeUnit.MINUTES.toMillis(100)
        val t2End = t2 + TimeUnit.MINUTES.toMillis(45)

        // Filtered out: long overnight sleep gap 400m (> 360m)
        val t3 = t2End + TimeUnit.MINUTES.toMillis(400)
        val t3End = t3 + TimeUnit.MINUTES.toMillis(45)

        // Valid 2: 105m
        val t4 = t3End + TimeUnit.MINUTES.toMillis(105)
        val t4End = t4 + TimeUnit.MINUTES.toMillis(45)

        // Valid 3: 110m
        val t5 = t4End + TimeUnit.MINUTES.toMillis(110)
        val t5End = t5 + TimeUnit.MINUTES.toMillis(45)

        val sleeps = listOf(
            BabyEvent(type = EventType.SLEEP, startTime = t0, endTime = t0End),
            BabyEvent(type = EventType.SLEEP, startTime = t1, endTime = t1End),
            BabyEvent(type = EventType.SLEEP, startTime = t2, endTime = t2End),
            BabyEvent(type = EventType.SLEEP, startTime = t3, endTime = t3End),
            BabyEvent(type = EventType.SLEEP, startTime = t4, endTime = t4End),
            BabyEvent(type = EventType.SLEEP, startTime = t5, endTime = t5End)
        )

        val result = WakeWindowCalculator.calculateOptimalWakeWindow(profile, sleeps, now)

        assertThat(result.usedHistoricalData).isTrue()
        assertThat(result.sampleCount).isEqualTo(3)
        // Median of [100, 105, 110] is 105
        assertThat(result.observedMedianMinutes).isEqualTo(105)
        assertThat(result.recommendedWakeWindowMinutes).isEqualTo(105)
    }

    @Test
    fun calculateOptimalWakeWindow_clampsMedianToPediatricAgeBounds() {
        val now = 1_000_000_000L
        // 6 weeks old: min 60, max 90
        val birth = now - TimeUnit.DAYS.toMillis(42)
        val profile = BabyProfile(birthTimestamp = birth)

        val t0 = now - TimeUnit.HOURS.toMillis(20)
        val t0End = t0 + TimeUnit.MINUTES.toMillis(45)

        // Very long wake windows for 6 weeks: 140m, 150m, 160m
        val t1 = t0End + TimeUnit.MINUTES.toMillis(140)
        val t1End = t1 + TimeUnit.MINUTES.toMillis(45)
        val t2 = t1End + TimeUnit.MINUTES.toMillis(150)
        val t2End = t2 + TimeUnit.MINUTES.toMillis(45)
        val t3 = t2End + TimeUnit.MINUTES.toMillis(160)
        val t3End = t3 + TimeUnit.MINUTES.toMillis(45)

        val sleeps = listOf(
            BabyEvent(type = EventType.SLEEP, startTime = t0, endTime = t0End),
            BabyEvent(type = EventType.SLEEP, startTime = t1, endTime = t1End),
            BabyEvent(type = EventType.SLEEP, startTime = t2, endTime = t2End),
            BabyEvent(type = EventType.SLEEP, startTime = t3, endTime = t3End)
        )

        val result = WakeWindowCalculator.calculateOptimalWakeWindow(profile, sleeps, now)

        assertThat(result.usedHistoricalData).isTrue()
        assertThat(result.observedMedianMinutes).isEqualTo(150)
        // Clamped to 90 min (maxWakeWindowMin for 5-8 weeks)
        assertThat(result.recommendedWakeWindowMinutes).isEqualTo(90)
    }

    @Test
    fun computeState_whenAutoModeWithHistory_shiftsDiurnalWindows() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 15, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        // 14 weeks old: morningWakeWindow = 105, midday = 120, min = 75, max = 140
        val birth = now - TimeUnit.DAYS.toMillis(14 * 7)
        val profile = BabyProfile(birthTimestamp = birth, customWakeWindowMinutes = null)

        val t0 = now - TimeUnit.HOURS.toMillis(20)
        val t0End = t0 + TimeUnit.MINUTES.toMillis(60)
        val t1 = t0End + TimeUnit.MINUTES.toMillis(130)
        val t1End = t1 + TimeUnit.MINUTES.toMillis(60)
        val t2 = t1End + TimeUnit.MINUTES.toMillis(130)
        val t2End = t2 + TimeUnit.MINUTES.toMillis(60)
        val t3 = t2End + TimeUnit.MINUTES.toMillis(130)
        val t3End = t3 + TimeUnit.MINUTES.toMillis(60)

        val weekEvents = listOf(
            BabyEvent(type = EventType.SLEEP, startTime = t0, endTime = t0End),
            BabyEvent(type = EventType.SLEEP, startTime = t1, endTime = t1End),
            BabyEvent(type = EventType.SLEEP, startTime = t2, endTime = t2End),
            BabyEvent(type = EventType.SLEEP, startTime = t3, endTime = t3End)
        )

        // 0 naps completed today at 8:30 AM -> nextCategory is MORNING_NAP
        // Median = 130 min, schedule midday = 120 min -> delta = +10 min
        // Base window for morning nap: 105 + 10 = 115 min
        val lastRestorativeNap = BabyEvent(
            type = EventType.SLEEP,
            startTime = now - TimeUnit.MINUTES.toMillis(150),
            endTime = now - TimeUnit.MINUTES.toMillis(30)
        )
        val state = WakeWindowCalculator.computeState(
            profile = profile,
            latestSleep = lastRestorativeNap,
            ongoingSleep = null,
            currentTime = now,
            dayEvents = emptyList(),
            recentWeekEvents = weekEvents
        )

        assertThat(state.isAutoWakeWindow).isTrue()
        assertThat(state.calculationResult?.usedHistoricalData).isTrue()
        assertThat(state.nextSleepCategory).isEqualTo(SleepCategory.MORNING_NAP)
        assertThat(state.baseWakeWindowMinutes).isEqualTo(115)
        assertThat(state.recommendedWakeWindowMinutes).isEqualTo(115)
    }

    @Test
    fun computeState_withEarlyBedtimeDeficit_advancesBedtimeWindow() {
        val cal = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 15, 18, 0, 0) // 6:00 PM (near bedtime)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        // 28 weeks old (7 months): target day sleep = 2.5h (150 min), target naps = 2
        // bedtime window: preBedtimeWakeWindowMin = 195 min
        val birth = now - TimeUnit.DAYS.toMillis(28 * 7)
        val profile = BabyProfile(birthTimestamp = birth, customWakeWindowMinutes = null)

        val wakeStart = now - TimeUnit.HOURS.toMillis(2)
        // Baby only had 1 short nap today of 40 minutes (deficit = 150 - 40 = 110m >= 45m)
        val completedNap = BabyEvent(
            type = EventType.SLEEP,
            startTime = wakeStart - TimeUnit.MINUTES.toMillis(40),
            endTime = wakeStart
        )

        val state = WakeWindowCalculator.computeState(
            profile = profile,
            latestSleep = completedNap,
            ongoingSleep = null,
            currentTime = now,
            dayEvents = listOf(completedNap)
        )

        assertThat(state.nextSleepCategory).isEqualTo(SleepCategory.BEDTIME)
        // Pre-bedtime window 195 min reduced by 25 min for early bedtime compensation = 170 min
        assertThat(state.recommendedWakeWindowMinutes).isEqualTo(170)
        assertThat(state.recommendationReason).contains("deficit")
    }
}
