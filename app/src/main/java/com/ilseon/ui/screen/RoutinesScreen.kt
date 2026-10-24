package com.ilseon.ui.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.RoutineRunnerUiState
import com.ilseon.RoutineViewModel
import com.ilseon.data.routine.RoutineWithSteps
import com.ilseon.ui.components.RoutineRunnerOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier,
    isRightHanded: Boolean = true
) {
    val activeRoutines by viewModel.activeRoutines.collectAsState()
    val contexts by viewModel.contexts.collectAsState()
    val isDialogOpen by viewModel.isCreateRoutineDialogOpen.collectAsState()
    val editingRoutine by viewModel.editingRoutine.collectAsState()
    val runnerUiState by viewModel.runnerUiState.collectAsState()
    val runningRoutine by viewModel.runningRoutine.collectAsState()
    val currentRun by viewModel.currentRun.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButtonPosition = if (isRightHanded) androidx.compose.material3.FabPosition.End else androidx.compose.material3.FabPosition.Start,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { viewModel.openCreateRoutineDialog() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "New Routine",
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "NEW ROUTINE",
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Routines",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily habit playlists with zero maintenance fatigue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeRoutines.isEmpty()) {
                // Calm Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No routines yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Routines run on a rolling cycle and reset automatically each morning at 4:00 AM.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.openCreateRoutineDialog() }
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create with a Template")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeRoutines, key = { it.routine.id }) { item ->
                        RoutineCard(
                            routineWithSteps = item,
                            onPlay = { viewModel.startRoutinePreview(item) },
                            onEdit = { viewModel.openEditRoutineDialog(item) },
                            onArchive = { viewModel.archiveRoutine(item.routine.id) },
                            onDelete = { viewModel.deleteRoutine(item.routine) }
                        )
                    }
                }
            }
        }
    }

    if (isDialogOpen) {
        CreateRoutineSheet(
            contexts = contexts,
            editingRoutine = editingRoutine,
            onDismiss = { viewModel.closeCreateRoutineDialog() },
            onSave = { existingId, title, description, contextId, startHour, startMinute, endHour, endMinute, daysOfWeek, steps ->
                viewModel.saveRoutine(
                    existingRoutineId = existingId,
                    title = title,
                    description = description,
                    contextId = contextId,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    daysOfWeek = daysOfWeek,
                    stepDrafts = steps
                )
            }
        )
    }

    if (runnerUiState == RoutineRunnerUiState.FullOverlay && runningRoutine != null && currentRun != null) {
        RoutineRunnerOverlay(
            routineWithSteps = runningRoutine!!,
            todayRun = currentRun!!,
            onStepCompleted = { step, nextIndex ->
                viewModel.onStepCompleted(step, nextIndex)
            },
            onStepSkipped = { step, nextIndex ->
                viewModel.onStepSkipped(step, nextIndex)
            },
            onPacingElapsed = {
                viewModel.onStepPacingElapsed()
            },
            onFinishRoutine = {
                viewModel.finishRoutine()
            },
            onMinimize = {
                viewModel.minimizeRunner()
            }
        )
    }
}

@Composable
private fun RoutineCard(
    routineWithSteps: RoutineWithSteps,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routineWithSteps.routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    routineWithSteps.routine.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test / Preview Routine",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Routine",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onArchive, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive Routine",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step count & duration badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${routineWithSteps.steps.size} steps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (routineWithSteps.totalEstimatedMinutes > 0) {
                    Text(
                        text = "•  ~${routineWithSteps.totalEstimatedMinutes} mins total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (routineWithSteps.routine.startHour != null && routineWithSteps.routine.startMinute != null) {
                    val timeStr = if (routineWithSteps.routine.endHour != null && routineWithSteps.routine.endMinute != null) {
                        String.format(
                            "•  %02d:%02d - %02d:%02d",
                            routineWithSteps.routine.startHour,
                            routineWithSteps.routine.startMinute,
                            routineWithSteps.routine.endHour,
                            routineWithSteps.routine.endMinute
                        )
                    } else {
                        String.format(
                            "•  Starts %02d:%02d",
                            routineWithSteps.routine.startHour,
                            routineWithSteps.routine.startMinute
                        )
                    }
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Preview of the first 3 steps
            if (routineWithSteps.sortedSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    routineWithSteps.sortedSteps.take(3).forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. ${step.title}" + if (step.targetDurationMinutes != null) " (${step.targetDurationMinutes}m)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    if (routineWithSteps.sortedSteps.size > 3) {
                        Text(
                            text = "+ ${routineWithSteps.sortedSteps.size - 3} more...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
