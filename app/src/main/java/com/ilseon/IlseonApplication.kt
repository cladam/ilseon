package com.ilseon

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ilseon.data.task.SettingsRepository
import com.ilseon.notifications.NotificationHelper
import com.ilseon.service.HapticWorker
import com.ilseon.service.RecordingService
import com.ilseon.wear.WearActionListenerService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class IlseonApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannels()
        setupHapticWorker()
        startRecordingServiceIfNeeded()
        registerWearMessageListener()
    }

    private fun startRecordingServiceIfNeeded() {
        CoroutineScope(Dispatchers.Main).launch {
            val enabled = settingsRepository.mediaButtonTriggerEnabled.first()
            if (enabled) {
                val intent = Intent(this@IlseonApplication, RecordingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }

    private fun setupHapticWorker() {
        val workRequest = PeriodicWorkRequestBuilder<HapticWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "haptic_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Registers a programmatic MessageClient listener as a backup.
     * The manifest-declared WearableListenerService can sometimes fail to be
     * discovered by GMS. This listener works while the app process is alive.
     */
    private fun registerWearMessageListener() {
        try {
            val listener = WearActionListenerService.createMessageListener(this)
            com.google.android.gms.wearable.Wearable.getMessageClient(this)
                .addListener(listener)
            Log.d("IlseonApp", "Registered programmatic Wearable message listener")
        } catch (e: Exception) {
            Log.d("IlseonApp", "Wearable API not available, skipping message listener", e)
        }
    }
}
