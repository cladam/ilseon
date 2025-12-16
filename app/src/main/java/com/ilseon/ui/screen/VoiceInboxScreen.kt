package com.ilseon.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.VoiceMemoViewModel
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.ui.components.VoiceMemoCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceInboxScreen(
    viewModel: VoiceMemoViewModel = hiltViewModel(),
    onNavigateToNewTask: (String, String) -> Unit,
    initialMemoIdToPlay: String? = null,
) {
    val voiceMemos by viewModel.voiceMemos.collectAsState()
    val currentlyPlayingId by viewModel.currentlyPlayingId.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    var memoToEdit by remember { mutableStateOf<VoiceMemo?>(null) }

    LaunchedEffect(initialMemoIdToPlay) {
        initialMemoIdToPlay?.let {
            viewModel.onPlayPause(it)
        }
    }

    if (memoToEdit != null) {
        EditTitleDialog(
            memo = memoToEdit!!,
            onDismiss = { memoToEdit = null },
            onSave = { updatedMemo, newTitle ->
                viewModel.updateVoiceMemoTitle(updatedMemo, newTitle)
                memoToEdit = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Screen Title
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
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
        }

        // Voice Memo List
        if (voiceMemos.isEmpty()) {
            item {
                Text(
                    text = "No voice memos yet. Tap the mic to record your thoughts.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(voiceMemos, key = { it.id }) { memo ->
                val isPlaying = memo.id == currentlyPlayingId
                VoiceMemoCard(
                    memo = memo,
                    isPlaying = isPlaying,
                    progress = if (isPlaying) progress else 0f,
                    onPlayPause = { viewModel.onPlayPause(it) },
                    onSeek = { newProgress -> viewModel.seekTo(newProgress) },
                    onConvertToTask = {
                        val description = "[Play Recording](ilseon://play-voice-memo/${it.id})"
                        onNavigateToNewTask(it.title, description)
                    },
                    onDelete = { viewModel.deleteVoiceMemo(it) },
                    onEditTitle = { memoToEdit = it },
                    onTranscribe = { viewModel.transcribeMemo(it) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun EditTitleDialog(
    memo: VoiceMemo,
    onDismiss: () -> Unit,
    onSave: (VoiceMemo, String) -> Unit
) {
    var title by remember(memo) { mutableStateOf(memo.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Title") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Voice Memo Title") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSave(memo, title) }) {
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
