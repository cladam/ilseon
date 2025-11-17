package com.ilseon.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.TaskContextViewModel
import com.ilseon.TaskViewModel
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.Task
import com.ilseon.ui.components.AnimatedTaskItem
import java.util.UUID

@Composable
fun DashboardScreen(
    tasks: List<Task>,
    completedTaskIds: Set<UUID>,
    onAnimateComplete: (Task) -> Unit,
    onTaskComplete: (Task, String) -> Unit,
    onTaskTimerFinished: (Task) -> Unit,
    onStartTask: (Task) -> Unit,
    onPauseTask: (Task) -> Unit,
    activeFocusBlock: FocusBlock?,
    taskViewModel: TaskViewModel = hiltViewModel(),
    contextViewModel: TaskContextViewModel = hiltViewModel()
) {
    val contextsWithFocusBlock by contextViewModel.contextsWithFocusBlock.collectAsState()
    val contextMap = remember(contextsWithFocusBlock) {
        contextsWithFocusBlock.associate { it.context.id to it.context }
    }

    val (priorityTask, nextUpTasks) = remember(tasks) {
        val now = System.currentTimeMillis()

        val sortedTasks = tasks.sortedWith(
            // Primary sort key: task "status"
            compareBy<Task> {
                when {
                    it.startTime != null && it.endTime != null && now in it.startTime..it.endTime -> 1 // Active
                    it.startTime != null && now < it.startTime -> 2 // Upcoming
                    it.dueTime != null -> 3 // Due soon
                    else -> 4 + it.priority.ordinal // Everything else, by priority
                }
            }
                // Secondary sort key: time for timed tasks, creation for others
                .thenBy {
                    when {
                        it.startTime != null && it.endTime != null && now in it.startTime..it.endTime -> it.endTime
                        it.startTime != null && now < it.startTime -> it.startTime
                        it.dueTime != null -> it.dueTime
                        else -> it.createdAt
                    }
                }
        )

        val priorityTask = sortedTasks.firstOrNull()
        val nextUp = sortedTasks.drop(1)
        priorityTask to nextUp
    }

    val focusContextName = activeFocusBlock?.let { block -> contextMap[block.contextId]?.name }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClockDisplay()

        Spacer(Modifier.height(32.dp))

        if (priorityTask == null) {
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
                item {
                    AnimatedTaskItem(
                        task = priorityTask,
                        isVisible = !completedTaskIds.contains(priorityTask.id),
                        onComplete = { onAnimateComplete(priorityTask) }
                    ) { task ->
                        CurrentPriorityTask(
                            task = task,
                            contextName = contextMap[task.contextId]?.name ?: "General",
                            onComplete = onTaskComplete,
                            onTimerFinished = onTaskTimerFinished,
                            onStartTask = onStartTask,
                            onPauseTask = onPauseTask,
                            focusContextName = focusContextName
                        )
                    }
                }

                if (nextUpTasks.isNotEmpty()) {
                    item {
                        NextUpTasks(
                            tasks = nextUpTasks,
                            completedTaskIds = completedTaskIds,
                            onComplete = onTaskComplete,
                            onAnimationFinished = onAnimateComplete,
                            contextMap = contextMap,
                            viewModel = taskViewModel
                        )
                    }
                }
            }
        }
    }
}