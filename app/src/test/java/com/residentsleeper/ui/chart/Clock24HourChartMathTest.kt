package com.residentsleeper.ui.chart

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class Clock24HourChartMathTest {

    @Test
    fun minuteToAngle_mapsKeyHoursCorrectly() {
        // When startHour is 0 (midnight at top of clock: -90 / 270 deg)
        assertThat(ClockChartMath.minuteToAngle(0, startHour = 0)).isEqualTo(270f)
        assertThat(ClockChartMath.minuteToAngle(360, startHour = 0)).isEqualTo(0f)
        assertThat(ClockChartMath.minuteToAngle(720, startHour = 0)).isEqualTo(90f)
        assertThat(ClockChartMath.minuteToAngle(1080, startHour = 0)).isEqualTo(180f)

        // When startHour is 7 (default 07:00 at top of clock: 270 deg)
        assertThat(ClockChartMath.minuteToAngle(420, startHour = 7)).isEqualTo(270f)
        assertThat(ClockChartMath.minuteToAngle(780, startHour = 7)).isEqualTo(0f)
        assertThat(ClockChartMath.minuteToAngle(1140, startHour = 7)).isEqualTo(90f)
        assertThat(ClockChartMath.minuteToAngle(60, startHour = 7)).isEqualTo(180f)
    }

    @Test
    fun computeArcsForInterval_normalDayInterval_returnsSingleArcWithCorrectStartAngle() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val dayEnd = cal.timeInMillis

        // Sleep from 10:00 to 12:00 (120 minutes = 30 degrees sweep)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val sleepStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        val sleepEnd = cal.timeInMillis

        // With startHour = 0 (top of clock is 00:00)
        val arcs0 = ClockChartMath.computeArcsForInterval(
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            eventStart = sleepStart,
            eventEnd = sleepEnd,
            isSleep = true,
            startHour = 0
        )
        assertThat(arcs0).hasSize(1)
        assertThat(arcs0[0].sweepAngle).isEqualTo(30f)
        assertThat(arcs0[0].startAngle).isEqualTo(ClockChartMath.minuteToAngle(10 * 60, 0))
        assertThat(arcs0[0].isSleep).isTrue()

        // With startHour = 10 (top of clock is 10:00 -> startAngle must be 270 degrees / top of clock)
        val arcs10 = ClockChartMath.computeArcsForInterval(
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            eventStart = sleepStart,
            eventEnd = sleepEnd,
            isSleep = true,
            startHour = 10
        )
        assertThat(arcs10).hasSize(1)
        assertThat(arcs10[0].sweepAngle).isEqualTo(30f)
        assertThat(arcs10[0].startAngle).isEqualTo(270f) // 10:00 is top of clock!

        // With startHour = 7 (default)
        val arcs7 = ClockChartMath.computeArcsForInterval(
            dayStartMillis = dayStart,
            dayEndMillis = dayEnd,
            eventStart = sleepStart,
            eventEnd = sleepEnd,
            isSleep = true,
            startHour = 7
        )
        assertThat(arcs7).hasSize(1)
        assertThat(arcs7[0].sweepAngle).isEqualTo(30f)
        assertThat(arcs7[0].startAngle).isEqualTo(ClockChartMath.minuteToAngle(10 * 60, 7))
    }

    @Test
    fun computeDayCycleArcs_generatesBothSleepAndActivityArcsWithCorrectFlags() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val dayEnd = cal.timeInMillis

        // Sleep from 08:00 to 10:00 (1 hour after dayStart of 07:00)
        cal.timeInMillis = dayStart
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 0)
        val sleepStart = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
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
            currentTime = currentTime,
            startHour = 7
        )

        val sleepArcs = arcs.filter { it.isSleep }
        val activityArcs = arcs.filter { !it.isSleep }

        assertThat(sleepArcs).isNotEmpty()
        assertThat(activityArcs).isNotEmpty()
        // 08:00 to 10:00 is 120 min = 30 degrees
        assertThat(sleepArcs.first().sweepAngle).isEqualTo(30f)
        assertThat(sleepArcs.first().startAngle).isEqualTo(ClockChartMath.minuteToAngle(8 * 60, 7))

        // Activity before sleep: 07:00 to 08:00 (60 min = 15 degrees, starts at 270 deg)
        val firstWake = activityArcs.first()
        assertThat(firstWake.startAngle).isEqualTo(270f)
        assertThat(firstWake.sweepAngle).isEqualTo(15f)
    }

    @Test
    fun computeMarkerAngle_12HourMode_mapsDayAndNightHoursCorrectly() {
        val windowStart = 1000000L
        val hourMillis = 3600000L

        // Top of clock (0h from window start) -> -90 / 270 deg
        val topAngle = ClockChartMath.computeMarkerAngle(windowStart, windowStartMillis = windowStart, is12Hour = true)
        assertThat(topAngle).isEqualTo(270f)

        // 3h from window start (e.g. 10:00 for Day or 22:00 for Night) -> 0 deg (Right)
        val rightAngle = ClockChartMath.computeMarkerAngle(windowStart + 3 * hourMillis, windowStartMillis = windowStart, is12Hour = true)
        assertThat(rightAngle).isEqualTo(0f)

        // 6h from window start (e.g. 13:00 for Day or 01:00 for Night) -> 90 deg (Bottom)
        val bottomAngle = ClockChartMath.computeMarkerAngle(windowStart + 6 * hourMillis, windowStartMillis = windowStart, is12Hour = true)
        assertThat(bottomAngle).isEqualTo(90f)

        // 9h from window start (e.g. 16:00 for Day or 04:00 for Night) -> 180 deg (Left)
        val leftAngle = ClockChartMath.computeMarkerAngle(windowStart + 9 * hourMillis, windowStartMillis = windowStart, is12Hour = true)
        assertThat(leftAngle).isEqualTo(180f)
    }

    @Test
    fun computeDayCycleArcs_12HourMode_computesScaledArcsForDayAndNight() {
        val windowStart = 10000000L
        val windowEnd = windowStart + 12 * 3600000L // 12-hour period (720 min)

        // Sleep from +1h to +3h (120 min duration)
        val sleepStart = windowStart + 1 * 3600000L
        val sleepEnd = windowStart + 3 * 3600000L

        val sleepEvent = com.residentsleeper.data.model.BabyEvent(
            type = com.residentsleeper.data.model.EventType.SLEEP,
            startTime = sleepStart,
            endTime = sleepEnd
        )

        // Mock current time at +5h
        val currentTime = windowStart + 5 * 3600000L

        val arcs = ClockChartMath.computeDayCycleArcs(
            dayStartMillis = windowStart,
            dayEndMillis = windowEnd,
            events = listOf(sleepEvent),
            currentTime = currentTime,
            startHour = 7,
            is12Hour = true
        )

        val sleepArcs = arcs.filter { it.isSleep }
        val activityArcs = arcs.filter { !it.isSleep }

        assertThat(sleepArcs).hasSize(1)
        // 120 minutes in 12h mode: 120 * 0.5 deg/min = 60 degrees!
        assertThat(sleepArcs.first().sweepAngle).isEqualTo(60f)
        // Starts 60 min after windowStart: 60 * 0.5 - 90 = -60 -> 300 deg
        assertThat(sleepArcs.first().startAngle).isEqualTo(300f)

        // First wake window: 0 to 60 min -> 30 degrees sweep, starting at 270 deg (top)
        val firstWake = activityArcs.first()
        assertThat(firstWake.startAngle).isEqualTo(270f)
        assertThat(firstWake.sweepAngle).isEqualTo(30f)

        // Second wake window: from +3h to +5h (120 min = 60 deg sweep, starting at 0 deg)
        val secondWake = activityArcs[1]
        assertThat(secondWake.sweepAngle).isEqualTo(60f)
        assertThat(secondWake.startAngle).isEqualTo(0f)
    }
}

