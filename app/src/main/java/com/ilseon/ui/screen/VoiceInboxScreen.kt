package com.ilseon.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ilseon.ui.components.VoiceMemoCard
import com.ilseon.VoiceMemoViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceInboxScreen(
    viewModel: VoiceMemoViewModel = hiltViewModel(),
    onNavigateToNewTask: (String, String) -> Unit,
    onStartRecording: () -> Unit // This is now called from MainActivity's FAB
) {
    val voiceMemos by viewModel.voiceMemos.collectAsState()
    val currentlyPlayingId by viewModel.currentlyPlayingId.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()

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
                        viewModel.convertToTask(it)
                        onNavigateToNewTask(it.transcription, "Converted from voice memo.")
                    },
                    onDelete = { viewModel.deleteVoiceMemo(it) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}
