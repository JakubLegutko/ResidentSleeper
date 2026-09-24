package com.residentsleeper.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import com.residentsleeper.domain.LittleOnesSleepScheduleDatabase
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.EventType
import com.residentsleeper.ui.components.EditEventDialog
import java.util.concurrent.TimeUnit
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
import androidx.compose.ui.platform.LocalContext
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
import com.residentsleeper.ui.theme.NursingPink
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
    var editingEvent by remember { mutableStateOf<BabyEvent?>(null) }

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
                                contentDescription = stringResource(R.string.content_desc_switch_profile),
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_previous_day))
                }
                Text(
                    text = if (state.isToday) "${stringResource(R.string.label_today)}, ${dateFormatter.format(state.selectedDayStart)}" else dateFormatter.format(state.selectedDayStart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { viewModel.nextDay() },
                    enabled = !state.isToday
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.content_desc_next_day))
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
                dayStartHour = state.activeProfile.dayStartHour,
                sizeDp = 324.dp,
                strokeWidthDp = 38.dp
            )

            // Color Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
            ) {
                ChartLegendItem(color = SleepIndigo, label = stringResource(R.string.legend_sleep))
                ChartLegendItem(color = WakeMint, label = stringResource(R.string.legend_activity))
                ChartLegendItem(color = NursingPink, label = stringResource(R.string.legend_nursing))
                ChartLegendItem(color = DiaperPeeCyan, label = stringResource(R.string.legend_diaper))
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
                    subtitle = if (isSleeping) stringResource(R.string.subtitle_tap_to_wake) else stringResource(R.string.subtitle_tap_to_sleep),
                    icon = Icons.Default.Hotel,
                    containerColor = if (isSleeping) SleepIndigo else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSleeping) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onSleepButtonClick() },
                    onLongClick = { showSleepTimeAdjustDialog = true }
                )

                // Nursing Button (Toggle with details dialog)
                val isNursing = state.ongoingNursing != null
                val nursingSubtitle = if (isNursing) {
                    stringResource(R.string.subtitle_tap_to_finish)
                } else if (state.feedingState.isOptionalNightFeed) {
                    stringResource(R.string.btn_nursing_optional_night_subtitle)
                } else {
                    stringResource(R.string.subtitle_breast_bottle)
                }
                ActionButtonCard(
                    title = if (isNursing) stringResource(R.string.btn_nursing_end) else stringResource(R.string.btn_nursing_start),
                    subtitle = nursingSubtitle,
                    icon = Icons.Default.Restaurant,
                    containerColor = if (isNursing) NursingPink else MaterialTheme.colorScheme.surfaceVariant,
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
                            text = stringResource(R.string.label_target_wake),
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
                            text = stringResource(R.string.label_feed_interval),
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
                            text = stringResource(R.string.label_today_logs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.label_items_count, state.events.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Little Ones Sleep Recommendation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SleepIndigo.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, SleepIndigo.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.wakeWindowState.recommendationTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SleepIndigo
                        )
                        Text(
                            text = stringResource(R.string.label_nap_progress, state.wakeWindowState.napsCompletedToday, state.wakeWindowState.targetNapsToday),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.label_target_duration, state.wakeWindowState.recommendedSleepDuration),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.wakeWindowState.recommendationReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Little Ones Pediatric Insight / Trivia Card
            val schedule = LittleOnesSleepScheduleDatabase.getScheduleForAge(state.wakeWindowState.babyAgeWeeks)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.label_insight, schedule.getLocalizedAgeBracket(LocalContext.current)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WakeMint
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.wakeWindowState.triviaTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day's Timeline & Entries List
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_timeline),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.timeline_items_count, state.events.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.events.isEmpty()) {
                        Text(
                            text = stringResource(R.string.timeline_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                        val sortedEvents = remember(state.events) {
                            state.events.sortedByDescending { it.startTime }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sortedEvents.forEach { event ->
                                TimelineEntryRow(
                                    event = event,
                                    selectedDayStart = state.selectedDayStart,
                                    timeFormat = timeFormat,
                                    onEditClick = { editingEvent = event }
                                )
                            }
                        }
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
            title = if (ongoing != null) stringResource(R.string.dialog_adjust_wake_up) else stringResource(R.string.dialog_adjust_sleep_start),
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
            title = if (isNursing) stringResource(R.string.dialog_adjust_nursing_end) else stringResource(R.string.dialog_adjust_nursing_start),
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
            title = if (diaperType == DiaperType.PEE) stringResource(R.string.dialog_adjust_pee_time) else stringResource(R.string.dialog_adjust_poo_time),
            onDismiss = { showDiaperTimeAdjustDialog = null },
            onTimeSelected = { adjustedTimestamp ->
                viewModel.onDiaperClick(diaperType, adjustedTimestamp)
                showDiaperTimeAdjustDialog = null
            }
        )
    }

    val currentEditing = editingEvent
    if (currentEditing != null) {
        EditEventDialog(
            event = currentEditing,
            onDismiss = { editingEvent = null },
            onSave = { updated ->
                viewModel.updateEvent(updated)
                editingEvent = null
            },
            onDelete = { deleted ->
                viewModel.deleteEvent(deleted)
                editingEvent = null
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
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TimelineEntryRow(
    event: BabyEvent,
    selectedDayStart: Long,
    timeFormat: SimpleDateFormat,
    onEditClick: () -> Unit
) {
    val isOvernightFromYesterday = event.startTime < selectedDayStart

    val (icon, tintColor, title) = when (event.type) {
        EventType.SLEEP -> {
            val titleText = if (event.endTime == null) stringResource(R.string.label_sleep_ongoing) else stringResource(R.string.legend_sleep)
            Triple(Icons.Default.Hotel, SleepIndigo, titleText)
        }
        EventType.NURSING -> {
            val titleText = when (event.nursingType) {
                NursingType.LEFT_BREAST -> stringResource(R.string.nursing_left_breast)
                NursingType.RIGHT_BREAST -> stringResource(R.string.nursing_right_breast)
                NursingType.BOTH_BREASTS -> stringResource(R.string.nursing_both_breasts)
                NursingType.BOTTLE -> {
                    if (event.amountMl != null) stringResource(R.string.label_bottle_ml, event.amountMl) else stringResource(R.string.nursing_bottle)
                }
                null -> stringResource(R.string.legend_nursing)
            }
            Triple(Icons.Default.Restaurant, NursingPink, titleText)
        }
        EventType.DIAPER -> {
            val (iconD, tintD, titleText) = when (event.diaperType) {
                DiaperType.PEE -> Triple(Icons.Default.WaterDrop, DiaperPeeCyan, stringResource(R.string.btn_diaper_pee))
                DiaperType.POO -> Triple(Icons.Default.Check, DiaperPooWarm, stringResource(R.string.btn_diaper_poo))
                DiaperType.BOTH -> Triple(Icons.Default.Check, DiaperPooWarm, stringResource(R.string.btn_diaper_both))
                null -> Triple(Icons.Default.WaterDrop, DiaperPeeCyan, stringResource(R.string.legend_diaper))
            }
            Triple(iconD, tintD, titleText)
        }
    }

    val timeText = if (event.type == EventType.DIAPER) {
        timeFormat.format(event.startTime)
    } else if (event.endTime != null) {
        val durationMins = TimeUnit.MILLISECONDS.toMinutes(maxOf(0L, event.endTime - event.startTime))
        val h = durationMins / 60
        val m = durationMins % 60
        val durStr = if (h > 0) "${h}h ${m}m" else "${m}m"
        "${timeFormat.format(event.startTime)} - ${timeFormat.format(event.endTime)} ($durStr)"
    } else {
        "${timeFormat.format(event.startTime)} (${stringResource(R.string.entry_ongoing)})"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tintColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isOvernightFromYesterday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SleepIndigo.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stringResource(R.string.timeline_overnight_tag),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleepIndigo,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!event.note.isNullOrBlank()) {
                        Text(
                            text = "📝 ${event.note}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.btn_edit_entry),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

