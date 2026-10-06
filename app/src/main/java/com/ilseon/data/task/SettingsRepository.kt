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

    val incidentFollowUpTitle: Flow<String>
    suspend fun setIncidentFollowUpTitle(title: String)

    val incidentFollowUpContextId: Flow<String?>
    suspend fun setIncidentFollowUpContextId(contextId: String?)

    val incidentFollowUpDelayMinutes: Flow<Int>
    suspend fun setIncidentFollowUpDelayMinutes(minutes: Int)
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
        const val KEY_INCIDENT_FOLLOW_UP_TITLE = "incident_follow_up_title"
        const val KEY_INCIDENT_FOLLOW_UP_CONTEXT_ID = "incident_follow_up_context_id"
        const val KEY_INCIDENT_FOLLOW_UP_DELAY_MINUTES = "incident_follow_up_delay_minutes"

        const val DEFAULT_INCIDENT_FOLLOW_UP_TITLE =
            "There was an incident that you have to follow-up, please pause, reflect and engage."
        const val DEFAULT_INCIDENT_FOLLOW_UP_DELAY_MINUTES = 45
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

    override val incidentFollowUpTitle: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_INCIDENT_FOLLOW_UP_TITLE) {
                trySend(prefs.getString(KEY_INCIDENT_FOLLOW_UP_TITLE, DEFAULT_INCIDENT_FOLLOW_UP_TITLE) ?: DEFAULT_INCIDENT_FOLLOW_UP_TITLE)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString(KEY_INCIDENT_FOLLOW_UP_TITLE, DEFAULT_INCIDENT_FOLLOW_UP_TITLE) ?: DEFAULT_INCIDENT_FOLLOW_UP_TITLE)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setIncidentFollowUpTitle(title: String) {
        prefs.edit {
            putString(KEY_INCIDENT_FOLLOW_UP_TITLE, title)
        }
    }

    override val incidentFollowUpContextId: Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_INCIDENT_FOLLOW_UP_CONTEXT_ID) {
                trySend(prefs.getString(KEY_INCIDENT_FOLLOW_UP_CONTEXT_ID, null))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString(KEY_INCIDENT_FOLLOW_UP_CONTEXT_ID, null))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setIncidentFollowUpContextId(contextId: String?) {
        prefs.edit {
            if (contextId != null) {
                putString(KEY_INCIDENT_FOLLOW_UP_CONTEXT_ID, contextId)
            } else {
                remove(KEY_INCIDENT_FOLLOW_UP_CONTEXT_ID)
            }
        }
    }

    override val incidentFollowUpDelayMinutes: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_INCIDENT_FOLLOW_UP_DELAY_MINUTES) {
                trySend(prefs.getInt(KEY_INCIDENT_FOLLOW_UP_DELAY_MINUTES, DEFAULT_INCIDENT_FOLLOW_UP_DELAY_MINUTES))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getInt(KEY_INCIDENT_FOLLOW_UP_DELAY_MINUTES, DEFAULT_INCIDENT_FOLLOW_UP_DELAY_MINUTES))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setIncidentFollowUpDelayMinutes(minutes: Int) {
        prefs.edit {
            putInt(KEY_INCIDENT_FOLLOW_UP_DELAY_MINUTES, minutes)
        }
    }
}
