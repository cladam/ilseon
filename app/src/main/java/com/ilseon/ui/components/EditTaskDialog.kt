package com.ilseon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ilseon.TaskViewModel
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    contexts: List<TaskContext>,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    viewModel: TaskViewModel,
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description ?: "") }
    var selectedPriority by remember { mutableStateOf(task.priority) }
    var selectedContextId by remember { mutableStateOf(task.contextId) }
    var isUrgent by remember { mutableStateOf(task.isUrgent) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var contextExpanded by remember { mutableStateOf(false) }
    var newSubTaskTitle by remember { mutableStateOf("") }

    val subTasks by viewModel.subTasks.collectAsState()

    LaunchedEffect(task.id) {
        viewModel.loadSubTasks(task.id)
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearSubTasks()
            onDismiss()
        },
        title = { Text("Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Dropdown
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPriority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        TaskPriority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.name) },
                                onClick = {
                                    selectedPriority = priority
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                // Urgent Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isUrgent,
                        onCheckedChange = { isUrgent = it }
                    )
                    Text(
                        text = "Mark as Urgent",
                        modifier = Modifier.clickable { isUrgent = !isUrgent }
                    )
                }

                // Context Dropdown
                ExposedDropdownMenuBox(
                    expanded = contextExpanded,
                    onExpandedChange = { contextExpanded = it }
                ) {
                    OutlinedTextField(
                        value = contexts.find { it.id == selectedContextId }?.name ?: "General",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Context") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contextExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = contextExpanded,
                        onDismissRequest = { contextExpanded = false }
                    ) {
                        contexts.forEach { context ->
                            DropdownMenuItem(
                                text = { Text(context.name) },
                                onClick = {
                                    selectedContextId = context.id
                                    contextExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sub-tasks section remains unchanged...
                Text("Sub-tasks", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    itemsIndexed(subTasks) { index, subTask ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = subTask.isComplete,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateTask(subTask.copy(isComplete = isChecked))
                                }
                            )
                            Text(subTask.title, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteSubTask(subTask) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete sub-task")
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubTaskTitle,
                        onValueChange = { newSubTaskTitle = it },
                        label = { Text("New sub-task") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (newSubTaskTitle.isNotBlank()) {
                            viewModel.addSubTask(task, newSubTaskTitle)
                            newSubTaskTitle = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add sub-task")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(task.copy(
                    title = title,
                    description = description.ifBlank { null },
                    priority = selectedPriority,
                    contextId = selectedContextId,
                    isUrgent = isUrgent
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.clearSubTasks()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}
