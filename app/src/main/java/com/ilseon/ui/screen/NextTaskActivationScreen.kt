
package com.ilseon.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.ui.components.HtmlText
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
                    color = MaterialTheme.colorScheme.secondary,
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
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nextTask.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            nextTask.description?.let {
                                if (it.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    HtmlText(html = it)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
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
