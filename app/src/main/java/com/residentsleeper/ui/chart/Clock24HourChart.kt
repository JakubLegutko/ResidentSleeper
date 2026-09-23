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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    sizeDp: Dp = 290.dp,
    strokeWidthDp: Dp = 34.dp
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

            // 4. Draw Point Markers (Nursing & Diapers)
            for (event in events) {
                when (event.type) {
                    EventType.NURSING -> {
                        val angle = ClockChartMath.computeMarkerAngle(event.startTime, dayStartHour)
                        val angleRad = (angle * PI / 180f).toFloat()
                        val markerPos = Offset(
                            x = center.x + radius * cos(angleRad),
                            y = center.y + radius * sin(angleRad)
                        )
                        val markerColor = NursingPink
                        // White outline halo
                        drawCircle(
                            color = Color.White,
                            radius = 6.5.dp.toPx(),
                            center = markerPos
                        )
                        // Colored center
                        drawCircle(
                            color = markerColor,
                            radius = 4.8.dp.toPx(),
                            center = markerPos
                        )
                        // Crisp dark outline border for pop against any background
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 6.5.dp.toPx(),
                            center = markerPos,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    EventType.DIAPER -> {
                        val angle = ClockChartMath.computeMarkerAngle(event.startTime, dayStartHour)
                        val angleRad = (angle * PI / 180f).toFloat()
                        // Offset slightly to outer edge of ring
                        val outerRadius = radius + (strokePx / 2f) - 3.dp.toPx()
                        val markerPos = Offset(
                            x = center.x + outerRadius * cos(angleRad),
                            y = center.y + outerRadius * sin(angleRad)
                        )
                        val dotColor = when (event.diaperType) {
                            DiaperType.PEE -> DiaperPeeCyan
                            DiaperType.POO -> DiaperPooWarm
                            DiaperType.BOTH, null -> DiaperPooWarm
                        }
                        // White outline halo
                        drawCircle(
                            color = Color.White,
                            radius = 5.5.dp.toPx(),
                            center = markerPos
                        )
                        // Colored center
                        drawCircle(
                            color = dotColor,
                            radius = 3.8.dp.toPx(),
                            center = markerPos
                        )
                        // Crisp dark outline border
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 5.5.dp.toPx(),
                            center = markerPos,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        if (event.diaperType == DiaperType.BOTH) {
                            drawCircle(
                                color = DiaperPeeCyan,
                                radius = 2.dp.toPx(),
                                center = markerPos
                            )
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
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            if (wakeState.isSleeping) {
                Text(
                    text = "😴 Sleeping",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleepIndigo
                )
            } else {
                Text(
                    text = "👶 Awake",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WakeMint
                )
                Text(
                    text = "${wakeState.wakeDurationMinutes} min",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                if (wakeState.minutesUntilWakeEnd != null) {
                    val remaining = wakeState.minutesUntilWakeEnd
                    val statusText = if (remaining >= 0) {
                        "Window ends: in $remaining m"
                    } else {
                        "Overdue: by ${-remaining} m"
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
                    text = "🍼 Nursing now",
                    style = MaterialTheme.typography.labelSmall,
                    color = NursingPink,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (feedingState.minutesUntilNextFeed != null) {
                val nextIn = feedingState.minutesUntilNextFeed
                val feedText = if (feedingState.isOptionalNightFeed) {
                    if (nextIn >= 0) "Next feed: in ${nextIn}m (Optional)" else "🌙 Night feed: Optional (On demand)"
                } else {
                    if (nextIn >= 0) "Next feed: in ${nextIn}m" else "Feed due now!"
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
