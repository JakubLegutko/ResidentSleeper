package com.residentsleeper.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.residentsleeper.R
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import com.residentsleeper.domain.FeedingState
import com.residentsleeper.domain.WakeWindowState
import com.residentsleeper.ui.theme.CurrentTimeNeedle
import com.residentsleeper.ui.theme.DiaperPeeCyan
import com.residentsleeper.ui.theme.DiaperPooWarm
import com.residentsleeper.ui.theme.NursingPink
import com.residentsleeper.ui.theme.RingBackgroundDark
import com.residentsleeper.ui.theme.RingBackgroundLight
import com.residentsleeper.ui.theme.SleepIndigo
import com.residentsleeper.ui.theme.SleepIndigoGradientEnd
import com.residentsleeper.ui.theme.SleepIndigoGradientStart
import com.residentsleeper.ui.theme.WakeMint
import com.residentsleeper.ui.theme.WakeMintGradientEnd
import com.residentsleeper.ui.theme.WakeMintGradientStart
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Clock24HourChart(
    dayStartMillis: Long,
    dayEndMillis: Long,
    events: List<BabyEvent>,
    wakeState: WakeWindowState,
    feedingState: FeedingState,
    dayStartHour: Int = 7,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 324.dp,
    strokeWidthDp: Dp = 38.dp
) {
    val isDark = isSystemInDarkTheme()
    val ringBgColor = if (isDark) RingBackgroundDark else RingBackgroundLight
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val canvasSize = size.minDimension
            val strokePx = strokeWidthDp.toPx()
            val radius = (canvasSize - strokePx) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Draw base thick hollow ring track
            drawCircle(
                color = ringBgColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )

            // 2. Draw 24-hour hour ticks & labels (shifted with dayStartHour at the top)
            drawHourTicksAndLabels(center, radius, strokePx, textColor, dayStartHour)

            // 3. Draw Sleep and Activity Arcs on the ring with distinct colors
            val cycleArcs = ClockChartMath.computeDayCycleArcs(
                dayStartMillis = dayStartMillis,
                dayEndMillis = dayEndMillis,
                events = events,
                currentTime = System.currentTimeMillis(),
                startHour = dayStartHour
            )

            // Draw Activity / Wake Arcs first
            for (arc in cycleArcs) {
                if (!arc.isSleep) {
                    val a1 = (arc.startAngle * PI / 180f).toFloat()
                    val a2 = ((arc.startAngle + arc.sweepAngle) * PI / 180f).toFloat()
                    val p1 = Offset(center.x + radius * cos(a1), center.y + radius * sin(a1))
                    val p2 = Offset(center.x + radius * cos(a2), center.y + radius * sin(a2))
                    val brush = Brush.linearGradient(
                        colors = listOf(WakeMintGradientStart, WakeMintGradientEnd),
                        start = p1,
                        end = p2
                    )
                    drawArc(
                        brush = brush,
                        startAngle = arc.startAngle,
                        sweepAngle = arc.sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                }
            }

            // Draw Sleep Arcs on top
            for (arc in cycleArcs) {
                if (arc.isSleep) {
                    val a1 = (arc.startAngle * PI / 180f).toFloat()
                    val a2 = ((arc.startAngle + arc.sweepAngle) * PI / 180f).toFloat()
                    val p1 = Offset(center.x + radius * cos(a1), center.y + radius * sin(a1))
                    val p2 = Offset(center.x + radius * cos(a2), center.y + radius * sin(a2))
                    val brush = Brush.linearGradient(
                        colors = listOf(SleepIndigoGradientStart, SleepIndigoGradientEnd),
                        start = p1,
                        end = p2
                    )
                    drawArc(
                        brush = brush,
                        startAngle = arc.startAngle,
                        sweepAngle = arc.sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                }
            }

            // 4. Draw Point Markers (Nursing in center lane, Pee in outer lane, Poo in inner lane)
            val peeTrackRadius = radius + (strokePx * 0.28f)   // Outer lane for Pee
            val pooTrackRadius = radius - (strokePx * 0.28f)   // Inner lane for Poo
            val nursingTrackRadius = radius                    // Centerline for Nursing

            for (event in events) {
                when (event.type) {
                    EventType.NURSING -> {
                        val angle = ClockChartMath.computeMarkerAngle(event.startTime, dayStartHour)
                        val angleRad = (angle * PI / 180f).toFloat()
                        val markerPos = Offset(
                            x = center.x + nursingTrackRadius * cos(angleRad),
                            y = center.y + nursingTrackRadius * sin(angleRad)
                        )
                        drawMarkerDot(
                            center = markerPos,
                            color = NursingPink,
                            radiusPx = 6.5.dp.toPx(),
                            innerRadiusPx = 4.8.dp.toPx()
                        )
                    }
                    EventType.DIAPER -> {
                        val angle = ClockChartMath.computeMarkerAngle(event.startTime, dayStartHour)
                        val angleRad = (angle * PI / 180f).toFloat()

                        when (event.diaperType) {
                            DiaperType.PEE -> {
                                val markerPos = Offset(
                                    x = center.x + peeTrackRadius * cos(angleRad),
                                    y = center.y + peeTrackRadius * sin(angleRad)
                                )
                                drawMarkerDot(
                                    center = markerPos,
                                    color = DiaperPeeCyan,
                                    radiusPx = 5.2.dp.toPx(),
                                    innerRadiusPx = 3.6.dp.toPx()
                                )
                            }
                            DiaperType.POO -> {
                                val markerPos = Offset(
                                    x = center.x + pooTrackRadius * cos(angleRad),
                                    y = center.y + pooTrackRadius * sin(angleRad)
                                )
                                drawMarkerDot(
                                    center = markerPos,
                                    color = DiaperPooWarm,
                                    radiusPx = 5.2.dp.toPx(),
                                    innerRadiusPx = 3.6.dp.toPx()
                                )
                            }
                            DiaperType.BOTH, null -> {
                                val peePos = Offset(
                                    x = center.x + peeTrackRadius * cos(angleRad),
                                    y = center.y + peeTrackRadius * sin(angleRad)
                                )
                                val pooPos = Offset(
                                    x = center.x + pooTrackRadius * cos(angleRad),
                                    y = center.y + pooTrackRadius * sin(angleRad)
                                )
                                // Connector line joining pee and poo for combined diaper event
                                drawLine(
                                    color = Color(0xFF0F172A).copy(alpha = 0.35f),
                                    start = peePos,
                                    end = pooPos,
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawMarkerDot(
                                    center = peePos,
                                    color = DiaperPeeCyan,
                                    radiusPx = 5.2.dp.toPx(),
                                    innerRadiusPx = 3.6.dp.toPx()
                                )
                                drawMarkerDot(
                                    center = pooPos,
                                    color = DiaperPooWarm,
                                    radiusPx = 5.2.dp.toPx(),
                                    innerRadiusPx = 3.6.dp.toPx()
                                )
                            }
                        }
                    }
                    EventType.SLEEP -> { /* Handled as arcs */ }
                }
            }

            // 5. Draw current time indicator needle
            val nowMinute = ClockChartMath.getMinuteOfDay(System.currentTimeMillis())
            val nowAngle = ClockChartMath.minuteToAngle(nowMinute, dayStartHour)
            val nowAngleRad = (nowAngle * PI / 180f).toFloat()
            val needleEnd = Offset(
                x = center.x + (radius + strokePx / 2f + 4.dp.toPx()) * cos(nowAngleRad),
                y = center.y + (radius + strokePx / 2f + 4.dp.toPx()) * sin(nowAngleRad)
            )
            val needleStart = Offset(
                x = center.x + (radius - strokePx / 2f - 4.dp.toPx()) * cos(nowAngleRad),
                y = center.y + (radius - strokePx / 2f - 4.dp.toPx()) * sin(nowAngleRad)
            )
            drawLine(
                color = CurrentTimeNeedle,
                start = needleStart,
                end = needleEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Hollow Center: Live summary status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            if (wakeState.isSleeping) {
                Text(
                    text = stringResource(R.string.state_sleeping),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleepIndigo
                )
            } else {
                Text(
                    text = stringResource(R.string.state_awake),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WakeMint
                )
                Text(
                    text = stringResource(R.string.state_wake_duration, wakeState.wakeDurationMinutes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                if (wakeState.minutesUntilWakeEnd != null) {
                    val remaining = wakeState.minutesUntilWakeEnd
                    val statusText = if (remaining >= 0) {
                        stringResource(R.string.state_window_ends_in, remaining)
                    } else {
                        stringResource(R.string.state_overdue_by, -remaining)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (remaining < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = wakeState.recommendationTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SleepIndigo,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (feedingState.isNursingNow) {
                Text(
                    text = stringResource(R.string.state_nursing_now),
                    style = MaterialTheme.typography.labelSmall,
                    color = NursingPink,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (feedingState.minutesUntilNextFeed != null) {
                val nextIn = feedingState.minutesUntilNextFeed
                val feedText = if (feedingState.isOptionalNightFeed) {
                    if (nextIn >= 0) stringResource(R.string.state_next_feed_optional, nextIn) else stringResource(R.string.state_night_feed_optional)
                } else {
                    if (nextIn >= 0) stringResource(R.string.state_next_feed_in, nextIn) else stringResource(R.string.state_feed_due_now)
                }
                Text(
                    text = feedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = NursingPink,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun DrawScope.drawHourTicksAndLabels(
    center: Offset,
    radius: Float,
    strokePx: Float,
    textColor: Color,
    dayStartHour: Int
) {
    val tickRadiusInner = radius - (strokePx / 2f)
    val majorTickRadiusInner = radius - (strokePx / 2f) - 6.dp.toPx()

    for (i in 0 until 24) {
        val hourOfDay = (dayStartHour + i) % 24
        val angle = ClockChartMath.minuteToAngle(hourOfDay * 60, dayStartHour)
        val angleRad = (angle * PI / 180f).toFloat()

        val isMajor = i % 3 == 0
        val innerR = if (isMajor) majorTickRadiusInner else tickRadiusInner
        val outerR = radius - (strokePx / 2f) + 2.dp.toPx()

        val p1 = Offset(
            x = center.x + innerR * cos(angleRad),
            y = center.y + innerR * sin(angleRad)
        )
        val p2 = Offset(
            x = center.x + outerR * cos(angleRad),
            y = center.y + outerR * sin(angleRad)
        )

        drawLine(
            color = textColor.copy(alpha = if (isMajor) 0.5f else 0.2f),
            start = p1,
            end = p2,
            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
        )

        // Draw hour numbers for 4 cardinal points (Top: startHour, Right: +6h, Bottom: +12h, Left: +18h)
        if (i % 6 == 0) {
            val labelR = radius + (strokePx / 2f) + 12.dp.toPx()
            val labelX = center.x + labelR * cos(angleRad)
            val labelY = center.y + labelR * sin(angleRad) + 4.dp.toPx()

            val text = java.lang.String.format(java.util.Locale.getDefault(), "%02d", hourOfDay)

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(text, labelX, labelY, paint)
            }
        }
    }
}

private fun DrawScope.drawMarkerDot(
    center: Offset,
    color: Color,
    radiusPx: Float,
    innerRadiusPx: Float
) {
    // 1. White contrast halo
    drawCircle(
        color = Color.White,
        radius = radiusPx,
        center = center
    )
    // 2. Colored center
    drawCircle(
        color = color,
        radius = innerRadiusPx,
        center = center
    )
    // 3. Crisp dark outline border for popping against any background
    drawCircle(
        color = Color(0xFF0F172A),
        radius = radiusPx,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

