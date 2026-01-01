package com.ilseon.ui.screen

import android.content.Intent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.IdeaInboxViewModel
import com.ilseon.NavigationEvent
import com.ilseon.VoiceMemoViewModel
import com.ilseon.data.idea.Idea
import com.ilseon.ui.components.AppCard
import com.ilseon.ui.components.GravitySwipeBox
import com.ilseon.ui.components.MarkdownText
import com.ilseon.ui.theme.MutedTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Helper functions for markdown formatting
private fun applyMarkdown(textFieldValue: TextFieldValue, prefix: String, suffix: String = prefix): TextFieldValue {
    val selection = textFieldValue.selection
    val newText = if (selection.collapsed) {
        textFieldValue.text.substring(0, selection.start) +
                prefix + suffix +
                textFieldValue.text.substring(selection.end)
    } else {
        textFieldValue.text.substring(0, selection.min) +
                prefix +
                textFieldValue.text.substring(selection.min, selection.max) +
                suffix +
                textFieldValue.text.substring(selection.max)
    }
    val newSelection = if (selection.collapsed) {
        TextRange(selection.start + prefix.length)
    } else {
        TextRange(selection.min + prefix.length, selection.max + prefix.length)
    }
    return textFieldValue.copy(text = newText, selection = newSelection)
}

private fun applyHeading(textFieldValue: TextFieldValue): TextFieldValue {
    val selection = textFieldValue.selection
    val text = textFieldValue.text
    val currentLineStart = text.lastIndexOf('\n', selection.start - 1).let { if (it == -1) 0 else it + 1 }
    val newText = text.take(currentLineStart) + "## " + text.substring(currentLineStart)
    val newSelection = TextRange(selection.start + 3, selection.end + 3)
    return textFieldValue.copy(text = newText, selection = newSelection)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IdeaInboxScreen(
    viewModel: IdeaInboxViewModel = hiltViewModel(),
    voiceMemoViewModel: VoiceMemoViewModel = hiltViewModel(),
    onNavigateToNewTask: (String, String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    showAddIdeaDialog: Boolean,
    onDismissAddIdeaDialog: () -> Unit,
    vttIdeaContent: String,
    onVttClick: () -> Unit,
    onSwipeUp: () -> Unit,
    newIdeaId: UUID? = null
) {
    val ideas by viewModel.ideas.collectAsState()
    var editingIdea by remember { mutableStateOf<Idea?>(null) }
    var currentView by remember { mutableStateOf("Inbox") }
    val lazyListState = rememberLazyListState()

    val navigationEvent by voiceMemoViewModel.navigationEvent.collectAsState()
    var ideaIdFromMemo by remember { mutableStateOf<UUID?>(null) }

    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is NavigationEvent.ToDashboard -> {
                onNavigateToDashboard()
                voiceMemoViewModel.resetTranscriptionResult()
            }
            is NavigationEvent.ToNote -> {
                currentView = "Notes"
                ideaIdFromMemo = event.ideaId
                voiceMemoViewModel.resetTranscriptionResult()
            }
            null -> { /* Do nothing */ }
        }
    }

    val finalNewIdeaId = newIdeaId ?: ideaIdFromMemo

    val ideaToShow = remember(finalNewIdeaId, ideas) {
        finalNewIdeaId?.let { id -> ideas.find { it.id == id } }
    }

    LaunchedEffect(ideaToShow) {
        ideaToShow?.let {
            if (it.isReference) {
                currentView = "Notes"
            }
        }
    }

    val filteredIdeas = remember(currentView, ideas) {
        ideas.filter {
            if (currentView == "Inbox") !it.isReference else it.isReference
        }.sortedWith(
            compareByDescending<Idea> { it.isPinned }
                .thenByDescending { it.weight }
        )
    }

    LaunchedEffect(filteredIdeas, finalNewIdeaId) {
        finalNewIdeaId?.let { id ->
            val index = filteredIdeas.indexOfFirst { it.id == id }
            if (index != -1) {
                lazyListState.animateScrollToItem(index)
            }
        }
    }

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

        val selectedTabIndex = if (currentView == "Inbox") 0 else 1
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

        LazyColumn(
            state = lazyListState,
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
                    val context = LocalContext.current
                    GravitySwipeBox(
                        onSwipeRight = { viewModel.increaseWeight(idea) },
                        onSwipeLeft = { viewModel.decreaseWeight(idea) }
                    ) {
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
                                        text = SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm",
                                            Locale.getDefault()
                                        ).format(Date(idea.createdAt)),
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
                                            IconButton(onClick = {
                                                viewModel.convertToTask(idea)
                                                val sentences =
                                                    idea.content?.split(Regex("(?<=[.!?])\\s*"))
                                                val title =
                                                    sentences?.firstOrNull() ?: idea.content ?: ""
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
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, idea.content ?: "")
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, null)
                                            context.startActivity(shareIntent)
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share Idea",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
fun FormattingBar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderscoreClick: () -> Unit,
    onHeadingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onBoldClick) {
            Icon(
                imageVector = Icons.Outlined.FormatBold,
                contentDescription = "Bold"
            )
        }
        IconButton(onClick = onItalicClick) {
            Icon(
                imageVector = Icons.Outlined.FormatItalic,
                contentDescription = "Italic"
            )
        }
        IconButton(onClick = onUnderscoreClick) {
            Icon(
                imageVector = Icons.Outlined.FormatUnderlined,
                contentDescription = "Underscore"
            )
        }
        IconButton(onClick = onHeadingClick) {
            Icon(
                imageVector = Icons.Outlined.Title,
                contentDescription = "Heading"
            )
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
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = idea.content ?: "",
                selection = TextRange(idea.content?.length ?: 0)
            )
        )
    }
    val title = if (idea.isReference) "Edit Note" else "Edit Idea"

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
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
                            onClick = { onSave(idea.copy(content = textFieldValue.text)) },
                            enabled = textFieldValue.text.isNotBlank()
                        ) {
                            Text(
                                text = "Save",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                FormattingBar(
                    onBoldClick = { textFieldValue = applyMarkdown(textFieldValue, "**") },
                    onItalicClick = { textFieldValue = applyMarkdown(textFieldValue, "*") },
                    onUnderscoreClick = { textFieldValue = applyMarkdown(textFieldValue, "<u>", "</u>") },
                    onHeadingClick = { textFieldValue = applyHeading(textFieldValue) }
                )
            }
        ) { paddingValues ->
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
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
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
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
                            onClick = { onAddIdea(textFieldValue.text) },
                            enabled = textFieldValue.text.isNotBlank()
                        ) {
                            Text(
                                text = "Save",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                FormattingBar(
                    onBoldClick = { textFieldValue = applyMarkdown(textFieldValue, "**") },
                    onItalicClick = { textFieldValue = applyMarkdown(textFieldValue, "*") },
                    onUnderscoreClick = { textFieldValue = applyMarkdown(textFieldValue, "<u>", "</u>") },
                    onHeadingClick = { textFieldValue = applyHeading(textFieldValue) }
                )
            }
        ) { paddingValues ->
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
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
