package com.ilseon.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ilseon.data.voicememo.VoiceMemo
import java.util.concurrent.TimeUnit

@Composable
fun VoiceMemoCard(
    memo: VoiceMemo,
    isPlaying: Boolean,
    isTranscribing: Boolean,
    progress: Float,
    onPlayPause: (VoiceMemo) -> Unit,
    onSeek: (Float) -> Unit,
    onConvertToTask: (VoiceMemo) -> Unit,
    onDelete: (VoiceMemo) -> Unit,
    onEditTitle: (VoiceMemo) -> Unit,
    onTranscribe: (VoiceMemo) -> Unit,
    showTranscribeOption: Boolean,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = memo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isTranscribing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Play/Pause and Progress Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayPauseButton(isPlaying = isPlaying, onClick = { onPlayPause(memo) })
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LinearProgressBarWithThumb(
                        progress = progress,
                        onSeek = { onSeek(it) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(if (isPlaying) (memo.durationSeconds * progress).toLong() else memo.durationSeconds.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Convert to Task") },
                    onClick = {
                        onConvertToTask(memo)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) }
                )
                if (showTranscribeOption) {
                    DropdownMenuItem(
                        text = { Text("Transcribe with Gemini") },
                        onClick = {
                            onTranscribe(memo)
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Edit title") },
                    onClick = {
                        onEditTitle(memo)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete(memo)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Icon(
        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
        contentDescription = if (isPlaying) "Pause" else "Play",
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        tint = MaterialTheme.colorScheme.secondary
    )
}

@Composable
fun LinearProgressBarWithThumb(
    progress: Float,
    onSeek: (Float) -> Unit
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    ).value

    Box(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds)
    val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
