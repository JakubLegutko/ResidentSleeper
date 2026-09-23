package com.residentsleeper.ui.chart

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class Clock24HourChartMathTest {

    @Test
    fun minuteToAngle_mapsKeyHoursCorrectly() {
        // 00:00 (midnight) should be at top (-90 degrees / 270 degrees)
        assertThat(ClockChartMath.minuteToAngle(0)).isEqualTo(270f)

        // 06:00 (6 * 60 = 360 min) -> 360 * 0.25 - 90 = 0 degrees (3 o'clock)
        assertThat(ClockChartMath.minuteToAngle(360)).isEqualTo(0f)

        // 12:00 (12 * 60 = 720 min) -> 720 * 0.25 - 90 = 90 degrees (6 o'clock)
        assertThat(ClockChartMath.minuteToAngle(720)).isEqualTo(90f)

        // 18:00 (18 * 60 = 1080 min) -> 1080 * 0.25 - 90 = 180 degrees (9 o'clock)
        assertThat(ClockChartMath.minuteToAngle(1080)).isEqualTo(180f)
    }

    @Test
    fun computeArcsForInterval_normalDayInterval_returnsSingleArc() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val dayEnd = cal.timeInMillis

        // Sleep from 10:00 to 12:00 (120 minutes = 30 degrees sweep)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        val sleepStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        val sleepEnd = cal.timeInMillis

        val arcs = ClockChartMath.computeArcsForInterval(
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            eventStart = sleepStart,
            eventEnd = sleepEnd,
            isSleep = true
        )

        assertThat(arcs).hasSize(1)
        assertThat(arcs[0].sweepAngle).isEqualTo(30f)
        assertThat(arcs[0].isSleep).isTrue()
    }

    @Test
    fun computeDayCycleArcs_generatesBothSleepAndActivityArcsWithCorrectFlags() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val dayEnd = cal.timeInMillis

        // Sleep from 08:00 to 10:00
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 0)
        val sleepStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 10)
        val sleepEnd = cal.timeInMillis

        val sleepEvent = com.residentsleeper.data.model.BabyEvent(
            type = com.residentsleeper.data.model.EventType.SLEEP,
            startTime = sleepStart,
            endTime = sleepEnd
        )

        // Mock current time at 12:00
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val currentTime = cal.timeInMillis

        val arcs = ClockChartMath.computeDayCycleArcs(
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            events = listOf(sleepEvent),
            currentTime = currentTime
        )

        val sleepArcs = arcs.filter { it.isSleep }
        val activityArcs = arcs.filter { !it.isSleep }

        assertThat(sleepArcs).isNotEmpty()
        assertThat(activityArcs).isNotEmpty()
        // 08:00 to 10:00 is 120 min = 30 degrees
        assertThat(sleepArcs.first().sweepAngle).isEqualTo(30f)
    }
}

