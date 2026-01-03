
package com.ilseon.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.data.EnergyLevel
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.toColor
import com.ilseon.ui.components.MarkdownText
import com.ilseon.ui.theme.QuietAmber
import com.ilseon.ui.theme.toColor
import java.util.UUID

@Composable
fun NextTaskActivationScreen(
    nextTask: Task?,
    contextMap: Map<UUID, TaskContext>,
    onStartNextBlock: () -> Unit,
    onGoToFilter: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Task Complete!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (nextTask != null) {
                Text(
                    text = "Here's your next priority:",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority & Context
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(nextTask.priority.toColor(), CircleShape)
                            )
                            Text(
                                text = contextMap[nextTask.contextId]?.name ?: "General",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        // Energy Badge
                        nextTask.energyLevel?.let { level ->
                            Row(
                                modifier = Modifier
                                    .background(level.toColor().copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = level.toColor().copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val icon = when (level) {
                                    EnergyLevel.High -> Icons.Default.BatteryFull
                                    EnergyLevel.Medium -> Icons.Default.Battery3Bar
                                    EnergyLevel.Low -> Icons.Default.Battery1Bar
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Energy Level",
                                    tint = level.toColor(),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(90f)
                                )
                                Text(
                                    text = level.name,
                                    color = level.toColor(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (nextTask.isUrgent) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Urgent",
                                        tint = QuietAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                dev.jeziellago.compose.markdowntext.MarkdownText(
                                    markdown = nextTask.title,
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            nextTask.description?.let {
                                if (it.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    MarkdownText(markdown = it)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))

                if (nextTask.schedulingType != SchedulingType.None) {
                    Button(
                        onClick = onStartNextBlock,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Start Next Task")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                TextButton(onClick = onGoToFilter) {
                    Text("Back to Dashboard")
                }
            } else {
                // This part is handled by the ViewModel logic now, but kept as a fallback.
                Text(
                    text = "No more tasks for today!",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = onGoToFilter) {
                    Text("Back to Dashboard")
                }
            }
        }
    }
}
