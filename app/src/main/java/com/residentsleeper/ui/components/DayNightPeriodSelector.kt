package com.residentsleeper.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.residentsleeper.R
import com.residentsleeper.ui.chart.DayPeriod

@Composable
fun DayNightPeriodSelector(
    selectedPeriod: DayPeriod,
    dayStartHour: Int,
    onPeriodSelected: (DayPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val nightStartHour = (dayStartHour + 12) % 24

    val dayTimeRange = java.lang.String.format(java.util.Locale.getDefault(), "%02d:00–%02d:00", dayStartHour, nightStartHour)
    val nightTimeRange = java.lang.String.format(java.util.Locale.getDefault(), "%02d:00–%02d:00", nightStartHour, dayStartHour)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Day Button
        val isDaySelected = selectedPeriod == DayPeriod.DAY
        val dayContainerTarget = when {
            !isDaySelected -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color(0xFFF1F5F9)
            isDark -> Color(0xFF78350F)
            else -> Color(0xFFFEF3C7)
        }
        val dayContentTarget = when {
            !isDaySelected -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            isDark -> Color(0xFFFDE68A)
            else -> Color(0xFFB45309)
        }
        val dayBorderTarget = if (isDaySelected) {
            if (isDark) Color(0xFFF59E0B) else Color(0xFFF59E0B)
        } else {
            Color.Transparent
        }

        val dayContainer by animateColorAsState(dayContainerTarget, label = "dayContainer")
        val dayContent by animateColorAsState(dayContentTarget, label = "dayContent")
        val dayBorder by animateColorAsState(dayBorderTarget, label = "dayBorder")

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPeriodSelected(DayPeriod.DAY) },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = dayContainer),
            border = if (isDaySelected) BorderStroke(1.5.dp, dayBorder) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = dayContent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.chart_period_day),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium,
                        color = dayContent
                    )
                    Text(
                        text = dayTimeRange,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = dayContent.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Night Button
        val isNightSelected = selectedPeriod == DayPeriod.NIGHT
        val nightContainerTarget = when {
            !isNightSelected -> if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color(0xFFF1F5F9)
            isDark -> Color(0xFF1E1B4B)
            else -> Color(0xFFEEF2FF)
        }
        val nightContentTarget = when {
            !isNightSelected -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            isDark -> Color(0xFFC7D2FE)
            else -> Color(0xFF4338CA)
        }
        val nightBorderTarget = if (isNightSelected) {
            if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1)
        } else {
            Color.Transparent
        }

        val nightContainer by animateColorAsState(nightContainerTarget, label = "nightContainer")
        val nightContent by animateColorAsState(nightContentTarget, label = "nightContent")
        val nightBorder by animateColorAsState(nightBorderTarget, label = "nightBorder")

        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onPeriodSelected(DayPeriod.NIGHT) },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = nightContainer),
            border = if (isNightSelected) BorderStroke(1.5.dp, nightBorder) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = nightContent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.chart_period_night),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isNightSelected) FontWeight.Bold else FontWeight.Medium,
                        color = nightContent
                    )
                    Text(
                        text = nightTimeRange,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = nightContent.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
