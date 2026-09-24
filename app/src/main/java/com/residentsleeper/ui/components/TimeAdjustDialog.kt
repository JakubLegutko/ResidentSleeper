package com.residentsleeper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.residentsleeper.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimeAdjustDialog(
    title: String,
    initialTimestamp: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onTimeSelected: (Long) -> Unit
) {
    var selectedTimestamp by remember { mutableStateOf(initialTimestamp) }
    var showCustomPicker by remember { mutableStateOf(false) }

    val cal = remember(selectedTimestamp) {
        Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = true
    )

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dialog_selected_time, timeFormatter.format(selectedTimestamp)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = stringResource(R.string.dialog_quick_offsets),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedFilterChip(
                        selected = selectedTimestamp == System.currentTimeMillis(),
                        onClick = { selectedTimestamp = System.currentTimeMillis() },
                        label = { Text(stringResource(R.string.time_now)) }
                    )
                    ElevatedFilterChip(
                        selected = false,
                        onClick = { selectedTimestamp = System.currentTimeMillis() - 10 * 60 * 1000 },
                        label = { Text(stringResource(R.string.time_minus_10m)) }
                    )
                    ElevatedFilterChip(
                        selected = false,
                        onClick = { selectedTimestamp = System.currentTimeMillis() - 20 * 60 * 1000 },
                        label = { Text(stringResource(R.string.time_minus_20m)) }
                    )
                    ElevatedFilterChip(
                        selected = false,
                        onClick = { selectedTimestamp = System.currentTimeMillis() - 30 * 60 * 1000 },
                        label = { Text(stringResource(R.string.time_minus_30m)) }
                    )
                    ElevatedFilterChip(
                        selected = false,
                        onClick = { selectedTimestamp = System.currentTimeMillis() - 60 * 60 * 1000 },
                        label = { Text(stringResource(R.string.time_minus_1h)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showCustomPicker = !showCustomPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showCustomPicker) stringResource(R.string.dialog_hide_custom_clock) else stringResource(R.string.time_custom))
                }

                if (showCustomPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TimePicker(state = timePickerState)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTimestamp = if (showCustomPicker) {
                        Calendar.getInstance().apply {
                            timeInMillis = selectedTimestamp
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }.timeInMillis
                    } else {
                        selectedTimestamp
                    }
                    onTimeSelected(finalTimestamp)
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
