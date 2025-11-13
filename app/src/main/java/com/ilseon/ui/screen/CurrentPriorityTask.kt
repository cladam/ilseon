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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ilseon.ui.theme.QuietAmber
import com.ilseon.ui.theme.toColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun CurrentPriorityTask(
    task: Task,
    contextName: String,
    onComplete: (Task) -> Unit,
    onTimerFinished: (Task) -> Unit,
    onStartTask: (Task) -> Unit,
    onPauseTask: (Task) -> Unit,
    focusContextName: String?
) {
    var remainingTime by remember(task.id) { mutableStateOf(task.remainingTimeInSeconds * 1000L) }
    val timerState = task.timerState
    val isInFocusBlock = focusContextName != null
    var isOverdue by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = task) {
        isOverdue = task.endTime != null && System.currentTimeMillis() > task.endTime && !task.isComplete

        if (timerState == TimerState.Running) {
            val startTime = task.timerStartTime ?: System.currentTimeMillis()
            // This is the duration to countdown from *now*
            val countdownDuration = task.remainingTimeInSeconds * 1000L
            val endTime = startTime + countdownDuration

            val initialRemaining = endTime - System.currentTimeMillis()
            remainingTime = max(0, initialRemaining)

            while (remainingTime > 0) {
                delay(1000L)
                remainingTime -= 1000L
                isOverdue = task.endTime != null && System.currentTimeMillis() > task.endTime && !task.isComplete
            }

            if (task.timerState == TimerState.Running) {
                onTimerFinished(task)
                isOverdue = true
            }
        } else {
            // If not running, display the remaining time from the task
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
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = when {
                        isOverdue -> QuietAmber
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    shape = RoundedCornerShape(16.dp)
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

                if (task.timerState == TimerState.NotStarted || task.timerState == TimerState.Paused) {
                    IconButton(onClick = { onStartTask(task) }) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Start Task",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (task.timerState == TimerState.Running) {
                    IconButton(onClick = { onPauseTask(task) }) {
                        Icon(
                            Icons.Filled.Pause,
                            contentDescription = "Pause Task",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }


                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable { onComplete(task) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Complete Task",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
