package com.ilseon.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.ReflectionsViewModel
import com.ilseon.data.EnergyLevel
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.toColor
import com.ilseon.ui.components.AppCard
import com.ilseon.ui.components.MarkdownText
import com.ilseon.ui.theme.QuietAmber
import com.ilseon.ui.theme.toColor
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore


private fun getWeekKey(timestamp: Long): Pair<Int, Int> {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return Pair(calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.WEEK_OF_YEAR))
}

private fun formatWeekLabel(year: Int, week: Int): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.YEAR, year)
    calendar.set(java.util.Calendar.WEEK_OF_YEAR, week)
    calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    val startDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(calendar.time)
    calendar.add(java.util.Calendar.DAY_OF_WEEK, 6)
    val endDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)
    return "$startDate - $endDate"
}

@Composable
fun ReflectionScreen(
    viewModel: ReflectionsViewModel = hiltViewModel()
) {
    val reflections by viewModel.reflections.collectAsState()
    val contextMap by viewModel.contextMap.collectAsState()
    var editingTask by remember { mutableStateOf<Task?>(null) }

    ReflectionScreenContent(
        reflections = reflections,
        contextMap = contextMap,
        onDeleteReflection = { viewModel.deleteReflection(it) },
        onEditReflection = { editingTask = it }
    )

    if (editingTask != null) {
        EditReflectionDialog(
            task = editingTask!!,
            onDismiss = { editingTask = null },
            onSave = { updatedTask ->
                viewModel.updateReflection(updatedTask)
                editingTask = null
            }
        )
    }
}

@Composable
private fun EditReflectionDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var reflectionText by remember { mutableStateOf(task.completionReflection ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Reflection") },
        text = {
            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                label = { Text("Your reflection") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedTask = task.copy(completionReflection = reflectionText)
                    onSave(updatedTask)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReflectionScreenContent(
    reflections: List<Task>,
    contextMap: Map<UUID, TaskContext>,
    onDeleteReflection: (Task) -> Unit,
    onEditReflection: (Task) -> Unit
) {
    val groupedReflections = remember(reflections) {
        reflections
            .sortedByDescending { it.completedAt ?: 0L }
            .groupBy { task ->
                val timestamp = task.completedAt ?: task.createdAt
                getWeekKey(timestamp)
            }
            .toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
    }

    var expandedWeeks by remember { mutableStateOf(setOf(groupedReflections.keys.firstOrNull())) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Your Reflections",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${reflections.size} reflections across ${groupedReflections.size} weeks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (reflections.isEmpty()) {
            item {
                Text(
                    text = "No reflections yet. Complete a task to add a reflection.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            groupedReflections.forEach { (weekKey, weekReflections) ->
                val isExpanded = expandedWeeks.contains(weekKey)
                val weekLabel = formatWeekLabel(weekKey.first, weekKey.second)

                item(key = "header_${weekKey.first}_${weekKey.second}") {
                    WeekHeader(
                        weekLabel = weekLabel,
                        count = weekReflections.size,
                        isExpanded = isExpanded,
                        onClick = {
                            expandedWeeks = if (isExpanded) {
                                expandedWeeks - weekKey
                            } else {
                                expandedWeeks + weekKey
                            }
                        }
                    )
                }

                if (isExpanded) {
                    items(weekReflections, key = { it.id }) { task ->
                        ReflectionItem(
                            task = task,
                            contextName = contextMap[task.contextId]?.name ?: "General",
                            onDelete = { onDeleteReflection(task) },
                            onEdit = { onEditReflection(task) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ReflectionItem(
    task: Task,
    contextName: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val completedDate = task.completedAt?.let { dateFormat.format(Date(it)) } ?: "N/A"

    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(task.priority.toColor(), CircleShape)
                    )
                    Text(
                        text = contextName,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    if (task.isUrgent) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Urgent",
                            tint = QuietAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                task.actualEnergyLevel?.let { level ->
                    Row(
                        modifier = Modifier
                            .background(
                                level
                                    .toColor()
                                    .copy(alpha = 0.1f), RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = level
                                    .toColor()
                                    .copy(alpha = 0.5f),
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
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MarkdownText(
                        markdown = task.title,
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    task.completionReflection?.let {
                        MarkdownText(
                            markdown = it,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Completed at $completedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit reflection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete reflection",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(
    weekLabel: String,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = weekLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

