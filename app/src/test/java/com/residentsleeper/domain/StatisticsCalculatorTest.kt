package com.residentsleeper.domain

import com.google.common.truth.Truth.assertThat
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class StatisticsCalculatorTest {

    @Test
    fun calculateDailySummary_aggregatesSleepFeedingAndDiapersAccurately() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis

        // Event 1: Day nap from 10:00 to 11:30 (90 min)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        val nap1Start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 30)
        val nap1End = cal.timeInMillis

        // Event 2: Day nap from 13:00 to 14:00 (60 min) -> wake window between nap1 and nap2 is 90 min (11:30 to 13:00)
        cal.set(Calendar.HOUR_OF_DAY, 13)
        cal.set(Calendar.MINUTE, 0)
        val nap2Start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 14)
        val nap2End = cal.timeInMillis

        // Event 3: Nursing (Bottle 120ml) from 11:35 to 11:55 (20 min)
        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 35)
        val feed1Start = cal.timeInMillis
        cal.set(Calendar.MINUTE, 55)
        val feed1End = cal.timeInMillis

        // Event 4: Diaper Pee
        val diaper1 = BabyEvent(type = EventType.DIAPER, startTime = feed1Start, diaperType = DiaperType.PEE)
        // Event 5: Diaper Poo
        val diaper2 = BabyEvent(type = EventType.DIAPER, startTime = nap2End, diaperType = DiaperType.POO)

        val events = listOf(
            BabyEvent(type = EventType.SLEEP, startTime = nap1Start, endTime = nap1End),
            BabyEvent(type = EventType.SLEEP, startTime = nap2Start, endTime = nap2End),
            BabyEvent(type = EventType.NURSING, startTime = feed1Start, endTime = feed1End, nursingType = NursingType.BOTTLE, amountMl = 120),
            diaper1,
            diaper2
        )

        val summary = StatisticsCalculator.calculateDailySummary(dayStart, events)

        assertThat(summary.totalSleepMinutes).isEqualTo(150L) // 90 + 60
        assertThat(summary.napCount).isEqualTo(2)
        assertThat(summary.feedingCount).isEqualTo(1)
        assertThat(summary.totalBottleMl).isEqualTo(120)
        assertThat(summary.totalNursingDurationMinutes).isEqualTo(20L)
        assertThat(summary.diaperPeeCount).isEqualTo(1)
        assertThat(summary.diaperPooCount).isEqualTo(1)
        assertThat(summary.averageWakeWindowMinutes).isEqualTo(90L) // 11:30 to 13:00
    }

    @Test
    fun aggregateSummaries_calculatesDailyAveragesCorrectly() {
        val s1 = DailySummary(
            dateEpochMillis = 1000L,
            totalSleepMinutes = 800L,
            daySleepMinutes = 300L,
            nightSleepMinutes = 500L,
            napCount = 4,
            feedingCount = 8,
            totalNursingDurationMinutes = 160L,
            totalBottleMl = 100,
            diaperPeeCount = 5,
            diaperPooCount = 3,
            averageWakeWindowMinutes = 60L
        )

        val s2 = DailySummary(
            dateEpochMillis = 2000L,
            totalSleepMinutes = 900L,
            daySleepMinutes = 350L,
            nightSleepMinutes = 550L,
            napCount = 4,
            feedingCount = 8,
            totalNursingDurationMinutes = 180L,
            totalBottleMl = 200,
            diaperPeeCount = 7,
            diaperPooCount = 3,
            averageWakeWindowMinutes = 70L
        )

        val agg = StatisticsCalculator.aggregateSummaries("2-Day Average", listOf(s1, s2))

        assertThat(agg.daysCount).isEqualTo(2)
        assertThat(agg.avgTotalSleepMinutesPerDay).isEqualTo(850L) // (800+900)/2
        assertThat(agg.avgDaySleepMinutesPerDay).isEqualTo(325L)
        assertThat(agg.avgNightSleepMinutesPerDay).isEqualTo(525L)
        assertThat(agg.avgFeedingsPerDay).isEqualTo(8.0f)
        assertThat(agg.avgBottleMlPerDay).isEqualTo(150.0f) // (100+200)/2
        assertThat(agg.avgDiaperPeePerDay).isEqualTo(6.0f) // (5+7)/2
        assertThat(agg.avgDiaperPooPerDay).isEqualTo(3.0f)
        assertThat(agg.avgWakeWindowMinutes).isEqualTo(65L) // (60+70)/2
    }
}
