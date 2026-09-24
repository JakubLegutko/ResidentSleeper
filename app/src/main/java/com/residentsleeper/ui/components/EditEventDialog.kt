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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.residentsleeper.R
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.DiaperType
import com.residentsleeper.data.model.EventType
import com.residentsleeper.data.model.NursingType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEventDialog(
    event: BabyEvent,
    onDismiss: () -> Unit,
    onSave: (BabyEvent) -> Unit,
    onDelete: (BabyEvent) -> Unit
) {
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var isOngoing by remember { mutableStateOf(event.endTime == null && event.type != EventType.DIAPER) }
    var nursingType by remember { mutableStateOf(event.nursingType ?: NursingType.LEFT_BREAST) }
    var diaperType by remember { mutableStateOf(event.diaperType ?: DiaperType.PEE) }
    var amountMlText by remember { mutableStateOf(event.amountMl?.toString() ?: "90") }
    var note by remember { mutableStateOf(event.note ?: "") }

    var editingTimeMode by remember { mutableStateOf<String?>(null) } // "START" or "END" or null
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_confirm_title)) },
            text = { Text(stringResource(R.string.dialog_delete_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(event)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete_entry))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (editingTimeMode != null) {
        val targetTimestamp = if (editingTimeMode == "START") startTime else (endTime ?: System.currentTimeMillis())
        val cal = remember(targetTimestamp) {
            Calendar.getInstance().apply { timeInMillis = targetTimestamp }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { editingTimeMode = null },
            title = {
                Text(
                    if (editingTimeMode == "START") stringResource(R.string.entry_start_time)
                    else stringResource(R.string.entry_end_time)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = targetTimestamp
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                        }
                        if (editingTimeMode == "START") {
                            startTime = newCal.timeInMillis
                            if (endTime != null && endTime!! < startTime) {
                                endTime = startTime
                            }
                        } else {
                            val newEnd = newCal.timeInMillis
                            endTime = if (newEnd < startTime) startTime else newEnd
                            isOngoing = false
                        }
                        editingTimeMode = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTimeMode = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dialog_edit_entry_title),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButtonSafeDelete(onClick = { showDeleteConfirm = true })
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Time adjustment section
                Text(
                    text = stringResource(R.string.entry_start_time),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { editingTimeMode = "START" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(timeFormatter.format(startTime))
                }

                if (event.type != EventType.DIAPER) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.entry_end_time),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { editingTimeMode = "END" },
                            enabled = !isOngoing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (isOngoing) stringResource(R.string.entry_ongoing)
                                else timeFormatter.format(endTime ?: startTime)
                            )
                        }
                        FilterChip(
                            selected = isOngoing,
                            onClick = {
                                isOngoing = !isOngoing
                                if (!isOngoing && endTime == null) {
                                    endTime = maxOf(startTime, System.currentTimeMillis())
                                }
                            },
                            label = { Text(stringResource(R.string.entry_ongoing)) }
                        )
                    }

                    // Duration display if both start and end exist
                    if (!isOngoing && endTime != null && endTime!! >= startTime) {
                        val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(endTime!! - startTime)
                        val hours = durationMinutes / 60
                        val mins = durationMinutes % 60
                        val durStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.entry_duration, durStr),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Type details
                if (event.type == EventType.NURSING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.dialog_method_side),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = nursingType == NursingType.LEFT_BREAST,
                            onClick = { nursingType = NursingType.LEFT_BREAST },
                            label = { Text(stringResource(R.string.nursing_left_breast)) }
                        )
                        FilterChip(
                            selected = nursingType == NursingType.RIGHT_BREAST,
                            onClick = { nursingType = NursingType.RIGHT_BREAST },
                            label = { Text(stringResource(R.string.nursing_right_breast)) }
                        )
                        FilterChip(
                            selected = nursingType == NursingType.BOTH_BREASTS,
                            onClick = { nursingType = NursingType.BOTH_BREASTS },
                            label = { Text(stringResource(R.string.nursing_both_breasts)) }
                        )
                        FilterChip(
                            selected = nursingType == NursingType.BOTTLE,
                            onClick = { nursingType = NursingType.BOTTLE },
                            label = { Text(stringResource(R.string.nursing_bottle)) }
                        )
                    }

                    if (nursingType == NursingType.BOTTLE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountMlText,
                            onValueChange = { amountMlText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(R.string.bottle_amount_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                } else if (event.type == EventType.DIAPER) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.dialog_diaper_content),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = diaperType == DiaperType.PEE,
                            onClick = { diaperType = DiaperType.PEE },
                            label = { Text(stringResource(R.string.btn_diaper_pee)) }
                        )
                        FilterChip(
                            selected = diaperType == DiaperType.POO,
                            onClick = { diaperType = DiaperType.POO },
                            label = { Text(stringResource(R.string.btn_diaper_poo)) }
                        )
                        FilterChip(
                            selected = diaperType == DiaperType.BOTH,
                            onClick = { diaperType = DiaperType.BOTH },
                            label = { Text(stringResource(R.string.btn_diaper_both)) }
                        )
                    }
                }

                // Notes
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.entry_notes)) },
                    placeholder = { Text(stringResource(R.string.entry_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalEnd = if (event.type == EventType.DIAPER) {
                        startTime
                    } else if (isOngoing) {
                        null
                    } else {
                        endTime ?: startTime
                    }
                    val finalAmount = if (event.type == EventType.NURSING && nursingType == NursingType.BOTTLE) {
                        amountMlText.toIntOrNull()
                    } else null

                    val updated = event.copy(
                        startTime = startTime,
                        endTime = finalEnd,
                        nursingType = if (event.type == EventType.NURSING) nursingType else null,
                        diaperType = if (event.type == EventType.DIAPER) diaperType else null,
                        amountMl = finalAmount,
                        note = note.ifBlank { null }
                    )
                    onSave(updated)
                }
            ) {
                Text(stringResource(R.string.btn_save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun IconButtonSafeDelete(onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.btn_delete_entry),
            tint = MaterialTheme.colorScheme.error
        )
    }
}
