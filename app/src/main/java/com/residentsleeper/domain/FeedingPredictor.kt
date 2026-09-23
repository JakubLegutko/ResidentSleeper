package com.residentsleeper.domain

import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import java.util.concurrent.TimeUnit

data class FeedingState(
    val isNursingNow: Boolean,
    val lastFeedStartTime: Long?,
    val minutesSinceLastFeedStart: Long?,
    val nextFeedEstimateTime: Long?,
    val minutesUntilNextFeed: Long?,
    val alert10MinTimestamp: Long?,
    val lastFeedAmountMl: Int? = null,
    val lastFeedTypeDescription: String? = null
)

object FeedingPredictor {

    fun computeState(
        profile: BabyProfile,
        latestNursing: BabyEvent?,
        ongoingNursing: BabyEvent?,
        currentTime: Long = System.currentTimeMillis()
    ): FeedingState {
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
                lastFeedTypeDescription = ongoingNursing.nursingType?.name
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
                alert10MinTimestamp = null
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
            lastFeedTypeDescription = latestNursing.nursingType?.name
        )
    }
}
