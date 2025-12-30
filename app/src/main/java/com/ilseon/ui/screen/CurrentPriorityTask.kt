package com.ilseon.ui.screen

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ilseon.TaskViewModel
import com.ilseon.data.EnergyLevel
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TimerState
import com.ilseon.data.toColor
import com.ilseon.ui.components.EditTaskDialog
import com.ilseon.ui.components.MarkdownText
import com.ilseon.ui.components.VisualCountdownTimer
import com.ilseon.ui.theme.QuietAmber
import com.ilseon.ui.theme.toColor
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurrentPriorityTask(
    task: Task,
    contextName: String,
    contexts: List<TaskContext>,
    onComplete: (Task) -> Unit,
    onTimerFinished: (Task) -> Unit,
    onStartTask: (Task) -> Unit,
    onPauseTask: (Task) -> Unit,
    onUpdate: (Task, String) -> Unit,
    focusContextName: String?,
    viewModel: TaskViewModel,
) {
    var remainingTime by remember(task.id) {
        mutableStateOf(task.remainingTimeInSeconds * 1000L)
    }
    val timerState = task.timerState
    val isInFocusBlock = focusContextName != null
    var isOverdue by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val subTasks by viewModel.subTasks.collectAsState()

    LaunchedEffect(task.id, refreshTrigger) {
        viewModel.loadSubTasks(task.id)
    }

    taskToEdit?.let {
        EditTaskDialog(
            task = it,
            contexts = contexts,
            onDismiss = { taskToEdit = null },
            onSave = { updatedTask ->
                onUpdate(updatedTask, "manual update")
                refreshTrigger++
                taskToEdit = null
            },
            viewModel = viewModel
        )
    }

    LaunchedEffect(key1 = task.id, key2 = timerState, key3 = task.dueTime) {
        val due = task.dueTime
        isOverdue = due != null && System.currentTimeMillis() > due && !task.isComplete

        if (timerState == TimerState.Running && due != null) {
            while (true) {
                val newRemaining = max(0, due - System.currentTimeMillis())
                remainingTime = newRemaining
                if (newRemaining == 0L) {
                    onTimerFinished(task)
                    isOverdue = true
                    break
                }
                delay(1000L)
            }
        } else {
            remainingTime = task.remainingTimeInSeconds * 1000L
        }
    }


    Column(modifier = Modifier.fillMaxWidth()) {
        val title = if (isInFocusBlock) {
            "Current Priority Task ($focusContextName)"
        } else {
            "Current Priority Task"
        }
        Text(
            text = title,
            color = colorScheme.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { taskToEdit = task }
                )
                .background(colorScheme.surface, RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = when {
                        isOverdue -> QuietAmber
                        else -> colorScheme.secondary
                    },
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp)
        ) {
            if (timerState == TimerState.Running && task.totalTimeInMinutes != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val totalTimeMillis = task.totalTimeInMinutes * 60 * 1000L
                    VisualCountdownTimer(
                        totalTimeInMillis = totalTimeMillis,
                        remainingTimeInMillis = remainingTime,
                        size = 175.dp
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            
            // Badges Row
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
                            .background(task.priority.toColor(), CircleShape)
                    )
                    Text(
                        text = contextName,
                        color = colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                // Energy Badge
                task.energyLevel?.let { level ->
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
                        val rotation = if (level == EnergyLevel.Medium) 0f else 270f
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

            // Main Content Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.isUrgent) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Urgent",
                                tint = QuietAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        MarkdownText(
                            markdown = task.title,
                            style = TextStyle(
                                color = colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    task.description?.let {
                        if (it.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            MarkdownText(markdown = it)
                        }
                    }

                    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    if (task.startTime != null && task.endTime != null && timerState != TimerState.Running) {
                        Spacer(Modifier.height(4.dp))
                        val startTimeStr = timeFormat.format(Date(task.startTime))
                        val endTimeStr = timeFormat.format(Date(task.endTime))
                        Text(
                            text = "Time Block: $startTimeStr - $endTimeStr",
                            color = colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))

                val showPlayButton = task.totalTimeInMinutes != null &&
                        (task.timerState == TimerState.Paused || (task.timerState == TimerState.NotStarted && task.schedulingType != SchedulingType.TimeBlock))

                if (showPlayButton) {
                    IconButton(onClick = { onStartTask(task) }) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Start Task",
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (task.totalTimeInMinutes != null && task.timerState == TimerState.Running) {
                    IconButton(onClick = { onPauseTask(task) }) {
                        Icon(
                            Icons.Filled.Pause,
                            contentDescription = "Pause Task",
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                val allSubTasksComplete = subTasks.all { it.isComplete }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (allSubTasksComplete) colorScheme.secondary else colorScheme.onSurface.copy(
                                alpha = 0.5f
                            )
                        )
                        .clickable(enabled = allSubTasksComplete) { 
                            viewModel.performHapticNudge()
                            onComplete(task) 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Complete Task",
                        tint = colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            if (subTasks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = colorScheme.outline.copy(alpha = 0.3f)
                )

                val completedCount = subTasks.count { it.isComplete }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sub-Tasks",
                        style = typography.labelMedium,
                        color = colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        "$completedCount/${subTasks.size}",
                        style = typography.labelMedium,
                        color = if (completedCount == subTasks.size)
                            colorScheme.secondary
                        else colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    subTasks.forEach { subTask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (subTask.isComplete)
                                        colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else colorScheme.surfaceVariant
                                )
                                .clickable {
                                    if (!subTask.isComplete) {
                                        Log.d("CurrentPriorityTask", "Subtask clicked, triggering haptic")
                                        viewModel.performHapticNudge()
                                    }
                                    viewModel.updateTask(subTask.copy(isComplete = !subTask.isComplete))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = subTask.isComplete,
                                onCheckedChange = null,
                                modifier = Modifier.size(20.dp),
                                colors = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor = colorScheme.secondary,
                                    uncheckedColor = colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = subTask.title,
                                style = typography.bodyMedium,
                                color = if (subTask.isComplete)
                                    colorScheme.onSurface.copy(alpha = 0.5f)
                                else colorScheme.onSurface,
                                textDecoration = if (subTask.isComplete)
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                else null
                            )
                        }
                    }
                }
            }
        }
    }
}
