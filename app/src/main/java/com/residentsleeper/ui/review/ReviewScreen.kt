package com.residentsleeper.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.residentsleeper.R
import com.residentsleeper.domain.DailySummary
import com.residentsleeper.ui.theme.DiaperPeeCyan
import com.residentsleeper.ui.theme.DiaperPooWarm
import com.residentsleeper.ui.theme.NursingPink
import com.residentsleeper.ui.theme.SleepIndigo
import com.residentsleeper.ui.theme.WakeMint
import java.text.SimpleDateFormat
import java.util.Locale

private data class MetricTrendConfig(
    val title: String,
    val color: Color,
    val unit: String,
    val extractor: (DailySummary) -> Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_review), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Period selector tabs
            val tabs = listOf(
                ReviewPeriod.DAILY to stringResource(R.string.review_daily),
                ReviewPeriod.WEEKLY to stringResource(R.string.review_weekly_avg),
                ReviewPeriod.MONTHLY to stringResource(R.string.review_monthly_avg)
            )

            val selectedIndex = tabs.indexOfFirst { it.first == state.selectedPeriod }

            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.setPeriod(pair.first) },
                        text = { Text(text = pair.second, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (state.selectedPeriod == ReviewPeriod.DAILY) {
                    val summary = state.dailySummary
                    if (summary != null) {
                        DailyMetricsView(
                            summary = summary,
                            selectedMetric = state.selectedMetric,
                            onSelectMetric = { viewModel.selectMetric(it) }
                        )
                    }
                } else {
                    val agg = state.aggregatedReview
                    if (agg != null) {
                        AggregatedMetricsView(
                            title = agg.title,
                            daysCount = agg.daysCount,
                            avgSleepMin = agg.avgTotalSleepMinutesPerDay,
                            avgDaySleepMin = agg.avgDaySleepMinutesPerDay,
                            avgNightSleepMin = agg.avgNightSleepMinutesPerDay,
                            avgWakeMin = agg.avgWakeWindowMinutes,
                            avgFeeds = agg.avgFeedingsPerDay,
                            avgBottleMl = agg.avgBottleMlPerDay,
                            avgPee = agg.avgDiaperPeePerDay,
                            avgPoo = agg.avgDiaperPooPerDay,
                            dailySummaries = agg.dailySummaries,
                            selectedPeriod = state.selectedPeriod,
                            selectedMetric = state.selectedMetric,
                            onSelectMetric = { viewModel.selectMetric(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMetricsView(
    summary: DailySummary,
    selectedMetric: ReviewMetric,
    onSelectMetric: (ReviewMetric) -> Unit
) {
    val totalHours = summary.totalSleepMinutes / 60
    val totalMins = summary.totalSleepMinutes % 60
    val dayHours = summary.daySleepMinutes / 60
    val dayMins = summary.daySleepMinutes % 60
    val nightHours = summary.nightSleepMinutes / 60
    val nightMins = summary.nightSleepMinutes % 60

    MetricCard(
        title = stringResource(R.string.stat_total_sleep),
        mainValue = "${totalHours}h ${totalMins}m",
        subValue = "Day: ${dayHours}h ${dayMins}m • Night: ${nightHours}h ${nightMins}m (${summary.napCount} naps)",
        icon = Icons.Default.Hotel,
        iconColor = SleepIndigo,
        selected = selectedMetric == ReviewMetric.SLEEP,
        onClick = { onSelectMetric(ReviewMetric.SLEEP) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = stringResource(R.string.stat_wake_window),
        mainValue = "${summary.averageWakeWindowMinutes} min",
        subValue = "Average activity cycle between naps",
        icon = Icons.Default.Schedule,
        iconColor = WakeMint,
        selected = selectedMetric == ReviewMetric.WAKE_WINDOW,
        onClick = { onSelectMetric(ReviewMetric.WAKE_WINDOW) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = stringResource(R.string.stat_feedings),
        mainValue = if (summary.nightFeedingCount > 0) {
            stringResource(R.string.stat_night_feedings, summary.feedingCount, summary.nightFeedingCount)
        } else {
            "${summary.feedingCount} sessions"
        },
        subValue = if (summary.totalBottleMl > 0) "${summary.totalNursingDurationMinutes}m total • ${summary.totalBottleMl} ml bottle" else "${summary.totalNursingDurationMinutes}m total nursing time",
        icon = Icons.Default.Restaurant,
        iconColor = NursingPink,
        selected = selectedMetric == ReviewMetric.FEEDINGS,
        onClick = { onSelectMetric(ReviewMetric.FEEDINGS) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = stringResource(R.string.stat_diapers),
        mainValue = "${summary.diaperPeeCount + summary.diaperPooCount} changes",
        subValue = "💧 Wet: ${summary.diaperPeeCount}  |  💩 Dirty: ${summary.diaperPooCount}",
        icon = Icons.Default.WaterDrop,
        iconColor = DiaperPeeCyan,
        selected = selectedMetric == ReviewMetric.DIAPERS,
        onClick = { onSelectMetric(ReviewMetric.DIAPERS) }
    )

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun AggregatedMetricsView(
    title: String,
    daysCount: Int,
    avgSleepMin: Long,
    avgDaySleepMin: Long,
    avgNightSleepMin: Long,
    avgWakeMin: Long,
    avgFeeds: Float,
    avgBottleMl: Float,
    avgPee: Float,
    avgPoo: Float,
    dailySummaries: List<DailySummary>,
    selectedPeriod: ReviewPeriod,
    selectedMetric: ReviewMetric,
    onSelectMetric: (ReviewMetric) -> Unit
) {
    Text(
        text = "$title ($daysCount days average)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    val totalHours = avgSleepMin / 60
    val totalMins = avgSleepMin % 60
    val dayHours = avgDaySleepMin / 60
    val dayMins = avgDaySleepMin % 60
    val nightHours = avgNightSleepMin / 60
    val nightMins = avgNightSleepMin % 60

    MetricCard(
        title = "Avg Sleep / Day",
        mainValue = "${totalHours}h ${totalMins}m",
        subValue = "Day: ${dayHours}h ${dayMins}m • Night: ${nightHours}h ${nightMins}m",
        icon = Icons.Default.Hotel,
        iconColor = SleepIndigo,
        selected = selectedMetric == ReviewMetric.SLEEP,
        onClick = { onSelectMetric(ReviewMetric.SLEEP) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = "Avg Wake Window",
        mainValue = "$avgWakeMin min",
        subValue = "Average activity cycle between naps",
        icon = Icons.Default.Schedule,
        iconColor = WakeMint,
        selected = selectedMetric == ReviewMetric.WAKE_WINDOW,
        onClick = { onSelectMetric(ReviewMetric.WAKE_WINDOW) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = "Avg Feedings / Day",
        mainValue = String.format(Locale.getDefault(), "%.1f times", avgFeeds),
        subValue = if (avgBottleMl > 0) String.format(Locale.getDefault(), "%.0f ml bottle/day", avgBottleMl) else "Nursing sessions per day",
        icon = Icons.Default.Restaurant,
        iconColor = NursingPink,
        selected = selectedMetric == ReviewMetric.FEEDINGS,
        onClick = { onSelectMetric(ReviewMetric.FEEDINGS) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = "Avg Diapers / Day",
        mainValue = String.format(Locale.getDefault(), "%.1f changes", avgPee + avgPoo),
        subValue = String.format(Locale.getDefault(), "💧 Wet: %.1f  |  💩 Dirty: %.1f", avgPee, avgPoo),
        icon = Icons.Default.WaterDrop,
        iconColor = DiaperPeeCyan,
        selected = selectedMetric == ReviewMetric.DIAPERS,
        onClick = { onSelectMetric(ReviewMetric.DIAPERS) }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Dynamic Trend Chart (Graph for Monthly, Bar Chart for Weekly)
    if (dailySummaries.isNotEmpty()) {
        val config = when (selectedMetric) {
            ReviewMetric.SLEEP -> MetricTrendConfig(
                title = "Sleep Trend (Hours / Day)",
                color = SleepIndigo,
                unit = "h",
                extractor = { it.totalSleepMinutes / 60f }
            )
            ReviewMetric.WAKE_WINDOW -> MetricTrendConfig(
                title = "Wake Window Trend (Minutes / Day)",
                color = WakeMint,
                unit = "m",
                extractor = { it.averageWakeWindowMinutes.toFloat() }
            )
            ReviewMetric.FEEDINGS -> MetricTrendConfig(
                title = "Feedings Trend (Sessions / Day)",
                color = NursingPink,
                unit = "",
                extractor = { it.feedingCount.toFloat() }
            )
            ReviewMetric.DIAPERS -> MetricTrendConfig(
                title = "Diapers Trend (Changes / Day)",
                color = DiaperPeeCyan,
                unit = "",
                extractor = { (it.diaperPeeCount + it.diaperPooCount).toFloat() }
            )
        }

        if (selectedPeriod == ReviewPeriod.MONTHLY) {
            TrendLineGraph(
                title = config.title,
                data = dailySummaries.map { it.dateEpochMillis to config.extractor(it) },
                lineColor = config.color,
                unit = config.unit
            )
        } else {
            TrendBarChart(
                title = config.title,
                summaries = dailySummaries.takeLast(7),
                barColor = config.color,
                unit = config.unit,
                valueExtractor = config.extractor
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun TrendLineGraph(
    title: String,
    data: List<Pair<Long, Float>>,
    lineColor: Color,
    unit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val avgVal = if (data.isNotEmpty()) data.map { it.second }.average() else 0.0
                Text(
                    text = "Avg: " + String.format(Locale.getDefault(), "%.1f%s", avgVal, unit),
                    style = MaterialTheme.typography.labelSmall,
                    color = lineColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (data.size >= 2) {
                val maxVal = maxOf(1f, data.maxOf { it.second })
                val minVal = data.minOf { it.second }.coerceAtLeast(0f)
                val range = if (maxVal == minVal) 1f else (maxVal - minVal)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 16f
                        val paddingTop = 12f
                        val chartHeight = height - paddingTop - paddingBottom
                        val stepX = width / (data.size - 1).coerceAtLeast(1)

                        // Reference horizontal grid lines
                        val yTop = paddingTop
                        val yMid = paddingTop + chartHeight / 2f
                        val yBottom = paddingTop + chartHeight

                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(0f, yTop),
                            end = Offset(width, yTop),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, yMid),
                            end = Offset(width, yMid),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(0f, yBottom),
                            end = Offset(width, yBottom),
                            strokeWidth = 1f
                        )

                        // Build smooth line path and gradient area
                        val points = data.mapIndexed { idx, pair ->
                            val x = idx * stepX
                            val normalizedY = (pair.second - minVal) / range
                            val y = yBottom - (normalizedY * chartHeight)
                            Offset(x, y)
                        }

                        val linePath = Path()
                        val fillPath = Path()

                        if (points.isNotEmpty()) {
                            linePath.moveTo(points[0].x, points[0].y)
                            fillPath.moveTo(points[0].x, yBottom)
                            fillPath.lineTo(points[0].x, points[0].y)

                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }

                            fillPath.lineTo(points.last().x, yBottom)
                            fillPath.close()

                            // Draw gradient fill under the curve
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lineColor.copy(alpha = 0.35f),
                                        lineColor.copy(alpha = 0.02f)
                                    ),
                                    startY = yTop,
                                    endY = yBottom
                                )
                            )

                            // Draw smooth curve stroke
                            drawPath(
                                path = linePath,
                                color = lineColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )

                            // Draw small circular points
                            points.forEach { pt ->
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = lineColor,
                                    radius = 1.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // X-Axis Date Labels along the bottom (5 labels)
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                val labelIndices = listOf(0, data.size / 4, data.size / 2, (3 * data.size) / 4, data.size - 1).distinct()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labelIndices.forEach { idx ->
                        Text(
                            text = dateFormat.format(data[idx].first),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Not enough data recorded yet for monthly graph.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendBarChart(
    title: String,
    summaries: List<DailySummary>,
    barColor: Color,
    unit: String,
    valueExtractor: (DailySummary) -> Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val dayFormat = SimpleDateFormat("E", Locale.getDefault())
            val maxVal = maxOf(1f, summaries.maxOfOrNull { valueExtractor(it) } ?: 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                summaries.forEach { item ->
                    val value = valueExtractor(item)
                    val ratio = (value / maxVal).coerceIn(0.05f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = if (unit == "h") String.format(Locale.getDefault(), "%.0fh", value) else String.format(Locale.getDefault(), "%.0f%s", value, unit),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(ratio)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayFormat.format(item.dateEpochMillis),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    mainValue: String,
    subValue: String,
    icon: ImageVector,
    iconColor: Color,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) iconColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(2.dp, iconColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = if (selected) 0.3f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) iconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selected) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
