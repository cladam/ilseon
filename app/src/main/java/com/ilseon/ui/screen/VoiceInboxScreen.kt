package com.ilseon.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.NavigationEvent
import com.ilseon.VoiceMemoViewModel
import com.ilseon.data.task.ExtractedTasks
import com.ilseon.data.task.TaskContext
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.ui.components.GravitySwipeBox
import com.ilseon.ui.components.VoiceMemoCard
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceInboxScreen(
    viewModel: VoiceMemoViewModel = hiltViewModel(),
    onNavigateToNewTask: (String, String) -> Unit,
    onNavigateToIdea: (UUID) -> Unit,
    onNavigateToDashboard: () -> Unit,
    initialMemoIdToPlay: String? = null,
) {
    val voiceMemos by viewModel.voiceMemos.collectAsState()
    val currentlyPlayingId by viewModel.currentlyPlayingId.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    var memoToEdit by remember { mutableStateOf<VoiceMemo?>(null) }
    val transcribingMemoId by viewModel.transcribingMemoId.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    val isApiKeySet by viewModel.isApiKeySet.collectAsState()
    val extractedTasks by viewModel.extractedTasks.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val taskContexts by viewModel.taskContexts.collectAsState()
    var selectedContextId by remember { mutableStateOf<UUID?>(null) }

    LaunchedEffect(initialMemoIdToPlay, voiceMemos) {
        initialMemoIdToPlay?.let { memoId ->
            voiceMemos.find { it.id == memoId }?.let { memo ->
                viewModel.onPlayPause(memo)
            }
        }
    }

    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is NavigationEvent.ToDashboard -> {
                onNavigateToDashboard()
                viewModel.resetTranscriptionResult()
            }

            is NavigationEvent.ToNote -> {
                onNavigateToIdea(event.ideaId)
                viewModel.resetTranscriptionResult()
            }

            null -> { /* Do nothing */ }
        }
    }

    if (memoToEdit != null) {
        EditVoiceMemoDialog(
            memo = memoToEdit!!,
            taskContexts = taskContexts,
            onDismiss = { memoToEdit = null },
            onSave = { memo, title, contextId ->
                viewModel.updateVoiceMemo(memo, title, contextId)
                memoToEdit = null
            }
        )
    }

    if (extractedTasks != null) {
        ExtractedTasksDialog(
            tasks = extractedTasks!!,
            onDismiss = { viewModel.dismissExtractedTasks() },
            onSave = { viewModel.saveExtractedTasks(it) }
        )
    }

    val filteredMemos = remember(voiceMemos, selectedContextId) {
        voiceMemos.filter { memo ->
            selectedContextId == null || memo.contextId == selectedContextId
        }.sortedWith(compareByDescending { it.weight })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Screen Title
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Voice Inbox",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "You have ${voiceMemos.size} voice memos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ContextFilterDropdown(
                    selectedContextId = selectedContextId,
                    taskContexts = taskContexts,
                    onContextSelected = { selectedContextId = it }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Voice Memo List
            if (filteredMemos.isEmpty()) {
                item {
                    Text(
                        text = "No voice memos yet. Tap the mic to record your thoughts.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredMemos, key = { it.id }) { memo ->
                    GravitySwipeBox(
                        onSwipeRight = { viewModel.increaseWeight(memo) },
                        onSwipeLeft = { viewModel.decreaseWeight(memo) }
                    ) {
                        val isPlaying = memo.id == currentlyPlayingId
                        val isTranscribing = memo.id == transcribingMemoId
                        val taskContext = taskContexts.find { it.id == memo.contextId }
                        VoiceMemoCard(
                            memo = memo,
                            isPlaying = isPlaying && !isPaused,
                            isTranscribing = isTranscribing,
                            progress = if (isPlaying) progress else 0f,
                            onPlayPause = { viewModel.onPlayPause(it) },
                            onSeek = { newProgress -> viewModel.seekTo(newProgress) },
                            onConvertToTask = {
                                val description = "[Play Recording](ilseon://play-voice-memo/${it.id})"
                                onNavigateToNewTask(it.title, description)
                            },
                            onDelete = { viewModel.deleteVoiceMemo(it) },
                            onEdit = { memoToEdit = it },
                            onTranscribe = { viewModel.transcribeMemo(it) },
                            showTranscribeOption = isApiKeySet,
                            taskContext = taskContext,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVoiceMemoDialog(
    memo: VoiceMemo,
    taskContexts: List<TaskContext>,
    onDismiss: () -> Unit,
    onSave: (VoiceMemo, String, UUID?) -> Unit
) {
    var title by remember(memo) { mutableStateOf(memo.title) }
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var selectedContextId by remember { mutableStateOf(memo.contextId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Voice Memo") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Voice Memo Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = contextMenuExpanded,
                    onExpandedChange = { contextMenuExpanded = !contextMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = taskContexts.find { it.id == selectedContextId }?.name ?: "No context",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Context") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contextMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = contextMenuExpanded,
                        onDismissRequest = { contextMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("No context") },
                            onClick = {
                                selectedContextId = null
                                contextMenuExpanded = false
                            }
                        )
                        taskContexts.forEach { context ->
                            DropdownMenuItem(
                                text = { Text(context.name) },
                                onClick = {
                                    selectedContextId = context.id
                                    contextMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(memo, title, selectedContextId) }) {
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
private fun ExtractedTasksDialog(
    tasks: ExtractedTasks,
    onDismiss: () -> Unit,
    onSave: (ExtractedTasks) -> Unit
) {
    var selectedTasks by remember { mutableStateOf(tasks.tasks.toSet()) }
    val allSelected = selectedTasks.size == tasks.tasks.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    "Suggested Tasks",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        selectedTasks = if (allSelected) {
                            emptySet()
                        } else {
                            tasks.tasks.toSet()
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (allSelected) "Deselect All" else "Select All")
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(vertical = 8.dp)
                ) {
                    items(tasks.tasks) { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTasks = if (selectedTasks.contains(task)) {
                                        selectedTasks - task
                                    } else {
                                        selectedTasks + task
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedTasks.contains(task),
                                onCheckedChange = { isChecked ->
                                    selectedTasks = if (isChecked) {
                                        selectedTasks + task
                                    } else {
                                        selectedTasks - task
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colorScheme.primary,
                                    uncheckedColor = colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${task.title} (Priority: ${task.priority}, Effort: ${task.effort})",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val tasksToSave = ExtractedTasks(tasks = selectedTasks.toList())
                            onSave(tasksToSave)
                        },
                        enabled = selectedTasks.isNotEmpty()
                    ) {
                        Text("Save (${selectedTasks.size})")
                    }
                }
            }
        }
    }
}
