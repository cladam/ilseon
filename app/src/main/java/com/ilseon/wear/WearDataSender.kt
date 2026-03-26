package com.ilseon.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ilseon.data.task.Task
import kotlinx.coroutines.tasks.await

/**
 * Sends the current priority task to the Wear OS watch via DataClient.
 * Called from TaskRepository whenever the priority task changes.
 */
object WearDataSender {

    private const val TAG = "WearDataSender"

    // Must match the constants in wear/.../WearTaskData.kt
    private const val PATH_PRIORITY_TASK = "/priority-task"
    private const val KEY_TITLE = "title"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_IS_URGENT = "is_urgent"
    private const val KEY_DUE_TIME = "due_time"

    /**
     * Returns true if the Wearable API is usable on this device.
     * False on phones without a paired watch or without the Wear OS companion app.
     */
    private suspend fun isWearableAvailable(context: Context): Boolean {
        return try {
            Wearable.getNodeClient(context).connectedNodes.await()
            true
        } catch (e: ApiException) {
            // statusCode 17 = API_UNAVAILABLE (no Wear OS companion app)
            Log.d(TAG, "Wearable API not available (code=${e.statusCode})")
            false
        } catch (e: Exception) {
            Log.d(TAG, "Wearable API check failed", e)
            false
        }
    }

    /**
     * Pushes the current priority task to the Wearable DataClient.
     * Pass null to clear the task (shows empty state on watch).
     * No-ops silently if Wearable API is unavailable.
     */
    suspend fun sendPriorityTask(context: Context, task: Task?) {
        if (!isWearableAvailable(context)) return

        try {
            val dataClient = Wearable.getDataClient(context)

            if (task == null) {
                // Clear data item so the watch shows the empty state
                dataClient.deleteDataItems(
                    android.net.Uri.parse("wear://*$PATH_PRIORITY_TASK")
                ).await()
                Log.d(TAG, "Cleared priority task on watch")
                return
            }

            val putDataReq = PutDataMapRequest.create(PATH_PRIORITY_TASK).apply {
                dataMap.putString(KEY_TITLE, task.title)
                dataMap.putString(KEY_DESCRIPTION, task.description ?: "")
                dataMap.putBoolean(KEY_IS_URGENT, task.isUrgent)
                if (task.dueTime != null) {
                    dataMap.putLong(KEY_DUE_TIME, task.dueTime)
                }
                // Force a data change even if values are the same, so the watch refreshes
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest()
                .setUrgent() // Deliver immediately, don't batch

            dataClient.putDataItem(putDataReq).await()
            Log.d(TAG, "Sent priority task to watch: ${task.title}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send task to watch", e)
        }
    }
}
