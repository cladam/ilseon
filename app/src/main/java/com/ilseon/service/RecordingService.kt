package com.ilseon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.ilseon.MainActivity
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.data.voicememo.VoiceMemoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var voiceMemoRepository: VoiceMemoRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var hapticManager: HapticManager

    private var mediaRecorder: MediaRecorder? = null
    private var activeOutputFile: File? = null
    private var startTime: Long = 0
    private var totalPausedMillis: Long = 0
    private var pauseStartTime: Long = 0

    private var mediaSession: MediaSession? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = LocalBinder()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d("RecordingService", "AudioFocus change: $focusChange")
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        
        // Initialize MediaSession synchronously to ensure it's ready for onStartCommand
        setupMediaSession()

        serviceScope.launch {
            settingsRepository.mediaButtonTriggerEnabled.collectLatest { enabled ->
                if (enabled) {
                    mediaSession?.isActive = true
                    tryToGainFocus()
                    // Stay alive in foreground to keep MediaSession priority
                    if (mediaRecorder == null) {
                        startForeground(NOTIFICATION_ID, createNotification("Hardware trigger active"))
                    }
                    Log.d("RecordingService", "MediaSession Active")
                } else {
                    mediaSession?.isActive = false
                    abandonFocus()
                    Log.d("RecordingService", "MediaSession Inactive")
                    if (mediaRecorder == null) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                }
            }
        }
    }

    private fun tryToGainFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun setupMediaSession() {
        // Use the platform MediaSession API directly (not MediaSessionCompat).
        // MediaSessionCompat's auto-discovery creates broadcast PendingIntents that
        // the system caches as "last known PendingIntent". These go stale on process
        // death → CanceledException. The platform API gives us full control.
        mediaSession = MediaSession(this, "IlseonRecordingSession").apply {
            // Explicitly clear the media button receiver — no broadcast PI will be cached.
            @Suppress("DEPRECATION")
            setMediaButtonReceiver(null)

            @Suppress("DEPRECATION")
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    handleMediaButtonClick()
                }
                override fun onPause() {
                    handleMediaButtonClick()
                }
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                handleMediaButtonClick()
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })

            val stateBuilder = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE
                )
                .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
            setPlaybackState(stateBuilder.build())

            // Activate immediately so the session claims media button events right away.
            // The collectLatest flow below will deactivate if the setting is disabled.
            isActive = true
        }
    }


    private fun handleMediaButtonClick() {
        if (mediaRecorder == null) {
            hapticManager.performNudge()
            startRecording()
        } else {
            val result = stopRecording()
            hapticManager.performSuccess()
            if (result != null) {
                processRecording(result)
            }
        }
    }

    private fun processRecording(result: RecordingResult) {
        serviceScope.launch(Dispatchers.IO) {
            val voiceMemo = VoiceMemo(
                title = "Media Button Capture",
                filePath = result.filePath,
                durationSeconds = result.durationSeconds
            )
            voiceMemoRepository.insert(voiceMemo)
        }
    }

    fun startRecording() {
        if (mediaRecorder != null) return

        val outputFile = createOutputFile() ?: return
        activeOutputFile = outputFile

        mediaRecorder = createMediaRecorder(outputFile).apply {
            try {
                prepare()
                start()
                startTime = System.currentTimeMillis()
                totalPausedMillis = 0
                
                updatePlaybackState(true)

                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, createNotification("Capturing thought..."), type)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification("Capturing thought..."))
                }
            } catch (e: IOException) {
                cancelRecording()
            }
        }
    }

    private fun updatePlaybackState(isRecording: Boolean) {
        val state = if (isRecording) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    fun pauseRecording() {
        mediaRecorder?.pause()
        pauseStartTime = System.currentTimeMillis()
        updatePlaybackState(false)
    }

    fun resumeRecording() {
        totalPausedMillis += (System.currentTimeMillis() - pauseStartTime)
        mediaRecorder?.resume()
        updatePlaybackState(true)
    }

    fun stopRecording(): RecordingResult? {
        val duration = ((System.currentTimeMillis() - startTime - totalPausedMillis) / 1000).toInt()
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            release()
        }
        mediaRecorder = null
        updatePlaybackState(false)
        
        serviceScope.launch {
            if (settingsRepository.mediaButtonTriggerEnabled.first()) {
                startForeground(NOTIFICATION_ID, createNotification("Hardware trigger active"))
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }

        val resultFile = activeOutputFile ?: return null
        activeOutputFile = null
        return RecordingResult(
            filePath = resultFile.absolutePath,
            durationSeconds = duration
        )
    }

    fun cancelRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            release()
        }
        mediaRecorder = null
        updatePlaybackState(false)
        activeOutputFile?.delete()
        activeOutputFile = null
    }

    private fun createOutputFile(): File? {
        val outputDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
        } else {
            val dir = File(getExternalFilesDir(null), "Recordings")
            if (!dir.exists()) dir.mkdirs()
            dir
        }

        if (outputDir == null) return null
        return File(outputDir, "voice_memo_${UUID.randomUUID()}.m4a")
    }

    private fun createMediaRecorder(file: File): MediaRecorder {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Ilseon Invisible Capture")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .apply {
                mediaSession?.sessionToken?.let { token ->
                    setStyle(
                        androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(
                                android.support.v4.media.session.MediaSessionCompat.Token.fromToken(token)
                            )
                    )
                }
            }
            .build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        abandonFocus()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL_ID, "Recording Service", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            // Ensure foreground state when started via startForegroundService()
            if (mediaRecorder == null) {
                startForeground(NOTIFICATION_ID, createNotification("Hardware trigger active"))
            }

            // Extract the KeyEvent directly and dispatch to the session callback,
            // or handle it ourselves as a fallback
            val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> handleMediaButtonClick()
                }
            }
        }
        return START_STICKY
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "RecordingServiceChannel"
    }
}
