package com.ilseon.ui.screen

import android.util.Log
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.TaskContextViewModel
import com.ilseon.TaskViewModel
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.Task
import com.ilseon.ui.components.AnimatedTaskItem
import com.ilseon.ui.theme.MutedRed
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    tasks: List<Task>,
    completedTaskIds: Set<UUID>,
    onAnimateComplete: (Task) -> Unit,
    onTaskComplete: (Task) -> Unit,
    onTaskTimerFinished: (Task) -> Unit,
    onStartTask: (Task) -> Unit,
    onPauseTask: (Task) -> Unit,
    activeFocusBlock: FocusBlock?,
    onSwipeUp: () -> Unit,
    taskViewModel: TaskViewModel = hiltViewModel(),
    contextViewModel: TaskContextViewModel = hiltViewModel()
) {
    Log.d("DashboardDebug", "=== Received Tasks ===")
    tasks.forEach { task ->
        Log.d("DashboardDebug", "Received: ${task.title}, startTime=${task.startTime}, isRecurring=${task.isRecurring}")
    }
    val contextsWithFocusBlock by contextViewModel.contextsWithFocusBlock.collectAsState()
    val contextMap = remember(contextsWithFocusBlock) {
        contextsWithFocusBlock.associate { it.context.id to it.context }
    }

    val (focusTasks, urgentOutOfContextTasks) = remember(tasks, activeFocusBlock) {
        if (activeFocusBlock != null) {
            val inContext = tasks.filter { task ->
                task.contextId == activeFocusBlock.contextId
            }
            val urgentOutOfContext = tasks.filter { task ->
                task.contextId != activeFocusBlock.contextId &&
                        task.isUrgent &&
                        !task.isComplete
            }
            inContext to urgentOutOfContext
        } else {
            // Don't filter by startTime here - let all today's tasks through
            // The split into active/future happens in the next remember block
            tasks to emptyList()
        }
    }

    val (priorityTask, nextUpTasks) = remember(focusTasks) {
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursFromNow = currentTime + TimeUnit.HOURS.toMillis(24)

        Log.d("DashboardDebug", "=== Sorting Debug ===")
        Log.d("DashboardDebug", "Current time: $currentTime")

        focusTasks.forEachIndexed { index, task ->
            Log.d("DashboardDebug", "Task[$index]: ${task.title}")
            Log.d("DashboardDebug", "  - isUrgent: ${task.isUrgent}")
            Log.d("DashboardDebug", "  - priority: ${task.priority} (ordinal: ${task.priority.ordinal})")
            Log.d("DashboardDebug", "  - startTime: ${task.startTime} (isFuture: ${task.startTime != null && task.startTime > currentTime})")
            Log.d("DashboardDebug", "  - createdAt: ${task.createdAt}")
            Log.d("DashboardDebug", "  - isRecurring: ${task.isRecurring}")
            Log.d("DashboardDebug", "  - schedulingType: ${task.schedulingType}")
        }

        // Only tasks that have started (or have no start time) can be the priority task
        val activeTasks = focusTasks.filter { task ->
            task.startTime == null || task.startTime <= currentTime
        }
        // Tasks scheduled for later go to "next up"
        val futureTasks = focusTasks.filter { task ->
            task.startTime != null && task.startTime > currentTime && task.startTime <= twentyFourHoursFromNow
        }

        Log.d("DashboardDebug", "Active tasks count: ${activeTasks.size}")
        Log.d("DashboardDebug", "Active tasks: ${activeTasks.map { "${it.title} (start=${it.startTime})" }}")
        Log.d("DashboardDebug", "Future tasks count: ${futureTasks.size}")
        Log.d("DashboardDebug", "Future tasks: ${futureTasks.map { "${it.title} (start=${it.startTime})" }}")
        Log.d("DashboardDebug", "NextUp tasks: ${(activeTasks.drop(1) + futureTasks).map { it.title }}")

        Log.d("DashboardDebug", "Current time: $currentTime")
        Log.d("DashboardDebug", "Active: ${activeTasks.map { "${it.title}(${it.startTime})" }}")
        Log.d("DashboardDebug", "Future: ${futureTasks.map { "${it.title}(${it.startTime})" }}")

        val priorityTask = activeTasks.firstOrNull()
        val nextUp = activeTasks.drop(1) + futureTasks

        Log.d("DashboardDebug", "Priority: ${priorityTask?.title}")
        Log.d("DashboardDebug", "NextUp: ${nextUp.map { it.title }}")


        priorityTask to nextUp
    }


    val focusContextName = activeFocusBlock?.let { block -> contextMap[block.contextId]?.name }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    val yDrag = dragAmount
                    if (yDrag < -40) { // Threshold for swipe up
                        onSwipeUp()
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClockDisplay()

        Spacer(Modifier.height(32.dp))

        if (priorityTask == null && urgentOutOfContextTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (focusContextName != null) "No tasks for $focusContextName" else "All clear!",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (priorityTask != null) {
                    item {
                        AnimatedTaskItem(
                            task = priorityTask,
                            isVisible = !completedTaskIds.contains(priorityTask.id),
                            onComplete = { onAnimateComplete(priorityTask) }
                        ) { task ->
                            CurrentPriorityTask(
                                task = task,
                                contextName = contextMap[task.contextId]?.name ?: "General",
                                contexts = contextMap.values.toList(),
                                onComplete = { onTaskComplete(task) },
                                onTimerFinished = onTaskTimerFinished,
                                onStartTask = onStartTask,
                                onPauseTask = onPauseTask,
                                onUpdate = { updatedTask, reason ->
                                    taskViewModel.updateTask(
                                        updatedTask
                                    )
                                },
                                focusContextName = focusContextName,
                                viewModel = taskViewModel
                            )
                        }
                    }
                }

                if (urgentOutOfContextTasks.isNotEmpty()) {
                    item {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Urgent Outside Focus",
                                color = MutedRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item {
                        NextUpTasks(
                            tasks = urgentOutOfContextTasks,
                            completedTaskIds = completedTaskIds,
                            onComplete = { onTaskComplete(it) },
                            onAnimationFinished = onAnimateComplete,
                            contextMap = contextMap,
                            viewModel = taskViewModel,
                            header = "Urgent Tasks Awaiting",
                            isUrgentList = true
                        )
                    }
                }


                if (nextUpTasks.isNotEmpty()) {
                    if (urgentOutOfContextTasks.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }
                    item {
                        NextUpTasks(
                            tasks = nextUpTasks,
                            completedTaskIds = completedTaskIds,
                            onComplete = { onTaskComplete(it) },
                            onAnimationFinished = onAnimateComplete,
                            contextMap = contextMap,
                            viewModel = taskViewModel,
                            header = ""
                        )
                    }
                }
            }
        }
    }
}