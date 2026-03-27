package com.ilseon.wear.tile

import android.util.Log
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/**
 * Listens for DataClient changes from the phone and triggers a tile refresh
 * so the Priority Task tile always shows up-to-date data.
 */
class TaskDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "TaskDataListener"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "Data changed, ${dataEvents.count} event(s)")

        var shouldRefresh = false
        for (event in dataEvents) {
            val path = event.dataItem.uri.path
            if (path == WearTaskData.PATH_PRIORITY_TASK || path == WearTaskData.PATH_RECORDING_STATE) {
                shouldRefresh = true
                break
            }
        }

        if (shouldRefresh) {
            Log.d(TAG, "Priority task changed, requesting tile update")
            TileService.getUpdater(this)
                .requestUpdate(PriorityTaskTileService::class.java)
        }
    }
}

