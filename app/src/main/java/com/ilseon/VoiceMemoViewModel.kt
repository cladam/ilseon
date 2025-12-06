package com.ilseon

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.data.voicememo.VoiceMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class VoiceMemoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceMemoRepository: VoiceMemoRepository
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackerJob: Job? = null

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId = _currentlyPlayingId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()

    val voiceMemos = voiceMemoRepository.getVoiceMemos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onPlayPause(memoId: String) {
        viewModelScope.launch {
            val memo = voiceMemoRepository.getVoiceMemo(memoId)
            if (memo != null) {
                if (_currentlyPlayingId.value == memo.id) {
                    stopPlayback()
                } else {
                    startPlayback(memo)
                }
            }
        }
    }

    fun onPlayPause(memo: VoiceMemo) {
        if (_currentlyPlayingId.value == memo.id) {
            stopPlayback()
        } else {
            startPlayback(memo)
        }
    }

    private fun startPlayback(memo: VoiceMemo) {
        stopPlayback() // Stop any previous playback
        mediaPlayer = MediaPlayer().apply {
            try {
                val uri = if (memo.filePath.startsWith("content://")) {
                    memo.filePath.toUri()
                } else {
                    Uri.fromFile(File(memo.filePath))
                }
                setDataSource(context, uri)
                prepareAsync()
                setOnPreparedListener { player ->
                    player.start()
                    _currentlyPlayingId.value = memo.id
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stopPlayback()
                }
            } catch (e: IOException) {
                stopPlayback()
            }
        }
    }

    private fun stopPlayback() {
        progressTrackerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentlyPlayingId.value = null
        _playbackProgress.value = 0f
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let {
            val newPosition = (it.duration * progress).toInt()
            it.seekTo(newPosition)
            _playbackProgress.value = progress
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = viewModelScope.launch {
            while (_currentlyPlayingId.value != null) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackProgress.value = it.currentPosition.toFloat() / it.duration
                    }
                }
                delay(50) // Update progress roughly 20 times per second
            }
        }
    }

    fun saveVoiceMemo(filePath: String, durationSeconds: Int) {
        viewModelScope.launch {
            val title = "Voice Memo"

            val finalUri = saveRecordingToMediaStore(filePath, title)

            if (finalUri != null) {
                val voiceMemo = VoiceMemo(
                    title = title,
                    filePath = finalUri.toString(),
                    durationSeconds = durationSeconds
                )
                voiceMemoRepository.insert(voiceMemo)
            }
        }
    }

    private suspend fun saveRecordingToMediaStore(tempFilePath: String, title: String): Uri? = withContext(Dispatchers.IO) {
        val sanitizedTitle = title
            .replace(Regex("[^a-zA-Z0-9.-]+"), "_")
            .trim('_')
            .let { it.ifBlank { "voicememo" } }
            .take(100)
        val displayName = "${sanitizedTitle}_${System.currentTimeMillis()}.m4a"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recordingsFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Environment.DIRECTORY_RECORDINGS
                } else {
                    "Recordings"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, recordingsFolder + File.separator + "ilseon")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val tempFile = File(tempFilePath)
                resolver.openOutputStream(it).use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                }
                tempFile.delete() // Clean up temp file
                return@withContext it
            } catch (e: IOException) {
                resolver.delete(it, null, null)
                e.printStackTrace()
            }
        }
        return@withContext null
    }


    fun updateVoiceMemoTitle(memo: VoiceMemo, newTitle: String) {
        viewModelScope.launch {
            if (memo.filePath.startsWith("content://")) {
                val uri = memo.filePath.toUri()
                val currentDisplayName = getDisplayName(uri)
                val extension = currentDisplayName?.substringAfterLast(".", "") ?: "m4a"

                val sanitizedTitle = newTitle
                    .replace(Regex("[^a-zA-Z0-9.-]+"), "_")
                    .trim('_')
                    .let { it.ifBlank { "voicememo" } }
                    .take(100)
                val newDisplayName = "$sanitizedTitle.$extension"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
                }
                context.contentResolver.update(uri, contentValues, null, null)
            }
            val updatedMemo = memo.copy(title = newTitle)
            voiceMemoRepository.update(updatedMemo)
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                }
            }
        }
        return uri.lastPathSegment
    }


    fun deleteVoiceMemo(voiceMemo: VoiceMemo) {
        viewModelScope.launch {
            if (voiceMemo.id == _currentlyPlayingId.value) {
                stopPlayback()
            }
            withContext(Dispatchers.IO) {
                try {
                    if (voiceMemo.filePath.startsWith("content://")) {
                        val uri = voiceMemo.filePath.toUri()
                        context.contentResolver.delete(uri, null, null)
                    } else {
                        val file = File(voiceMemo.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            voiceMemoRepository.delete(voiceMemo.id)
        }
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}