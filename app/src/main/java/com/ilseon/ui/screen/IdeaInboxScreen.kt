package com.ilseon.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.IdeaInboxViewModel
import com.ilseon.data.idea.Idea
import com.ilseon.ui.components.AppCard
import com.ilseon.ui.components.MarkdownText
import com.ilseon.ui.theme.MutedTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IdeaInboxScreen(
    viewModel: IdeaInboxViewModel = hiltViewModel(),
    onNavigateToNewTask: (String, String) -> Unit,
    showAddIdeaDialog: Boolean,
    onDismissAddIdeaDialog: () -> Unit,
    vttIdeaContent: String,
    onVttClick: () -> Unit,
    onSwipeUp: () -> Unit,
) {
    val ideas by viewModel.ideas.collectAsState()
    var editingIdea by remember { mutableStateOf<Idea?>(null) }
    var currentView by remember { mutableStateOf("Inbox") }
    val selectedTabIndex = if (currentView == "Inbox") 0 else 1

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -40) { // Swipe up threshold
                        onSwipeUp()
                    }
                }
            }
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = "Your Ideas",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        ) {
            Tab(
                text = { Text("Inbox") },
                selected = currentView == "Inbox",
                onClick = { currentView = "Inbox" },
                selectedContentColor = MaterialTheme.colorScheme.secondary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                text = { Text("Notes") },
                selected = currentView == "Notes",
                onClick = { currentView = "Notes" },
                selectedContentColor = MaterialTheme.colorScheme.secondary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val filteredIdeas = ideas.filter {
            if (currentView == "Inbox") !it.isReference else it.isReference
        }.sortedByDescending { it.isPinned }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredIdeas.isEmpty()) {
                item {
                    Text(
                        text = "No ideas yet. Jot down your thoughts and ideas here.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredIdeas, key = { it.id }) { idea ->
                    AppCard(
                        modifier = Modifier
                            .animateItem(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            idea.content?.let {
                                MarkdownText(
                                    markdown = it,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(idea.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row {
                                    if (currentView == "Inbox") {
                                        IconButton(onClick = { viewModel.saveAsReference(idea) }) {
                                            Icon(
                                                imageVector = Icons.Default.Save,
                                                contentDescription = "Save as Note",
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = { viewModel.togglePin(idea) }) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = "Pin Idea",
                                                tint = if (idea.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        viewModel.convertToTask(idea)
                                        val sentences = idea.content?.split(Regex("(?<=[.!?])\\s*"))
                                        val title = sentences?.firstOrNull() ?: idea.content ?: ""
                                        val description = if ((sentences?.size ?: 0) > 1) {
                                            sentences!!.drop(1).joinToString(" ")
                                        } else {
                                            ""
                                        }
                                        onNavigateToNewTask(title, description)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Convert to Task",
                                            tint = MutedTeal
                                        )
                                    }
                                    IconButton(onClick = { editingIdea = idea }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Idea",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteIdea(idea) }) {
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIdeaDialog(
    idea: Idea,
    onDismiss: () -> Unit,
    onSave: (Idea) -> Unit
) {
    var text by remember { mutableStateOf(idea.content ?: "") }
    val title = if (idea.isReference) "Edit Note" else "Edit Idea"

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(idea.copy(content = text)) },
                            enabled = text.isNotBlank()
                        ) {
                            Text(
                                text = "Save",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                placeholder = { Text("Jot down your idea...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIdeaDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onAddIdea: (String) -> Unit,
    onVttClick: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("New Idea") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onVttClick) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice To Text"
                            )
                        }
                        TextButton(
                            onClick = { onAddIdea(text) },
                            enabled = text.isNotBlank()
                        ) {
                            Text(
                                text = "Save",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Jot down your idea...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        if (initialText.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
}
