package com.ilseon.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.IdeaInboxViewModel
import com.ilseon.data.idea.Idea
import com.ilseon.ui.components.HtmlText
import com.ilseon.ui.theme.MutedTeal

@Composable
fun IdeaInboxScreen(
    viewModel: IdeaInboxViewModel = hiltViewModel(),
    onNavigateToNewTask: (String, String) -> Unit,
    showAddIdeaDialog: Boolean,
    onDismissAddIdeaDialog: () -> Unit,
    vttIdeaContent: String,
    onVttClick: () -> Unit,
) {
    val ideas by viewModel.ideas.collectAsState()
    var editingIdea by remember { mutableStateOf<Idea?>(null) }

    if (showAddIdeaDialog) {
        AddIdeaDialog(
            initialText = vttIdeaContent,
            onDismiss = onDismissAddIdeaDialog,
            onAddIdea = { content ->
                viewModel.addIdea(content)
                onDismissAddIdeaDialog()
            },
            onVttClick = onVttClick
        )
    }

    editingIdea?.let { idea ->
        EditIdeaDialog(
            idea = idea,
            onDismiss = { editingIdea = null },
            onSave = { updatedIdea ->
                viewModel.updateIdea(updatedIdea)
                editingIdea = null
            }
        )
    }

    IdeaInboxScreenContent(
        ideas = ideas,
        onConvertToTask = { idea ->
            viewModel.convertToTask(idea)
            val sentences = idea.content?.split(Regex("(?<=[.!?])\\s*"))
            val title = sentences?.firstOrNull() ?: idea.content
            val description = sentences?.size?.let {
                if (it > 1) {
                    sentences.drop(1).joinToString(" ")
                } else {
                    ""
                }
            }
            title?.let { description?.let { p2 -> onNavigateToNewTask(it, p2) } }
        },
        onDeleteIdea = { viewModel.deleteIdea(it) },
        onEditIdea = { editingIdea = it }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdeaInboxScreenContent(
    ideas: List<Idea>,
    onConvertToTask: (Idea) -> Unit,
    onDeleteIdea: (Idea) -> Unit,
    onEditIdea: (Idea) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Your Ideas",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You have ${ideas.size} ideas. Keep them coming!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (ideas.isEmpty()) {
            item {
                Text(
                    text = "No ideas yet. Jot down your thoughts and ideas here.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(ideas, key = { it.id }) { idea ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        idea.content?.let {
                            HtmlText(
                                html = it,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onConvertToTask(idea) }) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Convert to Task",
                                    tint = MutedTeal
                                )
                            }
                            IconButton(onClick = { onEditIdea(idea) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Idea",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteIdea(idea) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Idea",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditIdeaDialog(
    idea: Idea,
    onDismiss: () -> Unit,
    onSave: (Idea) -> Unit
) {
    var text by remember { mutableStateOf(idea.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Idea") },
        text = {
            TextField(
                value = text ?: "",
                onValueChange = { text = it },
                label = { Text("Idea") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(idea.copy(content = text)) },
                enabled = text?.isNotBlank() ?: false
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddIdeaDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onAddIdea: (String) -> Unit,
    onVttClick: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Idea") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Idea") },
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                trailingIcon = {
                    IconButton(onClick = onVttClick) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice To Text"
                        )
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { onAddIdea(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    LaunchedEffect(Unit) {
        if (initialText.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
}
