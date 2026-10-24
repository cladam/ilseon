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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.ContextWithFocusBlock
import com.ilseon.ui.components.TimePickerDialog
import com.ilseon.ui.theme.MutedTeal
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextBottomSheet(
    editingContext: ContextWithFocusBlock? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String?,
        startTime: String?,
        endTime: String?,
        repeatDays: List<Int>?
    ) -> Unit,
    onVttNameClick: () -> Unit,
    onVttDescriptionClick: () -> Unit,
    initialVttName: String = "",
    initialVttDescription: String = ""
) {
    var name by remember { mutableStateOf(editingContext?.context?.name ?: "") }
    var description by remember { mutableStateOf(editingContext?.context?.description ?: "") }

    var focusBlockEnabled by remember { mutableStateOf(editingContext?.focusBlock != null) }
    var startTime by remember { mutableStateOf(editingContext?.focusBlock?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(editingContext?.focusBlock?.endTime ?: "17:00") }
    var repeatDays by remember { mutableStateOf(editingContext?.focusBlock?.repeatDays ?: emptyList()) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val isEditing = editingContext != null
    val context = LocalContext.current
    val is24HourFormat = remember(context) {
        try {
            DateFormat.is24HourFormat(context)
        } catch (_: Exception) {
            true
        }
    }

    val startTimeState = rememberTimePickerState(
        initialHour = startTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9,
        initialMinute = startTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = is24HourFormat
    )
    val endTimeState = rememberTimePickerState(
        initialHour = endTime.split(":").getOrNull(0)?.toIntOrNull() ?: 17,
        initialMinute = endTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = is24HourFormat
    )

    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (name.isEmpty()) {
            delay(300)
            nameFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(initialVttName) {
        if (initialVttName.isNotBlank()) name = initialVttName
    }
    LaunchedEffect(initialVttDescription) {
        if (initialVttDescription.isNotBlank()) description = initialVttDescription
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Focus Block Start Time",
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                startTime = String.format("%02d:%02d", startTimeState.hour, startTimeState.minute)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = startTimeState)
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Focus Block End Time",
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                endTime = String.format("%02d:%02d", endTimeState.hour, endTimeState.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = endTimeState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (isEditing) "Edit Context" else "New Context",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Structure your day into dedicated focus environments.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Context Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Context Name (e.g. Work, Morning, Health)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = onVttNameClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice To Text")
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = onVttDescriptionClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice To Text")
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // Focus Block Toggle
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = focusBlockEnabled,
                        onClick = { focusBlockEnabled = !focusBlockEnabled },
                        role = Role.Checkbox
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = focusBlockEnabled,
                    onCheckedChange = null
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = "Enable Focus Block Schedule",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Activates priority tasks and routines during this window.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = focusBlockEnabled) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
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
                                readOnly = true
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
                                readOnly = true
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showEndTimePicker = true }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Active Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    DayPicker(
                        selectedDays = repeatDays,
                        onDaySelected = { day ->
                            repeatDays = if (repeatDays.contains(day)) {
                                repeatDays.filter { it != day }
                            } else {
                                repeatDays + day
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val st = if (focusBlockEnabled) startTime else null
                        val et = if (focusBlockEnabled) endTime else null
                        val desc = description.ifBlank { null }
                        val days = if (focusBlockEnabled) repeatDays else null
                        onSave(name.trim(), desc, st, et, days)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MutedTeal)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Create Context",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
