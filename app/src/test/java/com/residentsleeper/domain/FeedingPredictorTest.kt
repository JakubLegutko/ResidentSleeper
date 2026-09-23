package com.residentsleeper.domain

import com.google.common.truth.Truth.assertThat
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import org.junit.Test
import java.util.concurrent.TimeUnit

class FeedingPredictorTest {

    @Test
    fun computeState_whenNursingIsOngoing_returnsNursingNow() {
        val now = 1_000_000_000L
        val profile = BabyProfile()

        val ongoingNursing = BabyEvent(
            type = EventType.NURSING,
            startTime = now - TimeUnit.MINUTES.toMillis(15),
            endTime = null,
            nursingType = NursingType.LEFT_BREAST
        )

        val state = FeedingPredictor.computeState(profile, null, ongoingNursing, now)

        assertThat(state.isNursingNow).isTrue()
        assertThat(state.minutesSinceLastFeedStart).isEqualTo(15L)
        assertThat(state.nextFeedEstimateTime).isNull()
        assertThat(state.lastFeedTypeDescription).isEqualTo("LEFT_BREAST")
    }

    @Test
    fun computeState_calculatesNextFeedFromStartOfPreviousFeed() {
        val now = 2_000_000_000L
        val profile = BabyProfile(
            feedingIntervalMinutes = 150, // 2.5 hours
            notifyBeforeMinutes = 10
        )

        // Previous feeding started 60 min ago, ended 40 min ago
        val feedStart = now - TimeUnit.MINUTES.toMillis(60)
        val feedEnd = now - TimeUnit.MINUTES.toMillis(40)
        val completedNursing = BabyEvent(
            type = EventType.NURSING,
            startTime = feedStart,
            endTime = feedEnd,
            nursingType = NursingType.BOTTLE,
            amountMl = 90
        )

        val state = FeedingPredictor.computeState(profile, completedNursing, null, now)

        assertThat(state.isNursingNow).isFalse()
        assertThat(state.minutesSinceLastFeedStart).isEqualTo(60L)
        assertThat(state.lastFeedAmountMl).isEqualTo(90)

        // Next feed is feedStart + 150 min -> in 90 min from now
        val expectedNextFeed = feedStart + TimeUnit.MINUTES.toMillis(150)
        assertThat(state.nextFeedEstimateTime).isEqualTo(expectedNextFeed)
        assertThat(state.minutesUntilNextFeed).isEqualTo(90L)

        // 10-minute warning alert
        val expectedAlert = expectedNextFeed - TimeUnit.MINUTES.toMillis(10)
        assertThat(state.alert10MinTimestamp).isEqualTo(expectedAlert)
    }

    @Test
    fun computeState_whenNoFeedingRecorded_returnsNullsSafely() {
        val state = FeedingPredictor.computeState(BabyProfile(), null, null)

        assertThat(state.isNursingNow).isFalse()
        assertThat(state.lastFeedStartTime).isNull()
        assertThat(state.nextFeedEstimateTime).isNull()
        assertThat(state.minutesUntilNextFeed).isNull()
    }
}
