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

object ClockChartMath {

    const val MINUTES_PER_DAY = 1440f
    const val DEGREES_PER_MINUTE = 360f / MINUTES_PER_DAY // 0.25 deg/min
    const val TOP_CLOCK_OFFSET = -90f // 00:00 at 12 o'clock

    /**
     * Converts an epoch millisecond timestamp to the minute of the day in local time (0 .. 1439).
     */
    fun getMinuteOfDay(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Converts a minute of the day (0..1439) to a Canvas start angle in degrees,
     * where 00:00 is at top (-90 degrees) and moves clockwise.
     */
    fun minuteToAngle(minuteOfDay: Int): Float {
        return (minuteOfDay * DEGREES_PER_MINUTE + TOP_CLOCK_OFFSET + 360f) % 360f
    }

    /**
     * Converts a start timestamp and end timestamp on a given day into one or two drawing arcs
     * (handling wrap-around at midnight).
     *
     * @param dayStartMillis Start of the selected day (00:00:00)
     * @param dayEndMillis End of the selected day (23:59:59.999)
     * @param eventStart Start time of event
     * @param eventEnd End time of event
     * @param isSleep Whether this interval is a sleep interval
     */
    fun computeArcsForInterval(
        dayStartMillis: Long,
        dayEndMillis: Long,
        eventStart: Long,
        eventEnd: Long,
        isSleep: Boolean
    ): List<ChartArc> {
        // Clamp to current day window
        val clampedStart = maxOf(dayStartMillis, eventStart)
        val clampedEnd = minOf(dayEndMillis, eventEnd)

        if (clampedStart >= clampedEnd) return emptyList()

        val startMinute = getMinuteOfDay(clampedStart)
        val endMinute = getMinuteOfDay(clampedEnd)

        return if (endMinute >= startMinute) {
            val sweep = (endMinute - startMinute) * DEGREES_PER_MINUTE
            val angle = minuteToAngle(startMinute)
            listOf(ChartArc(startAngle = angle, sweepAngle = sweep, isSleep = isSleep))
        } else {
            // Crossed midnight within the day range
            val sweep1 = (1440 - startMinute) * DEGREES_PER_MINUTE
            val angle1 = minuteToAngle(startMinute)

            val sweep2 = endMinute * DEGREES_PER_MINUTE
            val angle2 = minuteToAngle(0)

            listOf(
                ChartArc(startAngle = angle1, sweepAngle = sweep1, isSleep = isSleep),
                ChartArc(startAngle = angle2, sweepAngle = sweep2, isSleep = isSleep)
            )
        }
    }

    /**
     * Computes the angle for a point-in-time marker (nursing or diaper).
     */
    fun computeMarkerAngle(timestamp: Long): Float {
        val minute = getMinuteOfDay(timestamp)
        return minuteToAngle(minute)
    }
}
