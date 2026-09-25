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

    @Test
    fun calculateOptimalInterval_withNoHistory_returnsNormativeWHOIntervalForAge() {
        val now = 10_000_000_000L

        // 2 days old (1.–3. doba) -> 120 min
        val p1 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(2))
        val res1 = FeedingPredictor.calculateOptimalInterval(p1, emptyList(), now)
        assertThat(res1.recommendedIntervalMinutes).isEqualTo(120)
        assertThat(res1.ageBracket.ageBracketLabel).isEqualTo("1.–3. doba")
        assertThat(res1.usedHistoricalData).isFalse()

        // 7 days old (4.–14. doba) -> 150 min
        val p2 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(7))
        val res2 = FeedingPredictor.calculateOptimalInterval(p2, emptyList(), now)
        assertThat(res2.recommendedIntervalMinutes).isEqualTo(150)
        assertThat(res2.ageBracket.ageBracketLabel).isEqualTo("4.–14. doba")

        // 4 weeks old (2.–8. tydzień) -> 180 min
        val p3 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(28))
        val res3 = FeedingPredictor.calculateOptimalInterval(p3, emptyList(), now)
        assertThat(res3.recommendedIntervalMinutes).isEqualTo(180)
        assertThat(res3.ageBracket.ageBracketLabel).isEqualTo("2.–8. tydzień")

        // 3 months old (2.–4. miesiąc) -> 195 min
        val p4 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(90))
        val res4 = FeedingPredictor.calculateOptimalInterval(p4, emptyList(), now)
        assertThat(res4.recommendedIntervalMinutes).isEqualTo(195)
        assertThat(res4.ageBracket.ageBracketLabel).isEqualTo("2.–4. miesiąc")

        // 5.5 months old (5.–6. miesiąc) -> 225 min
        val p5 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(160))
        val res5 = FeedingPredictor.calculateOptimalInterval(p5, emptyList(), now)
        assertThat(res5.recommendedIntervalMinutes).isEqualTo(225)
        assertThat(res5.ageBracket.ageBracketLabel).isEqualTo("5.–6. miesiąc")

        // 8 months old (7.–9. miesiąc) -> 225 min
        val p6 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(240))
        val res6 = FeedingPredictor.calculateOptimalInterval(p6, emptyList(), now)
        assertThat(res6.recommendedIntervalMinutes).isEqualTo(225)
        assertThat(res6.ageBracket.ageBracketLabel).isEqualTo("7.–9. miesiąc")

        // 11 months old (10.–12. miesiąc) -> 240 min
        val p7 = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(330))
        val res7 = FeedingPredictor.calculateOptimalInterval(p7, emptyList(), now)
        assertThat(res7.recommendedIntervalMinutes).isEqualTo(240)
        assertThat(res7.ageBracket.ageBracketLabel).isEqualTo("10.–12. miesiąc")
    }

    @Test
    fun calculateOptimalInterval_withHistoricalData_calculatesRollingMedianAccurately() {
        val now = 10_000_000_000L
        // 5 weeks old: bracket is 150..210 min, default 180 min
        val profile = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(35))

        // Create 4 feedings with intervals: 160m, 170m, 165m
        // Median of [160, 165, 170] = 165 min
        val t0 = now - TimeUnit.HOURS.toMillis(10)
        val t1 = t0 + TimeUnit.MINUTES.toMillis(160)
        val t2 = t1 + TimeUnit.MINUTES.toMillis(170)
        val t3 = t2 + TimeUnit.MINUTES.toMillis(165)

        val feeds = listOf(
            BabyEvent(type = EventType.NURSING, startTime = t0),
            BabyEvent(type = EventType.NURSING, startTime = t1),
            BabyEvent(type = EventType.NURSING, startTime = t2),
            BabyEvent(type = EventType.NURSING, startTime = t3)
        )

        val result = FeedingPredictor.calculateOptimalInterval(profile, feeds, now)

        assertThat(result.usedHistoricalData).isTrue()
        assertThat(result.sampleCount).isEqualTo(3)
        assertThat(result.observedMedianMinutes).isEqualTo(165)
        assertThat(result.recommendedIntervalMinutes).isEqualTo(165)
    }

    @Test
    fun calculateOptimalInterval_filtersOutMicroSnacksAndOvernightSleepStretches() {
        val now = 10_000_000_000L
        val profile = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(35))

        val t0 = now - TimeUnit.HOURS.toMillis(20)
        val t1 = t0 + TimeUnit.MINUTES.toMillis(20)  // 20 min -> micro-snack / cluster feed (< 45m), ignored
        val t2 = t1 + TimeUnit.MINUTES.toMillis(160) // 160 min -> valid
        val t3 = t2 + TimeUnit.MINUTES.toMillis(420) // 420 min -> long overnight stretch (> 360m), ignored
        val t4 = t3 + TimeUnit.MINUTES.toMillis(170) // 170 min -> valid
        val t5 = t4 + TimeUnit.MINUTES.toMillis(160) // 160 min -> valid

        val feeds = listOf(
            BabyEvent(type = EventType.NURSING, startTime = t0),
            BabyEvent(type = EventType.NURSING, startTime = t1),
            BabyEvent(type = EventType.NURSING, startTime = t2),
            BabyEvent(type = EventType.NURSING, startTime = t3),
            BabyEvent(type = EventType.NURSING, startTime = t4),
            BabyEvent(type = EventType.NURSING, startTime = t5)
        )

        val result = FeedingPredictor.calculateOptimalInterval(profile, feeds, now)

        // Only intervals 160, 170, 160 should be counted
        assertThat(result.sampleCount).isEqualTo(3)
        assertThat(result.observedMedianMinutes).isEqualTo(160)
        assertThat(result.recommendedIntervalMinutes).isEqualTo(160)
    }

    @Test
    fun calculateOptimalInterval_clampsMedianToPediatricAgeBounds() {
        val now = 10_000_000_000L
        // 2 days old: bracket is 90..180 min
        val profile = BabyProfile(birthTimestamp = now - TimeUnit.DAYS.toMillis(2))

        // History shows unusually long intervals (e.g. 240 min, 250 min, 260 min)
        val t0 = now - TimeUnit.HOURS.toMillis(15)
        val t1 = t0 + TimeUnit.MINUTES.toMillis(240)
        val t2 = t1 + TimeUnit.MINUTES.toMillis(250)
        val t3 = t2 + TimeUnit.MINUTES.toMillis(260)

        val feeds = listOf(
            BabyEvent(type = EventType.NURSING, startTime = t0),
            BabyEvent(type = EventType.NURSING, startTime = t1),
            BabyEvent(type = EventType.NURSING, startTime = t2),
            BabyEvent(type = EventType.NURSING, startTime = t3)
        )

        val result = FeedingPredictor.calculateOptimalInterval(profile, feeds, now)

        // Observed median is 250 min, but for a 2-day-old newborn it MUST clamp to max 180 min
        assertThat(result.observedMedianMinutes).isEqualTo(250)
        assertThat(result.recommendedIntervalMinutes).isEqualTo(180)
    }
}
