package com.ilseon.ui.screen

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.TaskContextViewModel
import com.ilseon.data.task.DayOfWeek
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.TaskPriority
import com.ilseon.ui.components.DayPicker
import com.ilseon.ui.components.TimePickerDialog
import com.ilseon.ui.theme.toColor
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    onSave: (String, String?, UUID?, TaskPriority, String, String, Int?, Boolean, Set<DayOfWeek>, Boolean) -> Unit,
    viewModel: TaskContextViewModel = hiltViewModel(),
    initialTitle: String = "",
    initialDescription: String = "",
    onTitleVttClick: () -> Unit,
    onDescriptionVttClick: () -> Unit
) {
    val contextsWithFocusBlock by viewModel.contextsWithFocusBlock.collectAsState()
    val contexts = remember(contextsWithFocusBlock) {
        contextsWithFocusBlock.map { it.context }
    }
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedContextId by remember { mutableStateOf<UUID?>(null) }
    var priority by remember { mutableStateOf(TaskPriority.Medium) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var schedulingType by remember { mutableStateOf(SchedulingType.None) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isForTomorrow by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val is24HourFormat = DateFormat.is24HourFormat(LocalContext.current)
    val startTimeState = rememberTimePickerState(is24Hour = is24HourFormat)
    val endTimeState = rememberTimePickerState(is24Hour = is24HourFormat)

    val focusManager = LocalFocusManager.current
    val titleFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }

    // Combined LaunchedEffect to handle initialization and focus
    LaunchedEffect(contexts) {
        if (selectedContextId == null && contexts.isNotEmpty()) {
            selectedContextId = contexts.first().id
        }
        // Only request focus if the title is empty
        if (initialTitle.isEmpty()) {
            // A minimal delay is still useful to ensure the keyboard appears smoothly
            delay(50) 
            titleFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(initialTitle) {
        title = initialTitle
    }

    LaunchedEffect(initialDescription) {
        if (initialDescription.isNotEmpty()) {
            description = initialDescription
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                startTime = String.format("%02d:%02d", startTimeState.hour, startTimeState.minute)
                showStartTimePicker = false
            },
        ) {
            TimePicker(state = startTimeState)
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                endTime = String.format("%02d:%02d", endTimeState.hour, endTimeState.minute)
                showEndTimePicker = false
            },
        ) {
            TimePicker(state = endTimeState)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Quick Capture",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What's on your mind?") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocusRequester),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.secondary,
                focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            maxLines = 1,
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    descriptionFocusRequester.requestFocus()
                }
            ),
            trailingIcon = {
                IconButton(onClick = onTitleVttClick) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice To Text"
                    )
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(descriptionFocusRequester),
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.secondary,
                focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            maxLines = 5,
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            trailingIcon = {
                IconButton(onClick = onDescriptionVttClick) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice To Text"
                    )
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // --- SCHEDULING SECTION ---
        Text(
            "Scheduling",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SchedulingType.entries.forEach { type ->
                Row(
                    Modifier
                        .selectable(
                            selected = (type == schedulingType),
                            onClick = { schedulingType = type },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (type == schedulingType),
                        onClick = null
                    )
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Time Block specific fields
        AnimatedVisibility(visible = schedulingType == SchedulingType.TimeBlock) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TimePickerField(
                        time = startTime,
                        label = "Start Time",
                        onClick = { showStartTimePicker = true }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    TimePickerField(
                        time = endTime,
                        label = "End Time",
                        onClick = { showEndTimePicker = true }
                    )
                }
            }
        }

        // Duration specific fields
        AnimatedVisibility(visible = schedulingType == SchedulingType.Duration) {
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter { char -> char.isDigit() } },
                label = { Text("Duration (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.secondary,
                    focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                ),
                maxLines = 1
            )
        }

        // --- TOMORROW CHECKBOX ---
        AnimatedVisibility(visible = schedulingType != SchedulingType.None) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isForTomorrow = !isForTomorrow }
            ) {
                Checkbox(
                    checked = isForTomorrow,
                    onCheckedChange = { isForTomorrow = it }
                )
                Text("Tomorrow?")
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- RECURRING SECTION ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isRecurring = !isRecurring }
        ) {
            Checkbox(
                checked = isRecurring,
                onCheckedChange = { isRecurring = it }
            )
            Text("Recurring Task?")
        }

        AnimatedVisibility(visible = isRecurring) {
            Column {
                Spacer(Modifier.height(16.dp))

                // Show Start Time field for Normal and Duration recurring tasks
                if (schedulingType != SchedulingType.TimeBlock) {
                    TimePickerField(
                        time = startTime,
                        label = "Start Time",
                        onClick = { showStartTimePicker = true }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    "Repeat on",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                DayPicker(
                    selectedDays = selectedDays.toList(),
                    onDaySelected = { day ->
                        selectedDays = if (selectedDays.contains(day)) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Task will recur every week on the selected days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- CONTEXT SECTION ---
        Text(
            "Context",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            contexts.chunked(3).forEach { rowContexts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowContexts.forEach { ctx ->
                        Button(
                            onClick = { selectedContextId = ctx.id },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedContextId == ctx.id) MaterialTheme.colorScheme.secondary.copy(
                                    alpha = 0.2f
                                ) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedContextId == ctx.id) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.7f
                                )
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Text(ctx.name)
                        }
                    }
                    // Add spacers to fill the row if there are less than 3 items
                    repeat(3 - rowContexts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- PRIORITY SECTION ---
        Text(
            "Priority",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskPriority.entries.forEach { prio ->
                Button(
                    onClick = { priority = prio },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (priority == prio) prio.toColor() else MaterialTheme.colorScheme.surface,
                        contentColor = if (priority == prio) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(prio.name)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val durationInt = duration.toIntOrNull()
                val days = if (isRecurring) {
                    selectedDays.map {
                        DayOfWeek.valueOf(java.time.DayOfWeek.of(it).name)
                    }.toSet()
                } else {
                    emptySet()
                }

                onSave(
                    title,
                    description.takeIf { it.isNotBlank() },
                    selectedContextId,
                    priority,
                    startTime,
                    endTime,
                    durationInt,
                    isRecurring,
                    days,
                    isForTomorrow
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = title.isNotBlank() && selectedContextId != null
        ) {
            Text("Save Task", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TimePickerField(time: String, label: String, onClick: () -> Unit) {
    Box {
        OutlinedTextField(
            value = time,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.secondary,
                focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.3f
                ),
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.7f
                ),
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.7f
                )
            ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}
