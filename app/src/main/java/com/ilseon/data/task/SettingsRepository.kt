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

    val naggingNotificationsEnabled: Flow<Boolean>
    suspend fun setNaggingNotificationsEnabled(enabled: Boolean)

    val bluetoothSstEnabled: Flow<Boolean>
    suspend fun setBluetoothSstEnabled(enabled: Boolean)

    val mediaButtonTriggerEnabled: Flow<Boolean>
    suspend fun setMediaButtonTriggerEnabled(enabled: Boolean)

    val sstLanguage: Flow<String>
    suspend fun setSstLanguage(language: String)

    val apiKey: Flow<String>
    suspend fun setApiKey(apiKey: String)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_NUDGE_NOTIFICATIONS = "nudge_notifications_enabled"
        const val KEY_NAGGING_NOTIFICATIONS = "nagging_notifications_enabled"
        const val KEY_BLUETOOTH_SST_ENABLED = "bluetooth_sst_enabled"
        const val KEY_MEDIA_BUTTON_TRIGGER = "media_button_trigger_enabled"
        const val KEY_SST_LANGUAGE = "sst_language"
        const val KEY_API_KEY = "gemini_api_key"
    }

    override val nudgeNotificationsEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NUDGE_NOTIFICATIONS) {
                trySend(prefs.getBoolean(KEY_NUDGE_NOTIFICATIONS, true))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_NUDGE_NOTIFICATIONS, true))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setNudgeNotificationsEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_NUDGE_NOTIFICATIONS, enabled)
        }
    }

    override val naggingNotificationsEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NAGGING_NOTIFICATIONS) {
                trySend(prefs.getBoolean(KEY_NAGGING_NOTIFICATIONS, false))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_NAGGING_NOTIFICATIONS, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setNaggingNotificationsEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_NAGGING_NOTIFICATIONS, enabled)
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

    override val mediaButtonTriggerEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MEDIA_BUTTON_TRIGGER) {
                trySend(prefs.getBoolean(KEY_MEDIA_BUTTON_TRIGGER, false))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_MEDIA_BUTTON_TRIGGER, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setMediaButtonTriggerEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_MEDIA_BUTTON_TRIGGER, enabled)
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

    override val apiKey: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_API_KEY) {
                trySend(prefs.getString(KEY_API_KEY, "") ?: "")
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString(KEY_API_KEY, "") ?: "")
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setApiKey(apiKey: String) {
        prefs.edit {
            putString(KEY_API_KEY, apiKey)
        }
    }
}
