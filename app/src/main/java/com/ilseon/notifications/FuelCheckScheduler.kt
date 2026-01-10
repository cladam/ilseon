package com.ilseon.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ilseon.MainActivity
import com.ilseon.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

interface FuelCheckScheduler {
    fun scheduleNextFuelCheck()
    fun cancel()
}

class WorkManagerFuelCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : FuelCheckScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun scheduleNextFuelCheck() {
        val randomHours = Random.nextLong(3, 9)

        val fuelCheckWorkRequest = PeriodicWorkRequestBuilder<FuelCheckWorker>(
            repeatInterval = randomHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(Random.nextLong(30, 90), TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "FuelCheck",
            ExistingPeriodicWorkPolicy.REPLACE, // Replace to update timing
            fuelCheckWorkRequest
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork("FuelCheck")
    }
}

class FuelCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "fuel_check_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fuel Check Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination", "fuel_check")
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setContentTitle("Fuel Check")
            .setContentText("How much energy do you have right now?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
