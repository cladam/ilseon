package com.ilseon.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.data.task.Task
import com.ilseon.data.task.TimerState
import com.ilseon.ui.components.VisualCountdownTimer
import com.ilseon.ui.theme.toColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrentPriorityTask(
    task: Task,
    contextName: String,
    onComplete: (Task) -> Unit,
    focusContextName: String?
) {
    var remainingTime by remember(task.id) { mutableStateOf(task.remainingTimeInSeconds * 1000L) }
    var timerState by remember(task.id) { mutableStateOf(task.timerState) }

    LaunchedEffect(task.id, task.startTime, task.endTime, task.isComplete) {
        if (task.startTime == null || task.endTime == null || task.isComplete) {
            timerState = if (task.isComplete) TimerState.Finished else TimerState.NotStarted
            return@LaunchedEffect
        }

        // Wait until it's time to start
        while (System.currentTimeMillis() < task.startTime) {
            delay(1000)
        }

        // Start the timer
        timerState = TimerState.Running
        task.timerState = TimerState.Running

        val initialRemaining = task.endTime - System.currentTimeMillis()
        if (initialRemaining > 0) {
            remainingTime = initialRemaining
        } else {
            remainingTime = 0
        }

        while (remainingTime > 0) {
            delay(1000)
            remainingTime -= 1000
            task.remainingTimeInSeconds = remainingTime / 1000
        }

        timerState = TimerState.Finished
        task.timerState = TimerState.Finished
        // Optionally auto-complete the task when the timer finishes
        // onComplete(task)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val title = if (focusContextName != null) {
            "Current Priority Task ($focusContextName)"
        } else {
            "Current Priority Task"
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
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
                        size = 150.dp
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    task.description?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                    val subText = if (task.startTime != null && task.endTime != null && timerState != TimerState.Running) {
                        val startTimeStr = timeFormat.format(Date(task.startTime))
                        val endTimeStr = timeFormat.format(Date(task.endTime))
                        "Time Block: $startTimeStr - $endTimeStr"
                    } else {
                        contextName
                    }
                    Text(
                        text = subText,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(task.priority.toColor().copy(alpha = 0.1f))
                        .border(2.dp, task.priority.toColor(), CircleShape)
                        .clickable { onComplete(task) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Complete Task",
                        tint = task.priority.toColor(),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}