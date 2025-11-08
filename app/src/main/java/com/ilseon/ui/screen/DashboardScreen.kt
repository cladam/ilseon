package com.ilseon.ui.screen

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.TaskContextViewModel
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskPriority
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@Composable
fun DashboardScreen(
    tasks: List<Task>,
    onTaskComplete: (Task) -> Unit,
    contextViewModel: TaskContextViewModel = hiltViewModel()
) {
    var completedTaskIds by remember { mutableStateOf<Set<UUID>>(emptySet()) }
    val contexts by contextViewModel.contexts.collectAsState()
    val contextMap = remember(contexts) { contexts.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClockDisplay()

        Spacer(Modifier.height(32.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("All clear!", color = MaterialTheme.colorScheme.secondary, fontSize = 20.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val task = tasks.first()
                    AnimatedTaskItem(
                        task = task,
                        isVisible = !completedTaskIds.contains(task.id),
                        onComplete = {
                            completedTaskIds = completedTaskIds + task.id
                        }
                    ) {
                        CurrentPriorityTask(
                            task = it,
                            contextName = contextMap[it.contextId]?.name ?: "N/A",
                            onComplete = onTaskComplete
                        )
                    }
                }

                if (tasks.size > 1) {
                    item {
                        NextUpTasks(
                            tasks = tasks.drop(1),
                            completedTaskIds = completedTaskIds,
                            onComplete = onTaskComplete
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedTaskItem(
    task: Task,
    isVisible: Boolean,
    onComplete: (Task) -> Unit,
    content: @Composable (Task) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) +
                fadeOut(animationSpec = tween(durationMillis = 300)),
    ) {
        content(task)
    }

    if (!isVisible) {
        LaunchedEffect(task) {
            delay(300) // Wait for animation to finish
            onComplete(task)
        }
    }
}

@Composable
fun ClockDisplay() {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val timeZone = TimeZone.getDefault()
        val is24HourFormat = DateFormat.is24HourFormat(context)
        val timePattern = if (is24HourFormat) "HH:mm" else "h:mm a"

        val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }

        while (true) {
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date())
            delay(1000)
        }

    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currentTime,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 72.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = currentDate,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CurrentPriorityTask(task: Task, contextName: String, onComplete: (Task) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Current Priority Task",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$contextName / ${task.priority.name} Priority",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                        .clickable { onComplete(task) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, "Complete Task", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun NextUpTasks(tasks: List<Task>, completedTaskIds: Set<UUID>, onComplete: (Task) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Next Up (${tasks.size})",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Filled.ChevronRight, "View All", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(12.dp))

        tasks.take(3).forEach { task ->
            AnimatedTaskItem(
                task = task,
                isVisible = !completedTaskIds.contains(task.id),
                onComplete = onComplete
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), CircleShape)
                            .clickable { onComplete(task) },
                        contentAlignment = Alignment.Center
                    ) {}
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = it.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun QuickCaptureSheet(
    onSave: (String, UUID?, TaskPriority) -> Unit,
    viewModel: TaskContextViewModel = hiltViewModel()
) {
    val contexts by viewModel.contexts.collectAsState()
    var title by remember { mutableStateOf("") }
    var selectedContextId by remember { mutableStateOf<UUID?>(null) }
    var priority by remember { mutableStateOf(TaskPriority.Mid) }

    LaunchedEffect(contexts) {
        if (selectedContextId == null) {
            selectedContextId = contexts.firstOrNull()?.id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = "Quick Capture",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            maxLines = 1
        )

        Spacer(Modifier.height(24.dp))

        Text("Context", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            contexts.chunked(3).forEach { rowContexts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowContexts.forEach { ctx ->
                        Button(
                            onClick = { selectedContextId = ctx.id },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedContextId == ctx.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedContextId == ctx.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(ctx.name)
                        }
                    }
                    // Add spacers to fill the row if there are less than 3 items
                    repeat(3 - rowContexts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Priority", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskPriority.entries.forEach { prio ->
                Button(
                    onClick = { priority = prio },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (priority == prio) prio.toColor() else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (priority == prio) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(prio.name)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { onSave(title, selectedContextId, priority) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = title.isNotBlank() && selectedContextId != null
        ) {
            Text("Save Task", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun TaskPriority.toColor(): Color {
    return when(this) {
        TaskPriority.High -> Color(0xFFEF4444)
        TaskPriority.Mid -> Color(0xFFF59E0B)
        TaskPriority.Low -> Color(0xFF3B82F6)
    }
}
