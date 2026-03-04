package com.ilseon

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ilseon.data.task.SettingsRepository
import com.ilseon.notifications.NotificationHelper
import com.ilseon.service.HapticWorker
import com.ilseon.service.RecordingService
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
}
