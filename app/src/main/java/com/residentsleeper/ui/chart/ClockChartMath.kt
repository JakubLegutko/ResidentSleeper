package com.residentsleeper.ui.chart

import java.util.Calendar

data class ChartArc(
    val startAngle: Float,
    val sweepAngle: Float,
    val isSleep: Boolean
)

data class ChartMarker(
    val angle: Float,
    val markerType: MarkerType,
    val label: String? = null
)

enum class MarkerType {
    NURSING_LEFT,
    NURSING_RIGHT,
    NURSING_BOTH,
    NURSING_BOTTLE,
    DIAPER_PEE,
    DIAPER_POO,
    DIAPER_BOTH
}

enum class DayPeriod {
    DAY,
    NIGHT
}

object ClockChartMath {

    const val MINUTES_PER_DAY = 1440f
    const val DEGREES_PER_MINUTE = 360f / MINUTES_PER_DAY // 0.25 deg/min
    const val MINUTES_PER_12H = 720f
    const val DEGREES_PER_MINUTE_12H = 360f / MINUTES_PER_12H // 0.5 deg/min
    const val TOP_CLOCK_OFFSET = -90f // 12 o'clock at top of circle

    /**
     * Converts an epoch millisecond timestamp to the minute of the day in local time (0 .. 1439).
     */
    fun getMinuteOfDay(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Converts an epoch millisecond timestamp to the fractional minute of the day in local time (0.0 .. 1439.999).
     */
    fun getMinuteOfDayFloat(timestamp: Long): Float {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.HOUR_OF_DAY) * 60f +
                cal.get(Calendar.MINUTE) +
                cal.get(Calendar.SECOND) / 60f +
                cal.get(Calendar.MILLISECOND) / 60000f
    }

    /**
     * Converts a fractional minute of the day (0.0..1439.999) to a Canvas start angle in degrees,
     * where startHour is at the top (-90 / 270 degrees) and moves clockwise.
     */
    fun minuteToAngle(minuteOfDay: Float, startHour: Int = 7): Float {
        val startMinute = startHour * 60f
        val relativeMinute = (minuteOfDay - startMinute + 1440f) % 1440f
        return (relativeMinute * DEGREES_PER_MINUTE + TOP_CLOCK_OFFSET + 360f) % 360f
    }

    /**
     * Converts a minute of the day (0..1439) to a Canvas start angle in degrees,
     * where startHour is at top (-90 degrees) and moves clockwise.
     */
    fun minuteToAngle(minuteOfDay: Int, startHour: Int = 7): Float =
        minuteToAngle(minuteOfDay.toFloat(), startHour)

    /**
     * Converts a start timestamp and end timestamp on a given day/period into a drawing arc.
     *
     * @param dayStartMillis Start of the selected day/period window (e.g. 07:00)
     * @param dayEndMillis End of the selected day/period window (e.g. 19:00 for 12h day, or next morning)
     * @param eventStart Start time of event
     * @param eventEnd End time of event
     * @param isSleep Whether this interval is a sleep interval
     * @param startHour Hour of starting the day (default 7:00 AM)
     * @param is12Hour Whether computing for 12-hour period (0.5 deg/min) instead of 24-hour (0.25 deg/min)
     */
    fun computeArcsForInterval(
        dayStartMillis: Long,
        dayEndMillis: Long,
        eventStart: Long,
        eventEnd: Long,
        isSleep: Boolean,
        startHour: Int = 7,
        is12Hour: Boolean = false
    ): List<ChartArc> {
        // Clamp to current window
        val clampedStart = maxOf(dayStartMillis, eventStart)
        val clampedEnd = minOf(dayEndMillis, eventEnd)

        if (clampedStart >= clampedEnd) return emptyList()

        val durationMinutes = (clampedEnd - clampedStart) / 60000f
        val degPerMin = if (is12Hour) DEGREES_PER_MINUTE_12H else DEGREES_PER_MINUTE
        val sweep = minOf(360f, durationMinutes * degPerMin)

        val startAngle = if (is12Hour) {
            val minutesFromStart = (clampedStart - dayStartMillis) / 60000f
            (minutesFromStart * DEGREES_PER_MINUTE_12H + TOP_CLOCK_OFFSET + 360f) % 360f
        } else {
            val minuteOfDay = getMinuteOfDayFloat(clampedStart)
            minuteToAngle(minuteOfDay, startHour)
        }

        return listOf(ChartArc(startAngle = startAngle, sweepAngle = sweep, isSleep = isSleep))
    }

