package com.ilseon.data

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
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_NUDGE_NOTIFICATIONS = "nudge_notifications_enabled"
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
}
