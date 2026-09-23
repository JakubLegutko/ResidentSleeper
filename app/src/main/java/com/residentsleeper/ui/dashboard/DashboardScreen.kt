package com.residentsleeper.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.residentsleeper.R
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.NursingType
import com.residentsleeper.domain.WakeWindowCalculator
import com.residentsleeper.ui.chart.Clock24HourChart
import com.residentsleeper.ui.components.NursingDetailsDialog
import com.residentsleeper.ui.components.ProfileSwitcherDialog
import com.residentsleeper.ui.components.TimeAdjustDialog
import com.residentsleeper.ui.theme.DiaperPeeCyan
import com.residentsleeper.ui.theme.DiaperPooWarm
import com.residentsleeper.ui.theme.NursingTeal
import com.residentsleeper.ui.theme.SleepIndigo
import com.residentsleeper.ui.theme.WakeMint
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToReviews: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    var showProfileSwitcher by remember { mutableStateOf(false) }
    var showSleepTimeAdjustDialog by remember { mutableStateOf(false) }
    var showNursingDialog by remember { mutableStateOf(false) }
    var showNursingTimeAdjustDialog by remember { mutableStateOf(false) }
    var pendingNursingAdjustedTime by remember { mutableStateOf<Long?>(null) }
    var showDiaperTimeAdjustDialog by remember { mutableStateOf<DiaperType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val ageWeeks = WakeWindowCalculator.calculateAgeInWeeks(state.activeProfile.birthTimestamp)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { showProfileSwitcher = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "👶 ${state.activeProfile.name} (${ageWeeks}w)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToReviews) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.nav_review)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Date Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousDay() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                }
                Text(
                    text = if (state.isToday) "Today, ${dateFormatter.format(state.selectedDayStart)}" else dateFormatter.format(state.selectedDayStart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { viewModel.nextDay() },
                    enabled = !state.isToday
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24-Hour Donut Chart
            Clock24HourChart(
                dayStartMillis = state.selectedDayStart,
                dayEndMillis = state.selectedDayEnd,
                events = state.events,
                wakeState = state.wakeWindowState,
                feedingState = state.feedingState,
                sizeDp = 280.dp,
                strokeWidthDp = 32.dp
            )

            // Color Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
            ) {
                ChartLegendItem(color = SleepIndigo, label = "Sleep")
                ChartLegendItem(color = WakeMint, label = "Activity")
                ChartLegendItem(color = NursingTeal, label = "Nursing")
                ChartLegendItem(color = DiaperPeeCyan, label = "Diaper")
            }

            // 4 ACTION BUTTONS GRID
            // Row 1: Sleep & Nursing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleep Button (Toggle with Now / Adjust)
                val isSleeping = state.ongoingSleep != null
                ActionButtonCard(
                    title = if (isSleeping) stringResource(R.string.btn_sleep_end) else stringResource(R.string.btn_sleep_start),
                    subtitle = if (isSleeping) "Tap to wake" else "Tap to sleep",
                    icon = Icons.Default.Hotel,
                    containerColor = if (isSleeping) SleepIndigo else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSleeping) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onSleepButtonClick() },
                    onLongClick = { showSleepTimeAdjustDialog = true }
                )

                // Nursing Button (Toggle with details dialog)
                val isNursing = state.ongoingNursing != null
                ActionButtonCard(
                    title = if (isNursing) stringResource(R.string.btn_nursing_end) else stringResource(R.string.btn_nursing_start),
                    subtitle = if (isNursing) "Tap to finish" else "Breast / Bottle",
                    icon = Icons.Default.Restaurant,
                    containerColor = if (isNursing) NursingTeal else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isNursing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isNursing) {
                            viewModel.onNursingButtonClick()
                        } else {
                            showNursingDialog = true
                        }
                    },
                    onLongClick = { showNursingTimeAdjustDialog = true }
                )
            }


            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Diaper Pee & Diaper Poo (One-shot buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Diaper Pee One-shot
                ActionButtonCard(
                    title = stringResource(R.string.btn_diaper_pee),
                    subtitle = null,
                    icon = Icons.Default.WaterDrop,
                    containerColor = DiaperPeeCyan.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDiaperClick(DiaperType.PEE) },
                    onLongClick = { showDiaperTimeAdjustDialog = DiaperType.PEE }
                )

                // Diaper Poo One-shot
                ActionButtonCard(
                    title = stringResource(R.string.btn_diaper_poo),
                    subtitle = null,
                    icon = Icons.Default.Check,
                    containerColor = DiaperPooWarm.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onDiaperClick(DiaperType.POO) },
                    onLongClick = { showDiaperTimeAdjustDialog = DiaperType.POO }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Status Ticker Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Target Wake",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.wakeWindowState.recommendedWakeWindowMinutes} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Feed Interval",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.activeProfile.feedingIntervalMinutes} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Today Logs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.events.size} items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogs
    if (showProfileSwitcher) {
        ProfileSwitcherDialog(
            currentProfileId = state.activeProfile.id,
            profiles = state.allProfiles,
            onDismiss = { showProfileSwitcher = false },
            onSelectProfile = { profileId ->
                viewModel.switchProfile(profileId)
                showProfileSwitcher = false
            },
            onAddProfile = { name, birthDate ->
                viewModel.addProfile(name, birthDate)
                showProfileSwitcher = false
            }
        )
    }

    if (showSleepTimeAdjustDialog) {
        val ongoing = state.ongoingSleep
        TimeAdjustDialog(
            title = if (ongoing != null) "Adjust Wake Up Time" else "Adjust Sleep Start Time",
            onDismiss = { showSleepTimeAdjustDialog = false },
            onTimeSelected = { adjustedTimestamp ->
                showSleepTimeAdjustDialog = false
                viewModel.onSleepButtonClick(adjustedTimestamp)
            }
        )
    }

    if (showNursingTimeAdjustDialog) {
        val isNursing = state.ongoingNursing != null
        TimeAdjustDialog(
            title = if (isNursing) "Adjust Nursing End Time" else "Adjust Nursing Start Time",
            onDismiss = { showNursingTimeAdjustDialog = false },
            onTimeSelected = { adjustedTimestamp ->
                showNursingTimeAdjustDialog = false
                if (isNursing) {
                    viewModel.onNursingButtonClick(adjustedTime = adjustedTimestamp)
                } else {
                    pendingNursingAdjustedTime = adjustedTimestamp
                    showNursingDialog = true
                }
            }
        )
    }

    if (showNursingDialog) {
        NursingDetailsDialog(
            onDismiss = {
                showNursingDialog = false
                pendingNursingAdjustedTime = null
            },
            onConfirm = { nursingType, amountMl ->
                val time = pendingNursingAdjustedTime
                pendingNursingAdjustedTime = null
                showNursingDialog = false
                viewModel.onNursingButtonClick(nursingType, amountMl, adjustedTime = time)
            }
        )
    }

    showDiaperTimeAdjustDialog?.let { diaperType ->
        TimeAdjustDialog(
            title = if (diaperType == DiaperType.PEE) "Adjust Pee Time" else "Adjust Poo Time",
            onDismiss = { showDiaperTimeAdjustDialog = null },
            onTimeSelected = { adjustedTimestamp ->
                viewModel.onDiaperClick(diaperType, adjustedTimestamp)
                showDiaperTimeAdjustDialog = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionButtonCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .androidx.compose.foundation.background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

