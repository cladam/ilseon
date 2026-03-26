package com.ilseon.wear.tile

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri

/**
 * Shared helper for loading the current priority task from the Wearable DataClient.
 * Used by both the Tile service and the main Activity.
 */
object WearTaskDataLoader {

    private const val TAG = "WearTaskDataLoader"

    /**
     * Loads the current priority task from the DataClient.
     * Returns null if no task is synced yet (empty state).
     */
    suspend fun loadTaskData(context: Context): WearTaskData? {
        return try {
            val dataClient = Wearable.getDataClient(context)
            val dataItems = dataClient.getDataItems(
                "wear://*${WearTaskData.PATH_PRIORITY_TASK}".toUri()
            ).await()

            val item = dataItems.firstOrNull()
            if (item != null) {
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                val descriptionRaw = dataMap.getString(WearTaskData.KEY_DESCRIPTION, "")
                val task = WearTaskData(
                    title = dataMap.getString(WearTaskData.KEY_TITLE, ""),
                    description = descriptionRaw.takeIf { it.isNotEmpty() },
                    isUrgent = dataMap.getBoolean(WearTaskData.KEY_IS_URGENT, false),
                    dueTime = if (dataMap.containsKey(WearTaskData.KEY_DUE_TIME))
                        dataMap.getLong(WearTaskData.KEY_DUE_TIME) else null
                )
                dataItems.release()
                task.takeIf { it.title.isNotEmpty() }
            } else {
                dataItems.release()
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "No task data from phone yet (expected on first run)", e)
            null
        }
    }

    /**
     * Sends an action message to all connected phone nodes.
     */
    suspend fun sendAction(context: Context, actionPath: String) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            Log.d(TAG, "Connected nodes: ${nodes.size} — ${nodes.map { "${it.displayName}(${it.id})" }}")
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected nodes found — message won't be delivered")
            }
            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, actionPath, byteArrayOf()).await()
                Log.d(TAG, "Message sent to ${node.displayName}: $actionPath")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send action to phone", e)
        }
    }
}