    /**
     * Computes the angle for a point-in-time marker (nursing or diaper).
     */
    fun computeMarkerAngle(
        timestamp: Long,
        startHour: Int = 7,
        windowStartMillis: Long = 0L,
        is12Hour: Boolean = false
    ): Float {
        return if (is12Hour) {
            val minutesFromStart = (timestamp - windowStartMillis) / 60000f
            (minutesFromStart * DEGREES_PER_MINUTE_12H + TOP_CLOCK_OFFSET + 360f) % 360f
        } else {
            val minute = getMinuteOfDay(timestamp)
            minuteToAngle(minute, startHour)
        }
    }

    /**
     * Computes all arcs for the day or 12h period, distinguishing between Sleep intervals and Activity/Wake intervals.
     */
    fun computeDayCycleArcs(
        dayStartMillis: Long,
        dayEndMillis: Long,
        events: List<com.residentsleeper.data.model.BabyEvent>,
        currentTime: Long = System.currentTimeMillis(),
        startHour: Int = 7,
        is12Hour: Boolean = false
    ): List<ChartArc> {
        val sleepEvents = events
            .filter { it.type == com.residentsleeper.data.model.EventType.SLEEP }
            .filter { (it.endTime ?: currentTime) > dayStartMillis && it.startTime < dayEndMillis }
            .sortedBy { it.startTime }

        val arcs = mutableListOf<ChartArc>()

        // 1. Generate Sleep Arcs
        for (s in sleepEvents) {
            val end = s.endTime ?: currentTime
            arcs.addAll(
                computeArcsForInterval(
                    dayStartMillis = dayStartMillis,
                    dayEndMillis = dayEndMillis,
                    eventStart = s.startTime,
                    eventEnd = end,
                    isSleep = true,
                    startHour = startHour,
                    is12Hour = is12Hour
                )
            )
        }

        // 2. Generate Activity / Wake Window Arcs between sleeps
        val effectiveLimit = minOf(dayEndMillis, if (dayEndMillis > currentTime) currentTime else dayEndMillis)

        if (sleepEvents.isEmpty()) {
            // No sleep recorded: awake from start of day to effective limit
            if (dayStartMillis < effectiveLimit) {
                arcs.addAll(
                    computeArcsForInterval(
                        dayStartMillis = dayStartMillis,
                        dayEndMillis = dayEndMillis,
                        eventStart = dayStartMillis,
                        eventEnd = effectiveLimit,
                        isSleep = false,
                        startHour = startHour,
                        is12Hour = is12Hour
                    )
                )
            }
        } else {
            // Wake window before first sleep of the day (if first sleep started after dayStart)
            val firstSleep = sleepEvents.first()
            val firstSleepClampedStart = maxOf(dayStartMillis, firstSleep.startTime)
            if (firstSleepClampedStart > dayStartMillis) {
                val wakeEnd = minOf(firstSleepClampedStart, effectiveLimit)
                if (dayStartMillis < wakeEnd) {
                    arcs.addAll(
                        computeArcsForInterval(
                            dayStartMillis = dayStartMillis,
                            dayEndMillis = dayEndMillis,
                            eventStart = dayStartMillis,
                            eventEnd = wakeEnd,
                            isSleep = false,
                            startHour = startHour,
                            is12Hour = is12Hour
                        )
                    )
                }
            }

            // Wake windows between consecutive sleeps
            for (i in 0 until sleepEvents.size - 1) {
                val currentEnd = sleepEvents[i].endTime ?: currentTime
                val nextStart = sleepEvents[i + 1].startTime
                if (nextStart > currentEnd) {
                    val wakeStart = maxOf(dayStartMillis, currentEnd)
                    val wakeEnd = minOf(nextStart, effectiveLimit)
                    if (wakeStart < wakeEnd) {
                        arcs.addAll(
                            computeArcsForInterval(
                                dayStartMillis = dayStartMillis,
                                dayEndMillis = dayEndMillis,
                                eventStart = wakeStart,
                                eventEnd = wakeEnd,
                                isSleep = false,
                                startHour = startHour,
                                is12Hour = is12Hour
                            )
                        )
                    }
                }
            }

            // Wake window after last sleep up to effective limit
            val lastSleep = sleepEvents.last()
            val lastEnd = lastSleep.endTime
            if (lastEnd != null && lastEnd < effectiveLimit) {
                val wakeStart = maxOf(dayStartMillis, lastEnd)
                if (wakeStart < effectiveLimit) {
                    arcs.addAll(
                        computeArcsForInterval(
                            dayStartMillis = dayStartMillis,
                            dayEndMillis = dayEndMillis,
                            eventStart = wakeStart,
                            eventEnd = effectiveLimit,
                            isSleep = false,
                            startHour = startHour,
                            is12Hour = is12Hour
                        )
                    )
                }
            }
        }

        return arcs
    }
}

