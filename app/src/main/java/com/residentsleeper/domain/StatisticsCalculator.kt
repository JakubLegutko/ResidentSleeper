package com.residentsleeper.domain

import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class DailySummary(
    val dateEpochMillis: Long,
    val totalSleepMinutes: Long,
    val daySleepMinutes: Long,
    val nightSleepMinutes: Long,
    val napCount: Int,
    val feedingCount: Int,
    val totalNursingDurationMinutes: Long,
    val totalBottleMl: Int,
    val diaperPeeCount: Int,
    val diaperPooCount: Int,
    val averageWakeWindowMinutes: Long
)

data class AggregatedReview(
    val title: String,
    val daysCount: Int,
    val avgTotalSleepMinutesPerDay: Long,
    val avgDaySleepMinutesPerDay: Long,
    val avgNightSleepMinutesPerDay: Long,
    val avgNapsPerDay: Float,
    val avgFeedingsPerDay: Float,
    val avgBottleMlPerDay: Float,
    val avgDiaperPeePerDay: Float,
    val avgDiaperPooPerDay: Float,
    val avgWakeWindowMinutes: Long,
    val dailySummaries: List<DailySummary>
)

object StatisticsCalculator {

    /**
     * Determines whether a timestamp falls into night sleep hours (19:00 - 07:00).
     */
    fun isNightHour(timestamp: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= 19 || hour < 7
    }

    /**
     * Calculates the daily summary for a list of events belonging to a 24-hour day.
     */
    fun calculateDailySummary(dayStartMillis: Long, events: List<BabyEvent>): DailySummary {
        var totalSleepMillis = 0L
        var daySleepMillis = 0L
        var nightSleepMillis = 0L
        var napCount = 0

        var feedingCount = 0
        var totalNursingMillis = 0L
        var totalBottleMl = 0

        var peeCount = 0
        var pooCount = 0

        val sleepEvents = mutableListOf<BabyEvent>()

        for (event in events) {
            when (event.type) {
                EventType.SLEEP -> {
                    napCount++
                    sleepEvents.add(event)
                    val end = event.endTime ?: System.currentTimeMillis()
                    val dur = maxOf(0L, end - event.startTime)
                    totalSleepMillis += dur

                    if (isNightHour(event.startTime)) {
                        nightSleepMillis += dur
                    } else {
                        daySleepMillis += dur
                    }
                }
                EventType.NURSING -> {
                    feedingCount++
                    val end = event.endTime ?: event.startTime
                    totalNursingMillis += maxOf(0L, end - event.startTime)
                    if (event.amountMl != null) {
                        totalBottleMl += event.amountMl
                    }
                }
                EventType.DIAPER -> {
                    when (event.diaperType) {
                        DiaperType.PEE -> peeCount++
                        DiaperType.POO -> pooCount++
                        DiaperType.BOTH -> {
                            peeCount++
                            pooCount++
                        }
                        null -> peeCount++
                    }
                }
            }
        }

        // Calculate wake windows between consecutive sleep events
        var totalWakeMillis = 0L
        var wakeCount = 0
        val sortedSleep = sleepEvents.sortedBy { it.startTime }
        for (i in 0 until sortedSleep.size - 1) {
            val currentSleepEnd = sortedSleep[i].endTime ?: sortedSleep[i].startTime
            val nextSleepStart = sortedSleep[i + 1].startTime
            if (nextSleepStart > currentSleepEnd) {
                totalWakeMillis += (nextSleepStart - currentSleepEnd)
                wakeCount++
            }
        }

        val avgWakeMinutes = if (wakeCount > 0) {
            TimeUnit.MILLISECONDS.toMinutes(totalWakeMillis / wakeCount)
        } else 0L

        return DailySummary(
            dateEpochMillis = dayStartMillis,
            totalSleepMinutes = TimeUnit.MILLISECONDS.toMinutes(totalSleepMillis),
            daySleepMinutes = TimeUnit.MILLISECONDS.toMinutes(daySleepMillis),
            nightSleepMinutes = TimeUnit.MILLISECONDS.toMinutes(nightSleepMillis),
            napCount = napCount,
            feedingCount = feedingCount,
            totalNursingDurationMinutes = TimeUnit.MILLISECONDS.toMinutes(totalNursingMillis),
            totalBottleMl = totalBottleMl,
            diaperPeeCount = peeCount,
            diaperPooCount = pooCount,
            averageWakeWindowMinutes = avgWakeMinutes
        )
    }

