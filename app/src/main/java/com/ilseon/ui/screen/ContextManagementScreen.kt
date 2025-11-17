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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.ContextWithFocusBlock
import com.ilseon.TaskContextViewModel
import com.ilseon.ui.components.HtmlText
import com.ilseon.ui.components.TimePickerDialog
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextManagementScreen(
    viewModel: TaskContextViewModel = hiltViewModel()
) {
    val contextsWithFocusBlock by viewModel.contextsWithFocusBlock.collectAsState()
    var newContextName by remember { mutableStateOf("") }
    var newContextDescription by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var focusBlockEnabled by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var newRepeatDays by remember { mutableStateOf<List<Int>>(emptyList()) }
    var editingContext by remember { mutableStateOf<ContextWithFocusBlock?>(null) }
    val isEditing = editingContext != null

    val is24HourFormat = DateFormat.is24HourFormat(LocalContext.current)
    val startTimeState = rememberTimePickerState(is24Hour = is24HourFormat)
    val endTimeState = rememberTimePickerState(is24Hour = is24HourFormat)

    fun resetForm() {
        newContextName = ""
        newContextDescription = ""
        startTime = ""
        endTime = ""
        focusBlockEnabled = false
        newRepeatDays = emptyList()
        editingContext = null
    }

    LaunchedEffect(editingContext) {
        editingContext?.let { contextToEdit ->
            newContextName = contextToEdit.context.name
            newContextDescription = contextToEdit.context.description ?: ""
            focusBlockEnabled = contextToEdit.focusBlock != null
            if (contextToEdit.focusBlock != null) {
                startTime = contextToEdit.focusBlock.startTime
                endTime = contextToEdit.focusBlock.endTime
                newRepeatDays = contextToEdit.focusBlock.repeatDays
            } else {
                startTime = ""
                endTime = ""
                newRepeatDays = emptyList()
            }
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
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Manage Contexts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(24.dp))

        // Add new context section
        Column {
            OutlinedTextField(
                value = newContextName,
                onValueChange = { newContextName = it },
                label = { Text(if (isEditing) "Context name" else "New context name") },
                modifier = Modifier.fillMaxWidth(),
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
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = newContextDescription,
                onValueChange = { newContextDescription = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
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
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = focusBlockEnabled,
                        onClick = { focusBlockEnabled = !focusBlockEnabled },
                        role = Role.Checkbox
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = focusBlockEnabled,
                    onCheckedChange = null
                )
                Text(
                    text = "Enable Focus Block",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = focusBlockEnabled) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = {},
                                label = { Text("Start Time") },
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
                                    .clickable { showStartTimePicker = true }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = {},
                                label = { Text("End Time") },
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
                                    .clickable { showEndTimePicker = true }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Repeat Days",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    DayPicker(
                        selectedDays = newRepeatDays,
                        onDaySelected = { day ->
                            newRepeatDays = if (newRepeatDays.contains(day)) {
                                newRepeatDays.filter { it != day }
                            } else {
                                newRepeatDays + day
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (newContextName.isNotBlank()) {
                            val st = if (focusBlockEnabled) startTime else null
                            val et = if (focusBlockEnabled) endTime else null
                            val desc = newContextDescription.ifBlank { null }
                            val days = if (focusBlockEnabled) newRepeatDays else null
                            if (isEditing) {
                                editingContext?.let {
                                    viewModel.updateContext(
                                        it.context.id,
                                        newContextName,
                                        desc,
                                        st,
                                        et,
                                        days
                                    )
                                }
                            } else {
                                viewModel.addContext(newContextName, desc, st, et, days)
                            }
                            resetForm()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = newContextName.isNotBlank()
                ) {
                    Text(if (isEditing) "Update" else "Add Context", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                if (isEditing) {
                    Button(
                        onClick = { resetForm() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                    ) {
                        Text("Cancel", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // List of existing contexts
        Text(
            text = "Existing Contexts",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        if (contextsWithFocusBlock.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No contexts yet. Add one above!",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contextsWithFocusBlock, key = { it.context.id }) { item ->
                    ContextItem(
                        contextWithFocusBlock = item,
                        onDelete = { viewModel.deleteContext(item.context.id) },
                        onEdit = { editingContext = item }
                    )
                }
            }
        }
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
        val days = DayOfWeek.values()
        days.forEach { day ->
            val isSelected = selectedDays.contains(day.value)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Gray.copy(
                            alpha = 0.2f
                        ),
                        shape = CircleShape
                    )
                    .clickable { onDaySelected(day.value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ContextItem(
    contextWithFocusBlock: ContextWithFocusBlock,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val context = contextWithFocusBlock.context
    val focusBlock = contextWithFocusBlock.focusBlock
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
    ) {
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
                    HtmlText(
                        html = it
                    )
                }
                focusBlock?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    val days = if (it.repeatDays.isNotEmpty()) {
                        it.repeatDays.sorted().joinToString(", ") { dayOfWeek ->
                            DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        }
                    } else {
                        "Not repeating"
                    }
                    Text(
                        text = "Focus: ${it.startTime} - ${it.endTime} ($days)",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = { onEdit() }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit context",
                    tint = MaterialTheme.colorScheme.primary,
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
