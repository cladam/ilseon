package com.ilseon.wear.tile

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri

/**
 * Shared helper for loading data from the Wearable DataClient
 * and sending messages to the phone.
 */
object WearTaskDataLoader {

    private const val TAG = "WearTaskDataLoader"

    /** Loads the current priority task from the DataClient. */
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

    /** Loads the current recording state from the DataClient. */
    suspend fun loadRecordingState(context: Context): Boolean {
        return try {
            val dataClient = Wearable.getDataClient(context)
            val dataItems = dataClient.getDataItems(
                "wear://*${WearTaskData.PATH_RECORDING_STATE}".toUri()
            ).await()

            val item = dataItems.firstOrNull()
            val isRecording = if (item != null) {
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                dataMap.getBoolean(WearTaskData.KEY_IS_RECORDING, false)
            } else {
                false
            }
            dataItems.release()
            isRecording
        } catch (e: Exception) {
            Log.d(TAG, "Could not read recording state", e)
            false
        }
    }

    /** Sends a toggle-recording message to the phone. */
    suspend fun toggleRecording(context: Context) {
        sendAction(context, WearTaskData.ACTION_TOGGLE_RECORDING)
    }

    /** Sends an action message to all connected phone nodes. */
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
