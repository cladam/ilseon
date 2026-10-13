package com.ilseon.ui.screen

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.data.routine.RoutineStepDraft
import com.ilseon.data.routine.RoutineTemplates
import com.ilseon.data.routine.RoutineWithSteps
import com.ilseon.data.task.TaskContext
import com.ilseon.ui.components.TimePickerDialog
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineDialog(
    contexts: List<TaskContext>,
    editingRoutine: RoutineWithSteps? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingId: UUID?,
        title: String,
        description: String?,
        contextId: UUID,
        startHour: Int?,
        startMinute: Int?,
        endHour: Int?,
        endMinute: Int?,
        daysOfWeek: String?,
        steps: List<RoutineStepDraft>
    ) -> Unit
) {
    var title by remember { mutableStateOf(editingRoutine?.routine?.title ?: "") }
    var description by remember { mutableStateOf(editingRoutine?.routine?.description ?: "") }
    var selectedContextId by remember {
        mutableStateOf(
            editingRoutine?.routine?.contextId ?: contexts.firstOrNull()?.id ?: UUID.randomUUID()
        )
    }

    var startHour by remember { mutableStateOf(editingRoutine?.routine?.startHour) }
    var startMinute by remember { mutableStateOf(editingRoutine?.routine?.startMinute) }
    var endHour by remember { mutableStateOf(editingRoutine?.routine?.endHour) }
    var endMinute by remember { mutableStateOf(editingRoutine?.routine?.endMinute) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val is24HourFormat = remember(context) {
        try {
            DateFormat.is24HourFormat(context)
        } catch (_: Exception) {
            true
        }
    }

    val startTimeState = rememberTimePickerState(
        initialHour = startHour ?: 7,
        initialMinute = startMinute ?: 0,
        is24Hour = is24HourFormat
    )
    val endTimeState = rememberTimePickerState(
        initialHour = endHour ?: 10,
        initialMinute = endMinute ?: 0,
        is24Hour = is24HourFormat
    )

    var steps by remember {
        mutableStateOf(
            editingRoutine?.sortedSteps?.map {
                RoutineStepDraft(it.title, it.targetDurationMinutes)
            } ?: emptyList()
        )
    }

    // New step inline input
    var newStepTitle by remember { mutableStateOf("") }
    var newStepDuration by remember { mutableIntStateOf(2) }

    // Bulk paste mode toggle
    var showBulkPasteArea by remember { mutableStateOf(false) }
    var bulkPasteText by remember { mutableStateOf("") }

    val isEditMode = editingRoutine != null

    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Routine Start Time",
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                startHour = startTimeState.hour
                startMinute = startTimeState.minute
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = startTimeState)
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Routine End Time (Optional)",
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                endHour = endTimeState.hour
                endMinute = endTimeState.minute
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = endTimeState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Routine" else "New Routine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Template Presets (Only shown in create mode to ease blank-page freeze)
                if (!isEditMode) {
                    Text(
                        text = "Quick Starter Templates",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoutineTemplates.allTemplates.forEach { template ->
                            SuggestionChip(
                                onClick = {
                                    title = template.title
                                    description = template.description
                                    steps = template.steps
                                    if (template.id == "morning_launch") {
                                        startHour = 7
                                        startMinute = 0
                                        endHour = 10
                                        endMinute = 0
                                    } else if (template.id == "evening_wind_down") {
                                        startHour = 20
                                        startMinute = 0
                                        endHour = 23
                                        endMinute = 0
                                    }
                                },
                                label = { Text(template.title, fontSize = 12.sp) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Routine Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Name (e.g. Morning Launch)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Routine Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Purpose / Motivation (optional)") },
                    placeholder = { Text("Why this routine matters or when to use it...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Active Hours / Window Configuration
                Text(
                    text = "Active Time Window (Optional)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showStartTimePicker = true },
                        label = {
                            Text(
                                text = if (startHour != null && startMinute != null) {
                                    String.format("Starts at %02d:%02d", startHour, startMinute)
                                } else {
                                    "Set Start Time"
                                },
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        trailingIcon = {
                            if (startHour != null) {
                                IconButton(
                                    onClick = {
                                        startHour = null
                                        startMinute = null
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    )

                    AssistChip(
                        onClick = { showEndTimePicker = true },
                        label = {
                            Text(
                                text = if (endHour != null && endMinute != null) {
                                    String.format("Until %02d:%02d", endHour, endMinute)
                                } else {
                                    "Until..."
                                },
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = {
                            if (endHour != null) {
                                IconButton(
                                    onClick = {
                                        endHour = null
                                        endMinute = null
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Context Selector Chips (Single scrollable row so multiple contexts never push steps offscreen)
                if (contexts.isNotEmpty()) {
                    Text(
                        text = "Focus Context",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        contexts.forEach { ctx ->
                            FilterChip(
                                selected = selectedContextId == ctx.id,
                                onClick = { selectedContextId = ctx.id },
                                label = { Text(ctx.name, fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Micro-Steps Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalMins = steps.sumOf { it.targetDurationMinutes ?: 0 }
                    Text(
                        text = "Steps (${steps.size})" + if (totalMins > 0) " • ~$totalMins min" else "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    TextButton(
                        onClick = { showBulkPasteArea = !showBulkPasteArea }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showBulkPasteArea) "Single Step" else "Paste List",
                            fontSize = 12.sp
                        )
                    }
                }

                // Bulk Paste Box (If active)
                AnimatedVisibility(visible = showBulkPasteArea) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        OutlinedTextField(
                            value = bulkPasteText,
                            onValueChange = { bulkPasteText = it },
                            label = { Text("Paste habits or notes (one per line)") },
                            placeholder = { Text("Drink water (2m)\nTake meds\nStretch (5m)") },
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (bulkPasteText.isNotBlank()) {
                                    val parsed = RoutineTemplates.parseBulkText(bulkPasteText)
                                    steps = steps + parsed
                                    bulkPasteText = ""
                                    showBulkPasteArea = false
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = bulkPasteText.isNotBlank()
                        ) {
                            Text("Import Steps")
                        }
                    }
                }

                // Inline Single-Step Input (Enter to Add)
                AnimatedVisibility(visible = !showBulkPasteArea) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newStepTitle,
                                onValueChange = { newStepTitle = it },
                                label = { Text("Add micro-step...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newStepTitle.isNotBlank()) {
                                            steps = steps + RoutineStepDraft(
                                                title = newStepTitle.trim(),
                                                targetDurationMinutes = newStepDuration
                                            )
                                            newStepTitle = ""
                                        }
                                    }
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (newStepTitle.isNotBlank()) {
                                        steps = steps + RoutineStepDraft(
                                            title = newStepTitle.trim(),
                                            targetDurationMinutes = newStepDuration
                                        )
                                        newStepTitle = ""
                                    }
                                },
                                enabled = newStepTitle.isNotBlank(),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (newStepTitle.isNotBlank()) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Step",
                                    tint = if (newStepTitle.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Duration Chips for the next step (horizontally scrollable so no chips get cut off)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pacing:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            listOf(1, 2, 5, 10, 15, 20, 30).forEach { mins ->
                                FilterChip(
                                    selected = newStepDuration == mins,
                                    onClick = { newStepDuration = mins },
                                    label = { Text("${mins}m", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }

                // Existing Steps List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (step.targetDurationMinutes != null) {
                                    Text(
                                        text = "${step.targetDurationMinutes} min target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Move Up
                            if (index > 0) {
                                IconButton(
                                    onClick = {
                                        val mutable = steps.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index - 1]
                                        mutable[index - 1] = temp
                                        steps = mutable
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Move Down
                            if (index < steps.lastIndex) {
                                IconButton(
                                    onClick = {
                                        val mutable = steps.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index + 1]
                                        mutable[index + 1] = temp
                                        steps = mutable
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Delete Step
                            IconButton(
                                onClick = {
                                    steps = steps.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Step",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            editingRoutine?.routine?.id,
                            title.trim(),
                            description.takeIf { it.isNotBlank() },
                            selectedContextId,
                            startHour,
                            startMinute,
                            endHour,
                            endMinute,
                            editingRoutine?.routine?.daysOfWeek,
                            steps
                        )
                    }
                },
                enabled = title.isNotBlank() && steps.isNotEmpty()
            ) {
                Text(if (isEditMode) "Save Changes" else "Create Routine")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
