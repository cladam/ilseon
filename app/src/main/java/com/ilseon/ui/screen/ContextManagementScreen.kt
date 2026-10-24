package com.ilseon.ui.screen

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.ContextWithFocusBlock
import com.ilseon.TaskContextViewModel
import com.ilseon.data.task.Task
import com.ilseon.ui.components.AppCard
import com.ilseon.ui.components.MarkdownText
import com.ilseon.ui.components.TimePickerDialog
import com.ilseon.ui.theme.extendedColors
import androidx.compose.ui.platform.LocalLocale
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextManagementScreen(
    viewModel: TaskContextViewModel = hiltViewModel(),
    onNewContextVttClick: () -> Unit,
    onNewContextDescriptionVttClick: () -> Unit,
    initialContextName: String = "",
    initialContextDescription: String = "",
    onNavigateToOngoingTasks: (String) -> Unit,
    isRightHanded: Boolean = true
) {
    val contextsWithFocusBlock by viewModel.contextsWithFocusBlock.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var editingContext by remember { mutableStateOf<ContextWithFocusBlock?>(null) }

    val showTasksDialog by viewModel.showTasksDialog.collectAsState()
    val tasks by viewModel.selectedContextTasks.collectAsState()

    if (showTasksDialog) {
        TasksDialog(
            tasks = tasks,
            onDismiss = { viewModel.dismissTasksDialog() }
        )
    }

    LaunchedEffect(initialContextName, initialContextDescription) {
        if (initialContextName.isNotBlank() || initialContextDescription.isNotBlank()) {
            showBottomSheet = true
        }
    }

    Scaffold(
        floatingActionButtonPosition = if (isRightHanded) FabPosition.End else FabPosition.Start,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable {
                        editingContext = null
                        showBottomSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "New Context",
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "NEW CONTEXT",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Manage Contexts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Dedicated focus blocks and environments for your tasks and routines.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(20.dp))

            if (contextsWithFocusBlock.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No contexts yet. Tap + to create one!",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contextsWithFocusBlock, key = { it.context.id }) { item ->
                        ContextItem(
                            contextWithFocusBlock = item,
                            onDelete = { viewModel.deleteContext(item.context.id) },
                            onEdit = {
                                editingContext = item
                                showBottomSheet = true
                            },
                            onNavigateToOngoingTasks = { onNavigateToOngoingTasks(item.context.id.toString()) }
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ContextBottomSheet(
            editingContext = editingContext,
            onDismiss = {
                showBottomSheet = false
                editingContext = null
            },
            onSave = { name, desc, st, et, days ->
                if (editingContext != null) {
                    viewModel.updateContext(
                        editingContext!!.context.id,
                        name,
                        desc,
                        st,
                        et,
                        days
                    )
                } else {
                    viewModel.addContext(name, desc, st, et, days)
                }
                showBottomSheet = false
                editingContext = null
            },
            onVttNameClick = onNewContextVttClick,
            onVttDescriptionClick = onNewContextDescriptionVttClick,
            initialVttName = initialContextName,
            initialVttDescription = initialContextDescription
        )
    }
}

@Composable
fun DayPicker(
    selectedDays: List<Int>,
    onDaySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val days = DayOfWeek.entries.toTypedArray()
        days.forEach { day ->
            val isSelected = selectedDays.contains(day.value)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                            alpha = 0.2f
                        ),
                        shape = CircleShape
                    )
                    .clickable { onDaySelected(day.value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, LocalLocale.current.platformLocale),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ContextItem(
    contextWithFocusBlock: ContextWithFocusBlock,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onNavigateToOngoingTasks: () -> Unit
) {
    val context = contextWithFocusBlock.context
    val focusBlock = contextWithFocusBlock.focusBlock
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = context.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                context.description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    MarkdownText(
                        markdown = it
                    )
                }
                focusBlock?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    val currentLocale = LocalLocale.current.platformLocale
                    val days = if (it.repeatDays.isNotEmpty()) {
                        it.repeatDays.sorted().joinToString(", ") { dayOfWeek ->
                            DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.SHORT, currentLocale)
                        }
                    } else {
                        "Not repeating"
                    }
                    Text(
                        text = "Focus: ${it.startTime} - ${it.endTime} ($days)",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onNavigateToOngoingTasks) {
                Icon(
                    imageVector = Icons.Default.Task,
                    contentDescription = "View ongoing tasks",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { onEdit() }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit context",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { onDelete() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete context",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TasksDialog(
    tasks: List<Task>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Tasks") },
        text = {
            if (tasks.isEmpty()) {
                Text("No open tasks for this context.")
            } else {
                LazyColumn {
                    items(tasks) { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (task.isUrgent) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Urgent",
                                    tint = MaterialTheme.extendedColors.priorityHigh,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(task.title)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
