package com.ilseon.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.ui.theme.MutedTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun VoiceMemoCard(
    memo: VoiceMemo,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: (VoiceMemo) -> Unit,
    onSeek: (Float) -> Unit,
    onConvertToTask: (VoiceMemo) -> Unit,
    onDelete: (VoiceMemo) -> Unit,
    onEditTitle: (VoiceMemo) -> Unit,
    onShowTranscription: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = memo.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(memo.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = memo.transcription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration Badge
                Text(
                    text = formatDuration(memo.durationSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Action Icons
                Row {
                    IconButton(onClick = { onPlayPause(memo) }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop Playback" else "Play Voice Memo"
                        )
                    }
                    IconButton(onClick = { onShowTranscription(memo.transcription) }) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = "Show Transcription"
                        )
                    }
                    IconButton(onClick = { onConvertToTask(memo) }) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Convert to Task",
                            tint = MutedTeal
                        )
                    }
                    IconButton(onClick = { onEditTitle(memo) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title"
                        )
                    }
                    IconButton(onClick = { onDelete(memo) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Voice Memo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
    val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
