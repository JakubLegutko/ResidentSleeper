package com.residentsleeper.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Boy
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Girl
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.residentsleeper.R
import com.residentsleeper.data.model.Gender

fun Gender.getIcon(): ImageVector = when (this) {
    Gender.BOY -> Icons.Default.Boy
    Gender.GIRL -> Icons.Default.Girl
    Gender.UNSPECIFIED -> Icons.Default.ChildCare
}

fun Gender.getDisplayColor(isDark: Boolean): Color = when (this) {
    Gender.BOY -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    Gender.GIRL -> if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
    Gender.UNSPECIFIED -> if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
}

@Composable
fun GenderSelectionRow(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val options = listOf(
        Triple(Gender.UNSPECIFIED, stringResource(R.string.gender_unspecified), Icons.Default.ChildCare),
        Triple(Gender.BOY, stringResource(R.string.gender_boy), Icons.Default.Boy),
        Triple(Gender.GIRL, stringResource(R.string.gender_girl), Icons.Default.Girl)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (gender, label, icon) ->
            val isSelected = selectedGender == gender

            val targetContainerColor = when {
                !isSelected -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                gender == Gender.BOY -> if (isDark) Color(0xFF075985) else Color(0xFFE0F2FE)
                gender == Gender.GIRL -> if (isDark) Color(0xFF831843) else Color(0xFFFCE7F3)
                else -> MaterialTheme.colorScheme.primaryContainer
            }

            val targetContentColor = when {
                !isSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                gender == Gender.BOY -> if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1)
                gender == Gender.GIRL -> if (isDark) Color(0xFFFCE7F3) else Color(0xFFBE123C)
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }

            val targetBorderColor = when {
                isSelected && gender == Gender.BOY -> if (isDark) Color(0xFF38BDF8) else Color(0xFF38BDF8)
                isSelected && gender == Gender.GIRL -> if (isDark) Color(0xFFFB7185) else Color(0xFFFB7185)
                isSelected -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }

            val containerColor by animateColorAsState(targetContainerColor, label = "genderContainer")
            val contentColor by animateColorAsState(targetContentColor, label = "genderContent")
            val borderColor by animateColorAsState(targetBorderColor, label = "genderBorder")

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onGenderSelected(gender) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = if (isSelected) BorderStroke(1.5.dp, borderColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "${gender.emote} $label",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}
