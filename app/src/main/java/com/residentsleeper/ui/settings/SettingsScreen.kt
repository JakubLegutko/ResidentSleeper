package com.residentsleeper.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.residentsleeper.R
import com.residentsleeper.domain.WakeWindowCalculator
import com.residentsleeper.ui.components.ProfileSwitcherDialog
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDayStartHourDialog by remember { mutableStateOf(false) }
    var calendarMenuExpanded by remember { mutableStateOf(false) }
    var showProfileSwitcher by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var importReplaceOption by remember { mutableStateOf(false) } // false = merge, true = replace

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadProfile()
    }

    // File picker for import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                reader.close()
                pendingImportJson = jsonString
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.import_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_settings), fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Child Profiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Child Profiles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showProfileSwitcher = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Manage")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.allProfiles.forEach { profile ->
                        val isActive = profile.id == state.activeProfile.id
                        val ageWeeks = WakeWindowCalculator.calculateAgeInWeeks(profile.birthTimestamp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.switchProfile(profile.id) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = "(Active)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "$ageWeeks weeks old • born ${dateFormatter.format(profile.birthTimestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (state.allProfiles.size > 1 && !isActive) {
                                IconButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Active Profile Details
            Text(
                text = "Active Child Details: ${state.activeProfile.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.activeProfile.name,
                        onValueChange = { viewModel.updateActiveProfileName(it) },
                        label = { Text("Child Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.settings_birth_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dateFormatter.format(state.activeProfile.birthTimestamp))
                    }
                }
            }

            // Section 3: Cycle Calculations
            Text(
                text = "Cycle Calculations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_wake_window),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.activeProfile.customWakeWindowMinutes == null,
                            onClick = { viewModel.updateWakeWindow(null) },
                            label = { Text("Auto (Age)") }
                        )
                        FilterChip(
                            selected = state.activeProfile.customWakeWindowMinutes == 60,
                            onClick = { viewModel.updateWakeWindow(60) },
                            label = { Text("60 min") }
                        )
                        FilterChip(
                            selected = state.activeProfile.customWakeWindowMinutes == 75,
                            onClick = { viewModel.updateWakeWindow(75) },
                            label = { Text("75 min") }
                        )
                        FilterChip(
                            selected = state.activeProfile.customWakeWindowMinutes == 90,
                            onClick = { viewModel.updateWakeWindow(90) },
                            label = { Text("90 min") }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.settings_feeding_interval),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.activeProfile.feedingIntervalMinutes == 120,
                            onClick = { viewModel.updateFeedingInterval(120) },
                            label = { Text("2.0 hours") }
                        )
                        FilterChip(
                            selected = state.activeProfile.feedingIntervalMinutes == 150,
                            onClick = { viewModel.updateFeedingInterval(150) },
                            label = { Text("2.5 hours") }
                        )
                        FilterChip(
                            selected = state.activeProfile.feedingIntervalMinutes == 180,
                            onClick = { viewModel.updateFeedingInterval(180) },
                            label = { Text("3.0 hours") }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.settings_day_start_hour),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { showDayStartHourDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "%02d:00%s",
                                state.activeProfile.dayStartHour,
                                if (state.activeProfile.dayStartHour == 7) " (Default)" else ""
                            )
                        )
                    }
                }
            }

            // Section 4: Alerts & Google Calendar Sync
            Text(
                text = "Alerts & Calendar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_notifications),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_notify_advance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.activeProfile.enablePushNotifications,
                            onCheckedChange = { viewModel.togglePushNotifications(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_calendar_sync),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add reminders to Google Calendar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.activeProfile.enableCalendarSync,
                            onCheckedChange = { enabled ->
                                if (enabled && !state.hasCalendarPermission) {
                                    calendarPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                } else {
                                    viewModel.toggleCalendarSync(enabled)
                                }
                            }
                        )
                    }

                    if (state.activeProfile.enableCalendarSync && state.hasCalendarPermission) {
                        val selectedCal = state.availableCalendars.find { it.id == state.activeProfile.selectedCalendarId }
                        ExposedDropdownMenuBox(
                            expanded = calendarMenuExpanded,
                            onExpandedChange = { calendarMenuExpanded = !calendarMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCal?.let { "${it.displayName} (${it.accountName})" } ?: "Select a Google Calendar",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.settings_choose_calendar)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calendarMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = calendarMenuExpanded,
                                onDismissRequest = { calendarMenuExpanded = false }
                            ) {
                                state.availableCalendars.forEach { calItem ->
                                    DropdownMenuItem(
                                        text = { Text("${calItem.displayName} (${calItem.accountName})") },
                                        onClick = {
                                            viewModel.selectCalendar(calItem.id)
                                            calendarMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Data Portability & Backup
            Text(
                text = stringResource(R.string.settings_data_portability),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export JSON
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.getJsonExportData()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    putExtra(Intent.EXTRA_TITLE, "ResidentSleeper_Backup.json")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export JSON Backup"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.btn_export_json))
                    }

                    // Export CSV
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val csv = viewModel.getCsvExportData()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    putExtra(Intent.EXTRA_TITLE, "ResidentSleeper_Events.csv")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Spreadsheet CSV"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.btn_export_csv))
                    }

                    // Import Backup
                    OutlinedButton(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.btn_import_json))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Profile Switcher Dialog
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

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.activeProfile.birthTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.updateBirthDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Import Options Dialog (Merge vs Replace)
    pendingImportJson?.let { jsonStr ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text(stringResource(R.string.import_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.import_dialog_message))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { importReplaceOption = false }
                    ) {
                        RadioButton(
                            selected = !importReplaceOption,
                            onClick = { importReplaceOption = false }
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.import_merge))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { importReplaceOption = true }
                    ) {
                        RadioButton(
                            selected = importReplaceOption,
                            onClick = { importReplaceOption = true }
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.import_replace))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackupData(jsonStr, importReplaceOption) { success ->
                            val msg = if (success) context.getString(R.string.import_success) else context.getString(R.string.import_failed)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            pendingImportJson = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Day Start Hour Picker Dialog
    if (showDayStartHourDialog) {
        AlertDialog(
            onDismissRequest = { showDayStartHourDialog = false },
            title = { Text(stringResource(R.string.settings_day_start_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (rowStart in 0 until 24 step 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (h in rowStart until (rowStart + 4)) {
                                val isSelected = state.activeProfile.dayStartHour == h
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateDayStartHour(h)
                                        showDayStartHourDialog = false
                                    },
                                    label = {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d:00", h),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDayStartHourDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