    /**
     * Aggregates a list of DailySummaries (e.g. 7 days for weekly, 30 days for monthly).
     */
    fun aggregateSummaries(title: String, summaries: List<DailySummary>): AggregatedReview {
        if (summaries.isEmpty()) {
            return AggregatedReview(
                title = title,
                daysCount = 0,
                avgTotalSleepMinutesPerDay = 0L,
                avgDaySleepMinutesPerDay = 0L,
                avgNightSleepMinutesPerDay = 0L,
                avgNapsPerDay = 0f,
                avgFeedingsPerDay = 0f,
                avgBottleMlPerDay = 0f,
                avgDiaperPeePerDay = 0f,
                avgDiaperPooPerDay = 0f,
                avgWakeWindowMinutes = 0L,
                dailySummaries = emptyList()
            )
        }

        // Exclude days with missing data from denominators so averages reflect actual logged days
        val daysWithSleep = summaries.filter { it.totalSleepMinutes > 0 }
        val sleepCount = daysWithSleep.size.toFloat()
        val avgTotalSleep = if (sleepCount > 0) (daysWithSleep.sumOf { it.totalSleepMinutes } / sleepCount).toLong() else 0L
        val avgDaySleep = if (sleepCount > 0) (daysWithSleep.sumOf { it.daySleepMinutes } / sleepCount).toLong() else 0L
        val avgNightSleep = if (sleepCount > 0) (daysWithSleep.sumOf { it.nightSleepMinutes } / sleepCount).toLong() else 0L
        val avgNaps = if (sleepCount > 0) daysWithSleep.sumOf { it.napCount } / sleepCount else 0f

        val daysWithFeeds = summaries.filter { it.feedingCount > 0 || it.totalBottleMl > 0 }
        val feedCount = daysWithFeeds.size.toFloat()
        val avgFeeds = if (feedCount > 0) daysWithFeeds.sumOf { it.feedingCount } / feedCount else 0f
        val daysWithBottle = summaries.filter { it.totalBottleMl > 0 }
        val avgBottle = if (daysWithBottle.isNotEmpty()) daysWithBottle.sumOf { it.totalBottleMl } / daysWithBottle.size.toFloat() else 0f

        val daysWithDiapers = summaries.filter { (it.diaperPeeCount + it.diaperPooCount) > 0 }
        val diaperCount = daysWithDiapers.size.toFloat()
        val avgPee = if (diaperCount > 0) daysWithDiapers.sumOf { it.diaperPeeCount } / diaperCount else 0f
        val avgPoo = if (diaperCount > 0) daysWithDiapers.sumOf { it.diaperPooCount } / diaperCount else 0f

        val wakeWins = summaries.filter { it.averageWakeWindowMinutes > 0 }
        val avgWake = if (wakeWins.isNotEmpty()) {
            wakeWins.sumOf { it.averageWakeWindowMinutes } / wakeWins.size
        } else 0L

        return AggregatedReview(
            title = title,
            daysCount = summaries.size,
            avgTotalSleepMinutesPerDay = avgTotalSleep,
            avgDaySleepMinutesPerDay = avgDaySleep,
            avgNightSleepMinutesPerDay = avgNightSleep,
            avgNapsPerDay = avgNaps,
            avgFeedingsPerDay = avgFeeds,
            avgBottleMlPerDay = avgBottle,
            avgDiaperPeePerDay = avgPee,
            avgDiaperPooPerDay = avgPoo,
            avgWakeWindowMinutes = avgWake,
            dailySummaries = summaries
        )
    }
}
