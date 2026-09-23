package com.residentsleeper.ui.review

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.residentsleeper.R
import com.residentsleeper.domain.DailySummary
import com.residentsleeper.ui.theme.DiaperPeeCyan
import com.residentsleeper.ui.theme.DiaperPooWarm
import com.residentsleeper.ui.theme.NursingPink
import com.residentsleeper.ui.theme.SleepIndigo
import com.residentsleeper.ui.theme.WakeMint
import java.text.SimpleDateFormat
import java.util.Locale

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
                        DailyMetricsView(summary)
                    }
                } else {
                    val agg = state.aggregatedReview
                    if (agg != null) {
                        AggregatedMetricsView(agg.title, agg.daysCount, agg.avgTotalSleepMinutesPerDay, agg.avgDaySleepMinutesPerDay, agg.avgNightSleepMinutesPerDay, agg.avgWakeWindowMinutes, agg.avgFeedingsPerDay, agg.avgBottleMlPerDay, agg.avgDiaperPeePerDay, agg.avgDiaperPooPerDay, agg.dailySummaries)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMetricsView(summary: DailySummary) {
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
        iconColor = SleepIndigo
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = stringResource(R.string.stat_wake_window),
        mainValue = "${summary.averageWakeWindowMinutes} min",
        subValue = "Average activity cycle between naps",
        icon = Icons.Default.Schedule,
        iconColor = WakeMint
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = stringResource(R.string.stat_feedings),
        mainValue = "${summary.feedingCount} sessions",
        subValue = if (summary.totalBottleMl > 0) "${summary.totalNursingDurationMinutes}m total • ${summary.totalBottleMl} ml bottle" else "${summary.totalNursingDurationMinutes}m total nursing time",
        icon = Icons.Default.Restaurant,
        iconColor = NursingPink
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = stringResource(R.string.stat_diapers),
        mainValue = "${summary.diaperPeeCount + summary.diaperPooCount} changes",
        subValue = "💧 Wet: ${summary.diaperPeeCount}  |  💩 Dirty: ${summary.diaperPooCount}",
        icon = Icons.Default.WaterDrop,
        iconColor = DiaperPeeCyan
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
    dailySummaries: List<DailySummary>
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
        iconColor = SleepIndigo
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = "Avg Wake Window",
        mainValue = "$avgWakeMin min",
        subValue = "Average activity cycle between naps",
        icon = Icons.Default.Schedule,
        iconColor = WakeMint
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = "Avg Feedings / Day",
        mainValue = String.format(Locale.getDefault(), "%.1f times", avgFeeds),
        subValue = if (avgBottleMl > 0) String.format(Locale.getDefault(), "%.0f ml bottle/day", avgBottleMl) else "Nursing sessions per day",
        icon = Icons.Default.Restaurant,
        iconColor = NursingPink
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetricCard(
        title = "Avg Diapers / Day",
        mainValue = String.format(Locale.getDefault(), "%.1f changes", avgPee + avgPoo),
        subValue = String.format(Locale.getDefault(), "💧 Wet: %.1f  |  💩 Dirty: %.1f", avgPee, avgPoo),
        icon = Icons.Default.WaterDrop,
        iconColor = DiaperPeeCyan
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Sleep Trend Chart (Bar Visualization)
    if (dailySummaries.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sleep Trend (Hours / Day)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val dayFormat = SimpleDateFormat("E", Locale.getDefault())
                val maxSleep = maxOf(1f, dailySummaries.maxOf { it.totalSleepMinutes }.toFloat())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailySummaries.takeLast(7).forEach { item ->
                        val ratio = (item.totalSleepMinutes.toFloat() / maxSleep).coerceIn(0.05f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = "${item.totalSleepMinutes / 60}h",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(SleepIndigo)
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

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun MetricCard(
    title: String,
    mainValue: String,
    subValue: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    .background(iconColor.copy(alpha = 0.15f)),
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
