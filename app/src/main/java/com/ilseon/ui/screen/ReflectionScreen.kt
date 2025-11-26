package com.ilseon.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.ReflectionsViewModel
import com.ilseon.data.task.Task
import com.ilseon.ui.components.HtmlText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReflectionScreen(
    viewModel: ReflectionsViewModel = hiltViewModel()
) {
    val reflections by viewModel.reflections.collectAsState()
    var editingTask by remember { mutableStateOf<Task?>(null) }

    ReflectionScreenContent(
        reflections = reflections,
        onDeleteReflection = { viewModel.deleteReflection(it) },
        onEditReflection = { editingTask = it }
    )

    if (editingTask != null) {
        EditReflectionDialog(
            task = editingTask!!,
            onDismiss = { editingTask = null },
            onSave = { updatedTask ->
                viewModel.updateReflection(updatedTask)
                editingTask = null
            }
        )
    }
}

@Composable
private fun EditReflectionDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var reflectionText by remember { mutableStateOf(task.completionReflection ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Reflection") },
        text = {
            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                label = { Text("Your reflection") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedTask = task.copy(completionReflection = reflectionText)
                    onSave(updatedTask)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ReflectionScreenContent(
    reflections: List<Task>,
    onDeleteReflection: (Task) -> Unit,
    onEditReflection: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Your Reflections",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You have ${reflections.size} reflections. Keep reflecting!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (reflections.isEmpty()) {
            item {
                Text(
                    text = "No reflections yet. Complete a task to add a reflection.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(reflections, key = { it.id }) { task ->
                ReflectionItem(
                    task = task,
                    onDelete = { onDeleteReflection(task) },
                    onEdit = { onEditReflection(task) }
                )
            }
        }
    }
}

@Composable
private fun ReflectionItem(
    task: Task,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val completedDate = task.completedAt?.let { dateFormat.format(Date(it)) } ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                task.completionReflection?.let {
                    HtmlText(
                        html = it,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Completed on $completedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit reflection",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete reflection",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}