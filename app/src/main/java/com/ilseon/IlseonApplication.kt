package com.ilseon

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ilseon.notifications.NotificationHelper
import com.ilseon.service.HapticWorker
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class IlseonApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY)
            .build())
        notificationHelper.createNotificationChannels()
        setupHapticWorker()
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
