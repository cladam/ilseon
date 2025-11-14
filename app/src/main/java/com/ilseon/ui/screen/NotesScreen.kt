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
import com.ilseon.NotesViewModel
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.ui.theme.IlseonTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun NotesScreen(
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    NotesScreenContent(notes = notes)
}

@Composable
private fun NotesScreenContent(notes: List<Task>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Your Notes",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You have ${notes.size} notes. Keep reflecting!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (notes.isEmpty()) {
            item {
                Text(
                    text = "No notes yet. Complete a task to add a note.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(notes, key = { it.id }) { task ->
                NoteItem(task = task)
            }
        }
    }
}

@Composable
private fun NoteItem(task: Task) {
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
            task.completionReflection?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
fun NotesScreenPreview() {
    IlseonTheme {
        val previewTasks = listOf(
            Task(
                id = UUID.randomUUID(),
                title = "Finish the design mockups",
                contextId = UUID.randomUUID(),
                priority = TaskPriority.High,
                isComplete = true,
                completedAt = System.currentTimeMillis() - 86400000, // 1 day ago
                completionReflection = "This went really well. I was in a good flow state."
            ),
            Task(
                id = UUID.randomUUID(),
                title = "Call the dentist",
                contextId = UUID.randomUUID(),
                priority = TaskPriority.Mid,
                isComplete = true,
                completedAt = System.currentTimeMillis() - 172800000, // 2 days ago
                completionReflection = "I procrastinated a bit on this, but I'm glad I did it."
            )
        )
        NotesScreenContent(notes = previewTasks)
    }
}