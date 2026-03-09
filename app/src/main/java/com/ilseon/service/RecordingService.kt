package com.ilseon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaRecorder
import android.media.browse.MediaBrowser
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.media.MediaBrowserService
import android.util.Log
import android.view.KeyEvent
import com.ilseon.MainActivity
import com.ilseon.data.bluetooth.BluetoothChecker
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.data.voicememo.VoiceMemoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : MediaBrowserService() {

    @Inject
    lateinit var voiceMemoRepository: VoiceMemoRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var hapticManager: HapticManager

    @Inject
    lateinit var bluetoothChecker: BluetoothChecker

    private var mediaRecorder: MediaRecorder? = null
    private var activeOutputFile: File? = null
    private var startTime: Long = 0
    private var totalPausedMillis: Long = 0
    private var pauseStartTime: Long = 0

    private var mediaSession: MediaSession? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var isScoStarted = false
    private var lastMediaButtonClickTime = 0L
    private val MEDIA_BUTTON_DEBOUNCE_MS = 1500L

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = LocalBinder()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d("RecordingService", "AudioFocus change: $focusChange")
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Another app (e.g. Spotify) took audio focus — that's fine, let them play.
            // But re-assert our MediaSession to stay on top for media button routing.
            // We do NOT reclaim audio focus here.
            serviceScope.launch {
                delay(2000) // Wait for the other app to fully settle
                if (settingsRepository.mediaButtonTriggerEnabled.first() && mediaRecorder == null) {
                    reassertPriority()
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent): IBinder? {
        if (SERVICE_INTERFACE == intent.action) {
            return super.onBind(intent)
        }
        return binder
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // Return a dummy root so the system sees us as a valid media player
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        // No media content to browse
        result.sendResult(mutableListOf())
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        
        setupMediaSession()

        serviceScope.launch {
            settingsRepository.mediaButtonTriggerEnabled.collectLatest { enabled ->
                if (enabled) {
                    reassertPriority()
                } else {
                    mediaSession?.isActive = false
                    abandonFocus()
                    if (mediaRecorder == null) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    }
                }
            }
        }
    }

    private fun reassertPriority() {
        val session = mediaSession ?: return
        serviceScope.launch {
            Log.d("RecordingService", "Cycling MediaSession priority")
            session.isActive = false
            delay(150)
            session.isActive = true

            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Ilseon Invisible Capture")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Hardware Trigger Active")
                .build()
            session.setMetadata(metadata)

            // Use STATE_PAUSED when idle so the system doesn't think we're
            // actively playing — STATE_PLAYING triggers auto-resume / event re-dispatch.
            val isRecording = mediaRecorder != null
            val state = if (isRecording) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val speed = if (isRecording) 1f else 0f
            val stateBuilder = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or 
                    PlaybackState.ACTION_PAUSE or 
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_STOP
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, speed)
            session.setPlaybackState(stateBuilder.build())
            
            if (mediaRecorder == null) {
                startForegroundWithType(createNotification("Hardware trigger active"))
            }
        }
    }

    private fun requestRecordingFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
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
        mediaSession = MediaSession(this, "IlseonRecordingSession").apply {
            val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setClass(this@RecordingService, RecordingService::class.java)
            }
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(this@RecordingService, 0, mediaButtonIntent, piFlags)
            } else {
                @Suppress("DEPRECATION")
                PendingIntent.getService(this@RecordingService, 0, mediaButtonIntent, piFlags)
            }
            @Suppress("DEPRECATION")
            setMediaButtonReceiver(pi)

            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setPlaybackToLocal(playbackAttributes)
            }

            @Suppress("DEPRECATION")
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { handleMediaButtonClick() }
                override fun onPause() { handleMediaButtonClick() }
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_HEADSETHOOK -> {
                                handleMediaButtonClick()
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })
            isActive = true
        }
        // Set the MediaBrowserService session token so the system binds media controls to our session
        mediaSession?.sessionToken?.let { setSessionToken(it) }
    }

    private fun handleMediaButtonClick() {
        val now = System.currentTimeMillis()
        if (now - lastMediaButtonClickTime < MEDIA_BUTTON_DEBOUNCE_MS) {
            Log.d("RecordingService", "handleMediaButtonClick: debounced (${now - lastMediaButtonClickTime}ms since last)")
            return
        }
        lastMediaButtonClickTime = now

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

        // Request audio focus only when actually recording
        requestRecordingFocus()

        // Start Bluetooth SCO routing if a headset is connected.
        // We don't wait for SCO to fully connect — we start recording immediately.
        // The system will route audio through the BT mic as soon as SCO is ready.
        // At worst, the very first ~200-500ms may come from the phone mic.
        startBluetoothRouting()

        mediaRecorder = createMediaRecorder(outputFile).apply {
            try {
                prepare()
                start()
                startTime = System.currentTimeMillis()
                totalPausedMillis = 0
                Log.d("RecordingService", "startRecording: recording started to ${outputFile.absolutePath}")
                updatePlaybackState(true)
                startForegroundWithType(createNotification("Capturing thought..."))
            } catch (e: Exception) {
                Log.e("RecordingService", "startRecording: failed", e)
                cancelRecording()
            }
        }
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
            try { stop() } catch (e: Exception) { Log.e("RecordingService", "Stop failed", e) }
            release()
        }
        mediaRecorder = null
        stopBluetoothRouting()
        abandonFocus() // Release audio focus so other apps can play
        updatePlaybackState(false)
        
        serviceScope.launch {
            if (settingsRepository.mediaButtonTriggerEnabled.first()) {
                startForegroundWithType(createNotification("Hardware trigger active"))
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }

        val resultFile = activeOutputFile ?: return null
        activeOutputFile = null
        return RecordingResult(filePath = resultFile.absolutePath, durationSeconds = duration)
    }

    fun cancelRecording() {
        mediaRecorder?.apply {
            try { stop() } catch (e: Exception) { Log.e("RecordingService", "Stop failed", e) }
            release()
        }
        mediaRecorder = null
        stopBluetoothRouting()
        abandonFocus() // Release audio focus so other apps can play
        updatePlaybackState(false)
        activeOutputFile?.delete()
        activeOutputFile = null
    }

    private fun updatePlaybackState(isRecording: Boolean) {
        val state = if (isRecording) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val speed = if (isRecording) 1f else 0f
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, speed)
                .build()
        )
    }

    private fun createOutputFile(): File? {
        val outputDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
        } else {
            val dir = File(getExternalFilesDir(null), "Recordings")
            if (!dir.exists()) dir.mkdirs()
            dir
        }
        return if (outputDir == null) null else File(outputDir, "voice_memo_${UUID.randomUUID()}.m4a")
    }

    private fun createMediaRecorder(file: File): MediaRecorder {
        val hasHeadset = bluetoothChecker.isHeadsetConnected()
        // Always use MIC source. VOICE_COMMUNICATION requires an active SCO link
        // (which needs MODE_IN_COMMUNICATION and breaks all audio output).
        // On API 28+, setPreferredDevice() routes MIC input to the BT device directly.
        val audioSource = if (hasHeadset && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        else
            MediaRecorder.AudioSource.MIC

        Log.d("RecordingService", "createMediaRecorder: hasHeadset=$hasHeadset, audioSource=$audioSource, isScoStarted=$isScoStarted")
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(audioSource)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)

            // On API 28+, route the mic input to the Bluetooth device.
            // SCO is started in startBluetoothRouting() to open the mic channel;
            // setPreferredDevice() then steers recording to that device.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasHeadset) {
                getBluetoothAudioDevice()?.let { device ->
                    Log.d("RecordingService", "Setting preferred device: ${device.productName} (type=${device.type})")
                    setPreferredDevice(device)
                } ?: Log.w("RecordingService", "BT headset connected but no SCO/BLE input device found")
            }
        }
    }

    /**
     * Open the Bluetooth SCO mic channel so audio can flow from the headset.
     *
     * On API < 28:  MODE_IN_COMMUNICATION + SCO (the only way).
     * On API >= 28:  SCO only (no mode change).  setPreferredDevice() on the
     *                MediaRecorder steers the audio to the BT mic once SCO is up.
     *                NOT changing mode avoids killing Spotify / other audio output.
     */
    private fun startBluetoothRouting() {
        if (!bluetoothChecker.isHeadsetConnected()) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            Log.d("RecordingService", "Starting Bluetooth SCO (legacy API, with MODE_IN_COMMUNICATION)")
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } else {
            Log.d("RecordingService", "Starting Bluetooth SCO (modern API, no mode change)")
        }

        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = true
        isScoStarted = true
    }

    private fun stopBluetoothRouting() {
        if (isScoStarted) {
            Log.d("RecordingService", "Stopping Bluetooth SCO")
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                audioManager.mode = AudioManager.MODE_NORMAL
            }
            isScoStarted = false
        }
    }

    private fun getBluetoothAudioDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        Log.d("RecordingService", "Available input devices: ${devices.map { "${it.productName}(type=${it.type})" }}")
        val btDevice = devices.find {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
        Log.d("RecordingService", "Bluetooth audio device: ${btDevice?.productName ?: "none"}")
        return btDevice
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Ilseon Invisible Capture")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .apply {
                    mediaSession?.sessionToken?.let { token ->
                        val style = Notification.MediaStyle()
                            .setMediaSession(token)
                        setStyle(style)
                    }
                }
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Ilseon Invisible Capture")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        stopBluetoothRouting()
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
        if (intent == null || intent.action == null || intent.action == "REFRESH_PRIORITY") {
            serviceScope.launch {
                if (settingsRepository.mediaButtonTriggerEnabled.first()) {
                    reassertPriority()
                }
            }
        }

        try {
            startForegroundWithType(createNotification("Hardware trigger active"))
        } catch (e: Exception) { Log.e("RecordingService", "Start foreground failed", e) }

        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("media_button_trigger_enabled", false)) {
                return START_STICKY
            }

            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_HEADSETHOOK -> handleMediaButtonClick()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "RecordingServiceChannel"
    }
}
