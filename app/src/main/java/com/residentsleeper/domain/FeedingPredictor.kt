package com.residentsleeper.domain

import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.EventType
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class FeedingState(
    val isNursingNow: Boolean,
    val lastFeedStartTime: Long?,
    val minutesSinceLastFeedStart: Long?,
    val nextFeedEstimateTime: Long?,
    val minutesUntilNextFeed: Long?,
    val alert10MinTimestamp: Long?,
    val lastFeedAmountMl: Int? = null,
    val lastFeedTypeDescription: String? = null,
    val isNightTime: Boolean = false,
    val nightFeedsCountTonight: Int = 0,
    val isOptionalNightFeed: Boolean = false
)

object FeedingPredictor {

    fun getNightWindowStart(currentTime: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTime
            val hour = get(Calendar.HOUR_OF_DAY)
            if (hour < 7) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun computeState(
        profile: BabyProfile,
        latestNursing: BabyEvent?,
        ongoingNursing: BabyEvent?,
        currentTime: Long = System.currentTimeMillis(),
        recentEvents: List<BabyEvent> = emptyList()
    ): FeedingState {
        val isNight = StatisticsCalculator.isNightHour(currentTime)
        val nightStart = getNightWindowStart(currentTime)

        val nightFeedsCount = if (recentEvents.isNotEmpty()) {
            recentEvents.count { event ->
                event.type == EventType.NURSING &&
                event.startTime >= nightStart &&
                event.startTime <= currentTime
            }
        } else {
            if (latestNursing != null && latestNursing.startTime >= nightStart && latestNursing.startTime <= currentTime) 1 else 0
        }

        val isOptional = isNight && (nightFeedsCount >= profile.maxRecommendedNightFeeds)

        if (ongoingNursing != null) {
            val durationMillis = maxOf(0L, currentTime - ongoingNursing.startTime)
            return FeedingState(
                isNursingNow = true,
                lastFeedStartTime = ongoingNursing.startTime,
                minutesSinceLastFeedStart = TimeUnit.MILLISECONDS.toMinutes(durationMillis),
                nextFeedEstimateTime = null,
                minutesUntilNextFeed = null,
                alert10MinTimestamp = null,
                lastFeedAmountMl = ongoingNursing.amountMl,
                lastFeedTypeDescription = ongoingNursing.nursingType?.name,
                isNightTime = isNight,
                nightFeedsCountTonight = nightFeedsCount,
                isOptionalNightFeed = isOptional
            )
        }

        if (latestNursing == null) {
            // No recorded nursing yet
            return FeedingState(
                isNursingNow = false,
                lastFeedStartTime = null,
                minutesSinceLastFeedStart = null,
                nextFeedEstimateTime = null,
                minutesUntilNextFeed = null,
                alert10MinTimestamp = null,
                isNightTime = isNight,
                nightFeedsCountTonight = nightFeedsCount,
                isOptionalNightFeed = isOptional
            )
        }

        // Interval calculated from the start of the previous feeding
        val feedStart = latestNursing.startTime
        val elapsedMillis = maxOf(0L, currentTime - feedStart)
        val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)

        val intervalMillis = TimeUnit.MINUTES.toMillis(profile.feedingIntervalMinutes.toLong())
        val nextFeedEstimateTime = feedStart + intervalMillis
        val diffToNextFeed = nextFeedEstimateTime - currentTime
        val minutesUntilNextFeed = TimeUnit.MILLISECONDS.toMinutes(diffToNextFeed)

        val alert10MinTimestamp = nextFeedEstimateTime - TimeUnit.MINUTES.toMillis(profile.notifyBeforeMinutes.toLong())

        return FeedingState(
            isNursingNow = false,
            lastFeedStartTime = feedStart,
            minutesSinceLastFeedStart = elapsedMinutes,
            nextFeedEstimateTime = nextFeedEstimateTime,
            minutesUntilNextFeed = minutesUntilNextFeed,
            alert10MinTimestamp = alert10MinTimestamp,
            lastFeedAmountMl = latestNursing.amountMl,
            lastFeedTypeDescription = latestNursing.nursingType?.name,
            isNightTime = isNight,
            nightFeedsCountTonight = nightFeedsCount,
            isOptionalNightFeed = isOptional
        )
    }
}
