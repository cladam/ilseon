package com.ilseon.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.ui.components.AnimatedTaskItem
import com.ilseon.ui.components.TaskDetailsDialog
import com.ilseon.ui.theme.toColor
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NextUpTasks(
    tasks: List<Task>,
    completedTaskIds: Set<UUID>,
    onComplete: (Task) -> Unit,
    onAnimationFinished: (Task) -> Unit,
    contextMap: Map<UUID, TaskContext>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "")
    var taskToShowDetails by remember { mutableStateOf<Task?>(null) }

    taskToShowDetails?.let { task ->
        TaskDetailsDialog(
            task = task,
            context = contextMap[task.contextId],
            onDismiss = { taskToShowDetails = null }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Next Up (${tasks.size})",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Expand or collapse",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.rotate(rotationAngle)
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                tasks.forEach { task ->
                    AnimatedTaskItem(
                        task = task,
                        isVisible = !completedTaskIds.contains(task.id),
                        onComplete = onAnimationFinished
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        taskToShowDetails = it
                                    }
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(it.priority.toColor())
                            ) {}
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = it.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), CircleShape)
                                    .clickable { onComplete(it) },
                                contentAlignment = Alignment.Center
                            ) {
                                // Empty, for the checkmark to appear after click
                            }
                        }
                    }
                }
            }
        }
    }
}
