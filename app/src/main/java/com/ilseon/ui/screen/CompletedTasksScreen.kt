package com.ilseon.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.CompletedTasksViewModel
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.ui.theme.IlseonTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun CompletedTasksScreen(
    viewModel: CompletedTasksViewModel = hiltViewModel()
) {
    val completedTasks by viewModel.completedTasks.collectAsState()
    CompletedTasksScreenContent(tasks = completedTasks)
}

@Composable
private fun CompletedTasksScreenContent(tasks: List<Task>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Your Accomplishments",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You've completed ${tasks.size} tasks. Great job!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (tasks.isEmpty()) {
            item {
                Text(
                    text = "No completed tasks yet. Keep going!",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                CompletedTaskItem(task = task)
            }
        }
    }
}

@Composable
private fun CompletedTaskItem(task: Task) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val completedDate = task.completedAt?.let { dateFormat.format(Date(it)) } ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Completed on $completedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompletedTasksScreenPreview() {
    IlseonTheme {
        val previewTasks = listOf(
            Task(
                id = UUID.randomUUID(),
                title = "Finish the design mockups",
                contextId = UUID.randomUUID(),
                priority = TaskPriority.High,
                isComplete = true,
                completedAt = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            Task(
                id = UUID.randomUUID(),
                title = "Call the dentist",
                contextId = UUID.randomUUID(),
                priority = TaskPriority.Mid,
                isComplete = true,
                completedAt = System.currentTimeMillis() - 172800000 // 2 days ago
            )
        )
        CompletedTasksScreenContent(tasks = previewTasks)
    }
}
