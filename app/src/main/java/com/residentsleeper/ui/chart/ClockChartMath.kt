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
    fun minuteToAngle(minuteOfDay: Int, startHour: Int = 7): Float {
        val startMinute = startHour * 60
        val relativeMinute = (minuteOfDay - startMinute + 1440) % 1440
        return (relativeMinute * DEGREES_PER_MINUTE + TOP_CLOCK_OFFSET + 360f) % 360f
    }

    /**
     * Converts a start timestamp and end timestamp on a given day into a drawing arc.
     *
     * @param dayStartMillis Start of the selected day (e.g. 07:00)
     * @param dayEndMillis End of the selected day (e.g. 06:59:59 next day)
     * @param eventStart Start time of event
     * @param eventEnd End time of event
     * @param isSleep Whether this interval is a sleep interval
     * @param startHour Hour of starting the day (default 7:00 AM)
     */
    fun computeArcsForInterval(
        dayStartMillis: Long,
        dayEndMillis: Long,
        eventStart: Long,
        eventEnd: Long,
        isSleep: Boolean,
        startHour: Int = 7
    ): List<ChartArc> {
        // Clamp to current day window
        val clampedStart = maxOf(dayStartMillis, eventStart)
        val clampedEnd = minOf(dayEndMillis, eventEnd)

        if (clampedStart >= clampedEnd) return emptyList()

        val offsetStart = (clampedStart - dayStartMillis) / 60000f
        val offsetEnd = (clampedEnd - dayStartMillis) / 60000f

        val sweep = (offsetEnd - offsetStart) * DEGREES_PER_MINUTE
        val startAngle = (offsetStart * DEGREES_PER_MINUTE + TOP_CLOCK_OFFSET + 360f) % 360f

        return listOf(ChartArc(startAngle = startAngle, sweepAngle = sweep, isSleep = isSleep))
    }

    /**
     * Computes the angle for a point-in-time marker (nursing or diaper).
     */
    fun computeMarkerAngle(timestamp: Long, startHour: Int = 7): Float {
        val minute = getMinuteOfDay(timestamp)
        return minuteToAngle(minute, startHour)
    }

    /**
     * Computes all arcs for the day, distinguishing between Sleep intervals and Activity/Wake intervals.
     */
    fun computeDayCycleArcs(
        dayStartMillis: Long,
        dayEndMillis: Long,
        events: List<com.residentsleeper.data.model.BabyEvent>,
        currentTime: Long = System.currentTimeMillis(),
        startHour: Int = 7
    ): List<ChartArc> {
        val sleepEvents = events
            .filter { it.type == com.residentsleeper.data.model.EventType.SLEEP }
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
                    startHour = startHour
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
                        startHour = startHour
                    )
                )
            }
        } else {
            // Wake window before first sleep of the day (if first sleep started after dayStart)
            val firstSleep = sleepEvents.first()
            if (firstSleep.startTime > dayStartMillis) {
                val wakeEnd = minOf(firstSleep.startTime, effectiveLimit)
                if (dayStartMillis < wakeEnd) {
                    arcs.addAll(
                        computeArcsForInterval(
                            dayStartMillis = dayStartMillis,
                            dayEndMillis = dayEndMillis,
                            eventStart = dayStartMillis,
                            eventEnd = wakeEnd,
                            isSleep = false,
                            startHour = startHour
                        )
                    )
                }
            }

            // Wake windows between consecutive sleeps
            for (i in 0 until sleepEvents.size - 1) {
                val currentEnd = sleepEvents[i].endTime ?: currentTime
                val nextStart = sleepEvents[i + 1].startTime
                if (nextStart > currentEnd) {
                    val wakeEnd = minOf(nextStart, effectiveLimit)
                    if (currentEnd < wakeEnd) {
                        arcs.addAll(
                            computeArcsForInterval(
                                dayStartMillis = dayStartMillis,
                                dayEndMillis = dayEndMillis,
                                eventStart = currentEnd,
                                eventEnd = wakeEnd,
                                isSleep = false,
                                startHour = startHour
                            )
                        )
                    }
                }
            }

            // Wake window after last sleep up to effective limit
            val lastSleep = sleepEvents.last()
            val lastEnd = lastSleep.endTime
            if (lastEnd != null && lastEnd < effectiveLimit) {
                arcs.addAll(
                    computeArcsForInterval(
                        dayStartMillis = dayStartMillis,
                        dayEndMillis = dayEndMillis,
                        eventStart = lastEnd,
                        eventEnd = effectiveLimit,
                        isSleep = false,
                        startHour = startHour
                    )
                )
            }
        }

        return arcs
    }
}

