package com.ilseon

import android.app.Application
import com.ilseon.service.NotificationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class IlseonApplication : Application() {

    @Inject
    lateinit var notificationService: NotificationService

    override fun onCreate() {
        super.onCreate()
        notificationService.createNotificationChannel()
    }
}
