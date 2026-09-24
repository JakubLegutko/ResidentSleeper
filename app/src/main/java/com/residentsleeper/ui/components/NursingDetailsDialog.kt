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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.residentsleeper.R
import com.residentsleeper.data.model.NursingType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NursingDetailsDialog(
    initialNursingType: NursingType = NursingType.LEFT_BREAST,
    onDismiss: () -> Unit,
    onConfirm: (nursingType: NursingType, amountMl: Int?) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialNursingType) }
    var bottleMlText by remember { mutableStateOf("90") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.nursing_dialog_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dialog_select_method),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == NursingType.LEFT_BREAST,
                        onClick = { selectedType = NursingType.LEFT_BREAST },
                        label = { Text(stringResource(R.string.nursing_left_breast)) }
                    )
                    FilterChip(
                        selected = selectedType == NursingType.RIGHT_BREAST,
                        onClick = { selectedType = NursingType.RIGHT_BREAST },
                        label = { Text(stringResource(R.string.nursing_right_breast)) }
                    )
                    FilterChip(
                        selected = selectedType == NursingType.BOTH_BREASTS,
                        onClick = { selectedType = NursingType.BOTH_BREASTS },
                        label = { Text(stringResource(R.string.nursing_both_breasts)) }
                    )
                    FilterChip(
                        selected = selectedType == NursingType.BOTTLE,
                        onClick = { selectedType = NursingType.BOTTLE },
                        label = { Text(stringResource(R.string.nursing_bottle)) }
                    )
                }

                if (selectedType == NursingType.BOTTLE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = bottleMlText,
                        onValueChange = { bottleMlText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.bottle_amount_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = if (selectedType == NursingType.BOTTLE) bottleMlText.toIntOrNull() else null
                    onConfirm(selectedType, amount)
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
