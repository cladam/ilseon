package com.ilseon.ui.screen

import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.TaskContextViewModel
import com.ilseon.TaskViewModel
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.Task
import com.ilseon.ui.components.AnimatedTaskItem
import java.util.UUID
import kotlin.math.abs

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
    val contextsWithFocusBlock by contextViewModel.contextsWithFocusBlock.collectAsState()
    val contextMap = remember(contextsWithFocusBlock) {
        contextsWithFocusBlock.associate { it.context.id to it.context }
    }

    val (priorityTask, nextUpTasks) = remember(tasks) {
        val priorityTask = tasks.firstOrNull()
        val nextUp = tasks.drop(1)
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
                            onComplete = { onTaskComplete(task) },
                            onTimerFinished = onTaskTimerFinished,
                            onStartTask = onStartTask,
                            onPauseTask = onPauseTask,
                            onUpdate = { updatedTask, reason -> taskViewModel.updateTask(updatedTask) },
                            focusContextName = focusContextName
                        )
                    }
                }

                if (nextUpTasks.isNotEmpty()) {
                    item {
                        NextUpTasks(
                            tasks = nextUpTasks,
                            completedTaskIds = completedTaskIds,
                            onComplete = { task, _ -> onTaskComplete(task) },
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
