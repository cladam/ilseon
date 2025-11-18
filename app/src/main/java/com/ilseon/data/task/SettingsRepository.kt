package com.ilseon.data.task

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val nudgeNotificationsEnabled: Flow<Boolean>
    suspend fun setNudgeNotificationsEnabled(enabled: Boolean)

    val bluetoothSstEnabled: Flow<Boolean>
    suspend fun setBluetoothSstEnabled(enabled: Boolean)

    val sstLanguage: Flow<String>
    suspend fun setSstLanguage(language: String)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_NUDGE_NOTIFICATIONS = "nudge_notifications_enabled"
        const val KEY_BLUETOOTH_SST_ENABLED = "bluetooth_sst_enabled"
        const val KEY_SST_LANGUAGE = "sst_language"
    }

    override val nudgeNotificationsEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NUDGE_NOTIFICATIONS) {
                trySend(prefs.getBoolean(KEY_NUDGE_NOTIFICATIONS, true))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // Send initial value
        trySend(prefs.getBoolean(KEY_NUDGE_NOTIFICATIONS, true))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setNudgeNotificationsEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_NUDGE_NOTIFICATIONS, enabled)
        }
    }

    override val bluetoothSstEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_BLUETOOTH_SST_ENABLED) {
                trySend(prefs.getBoolean(KEY_BLUETOOTH_SST_ENABLED, false))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_BLUETOOTH_SST_ENABLED, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setBluetoothSstEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_BLUETOOTH_SST_ENABLED, enabled)
        }
    }

    override val sstLanguage: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SST_LANGUAGE) {
                trySend(prefs.getString(KEY_SST_LANGUAGE, "en-GB") ?: "en-GB")
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString(KEY_SST_LANGUAGE, "en-GB") ?: "en-GB")
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setSstLanguage(language: String) {
        prefs.edit {
            putString(KEY_SST_LANGUAGE, language)
        }
    }
}
