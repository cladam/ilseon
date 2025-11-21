package com.ilseon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayPicker(
    selectedDays: List<Int>,
    onDaySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val days = DayOfWeek.values()
        days.forEach { day ->
            val isSelected = selectedDays.contains(day.value)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Gray.copy(
                            alpha = 0.2f
                        ),
                        shape = CircleShape
                    )
                    .clickable { onDaySelected(day.value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}