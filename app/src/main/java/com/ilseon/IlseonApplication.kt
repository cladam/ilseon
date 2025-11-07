package com.ilseon

import android.app.Application

class IlseonApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.Companion.getDatabase(this) }
}