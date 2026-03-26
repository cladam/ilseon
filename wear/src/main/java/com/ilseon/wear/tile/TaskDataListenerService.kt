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

        var taskDataChanged = false
        for (event in dataEvents) {
            if (event.dataItem.uri.path == WearTaskData.PATH_PRIORITY_TASK) {
                taskDataChanged = true
                break
            }
        }

        if (taskDataChanged) {
            Log.d(TAG, "Priority task changed, requesting tile update")
            TileService.getUpdater(this)
                .requestUpdate(PriorityTaskTileService::class.java)
        }
    }
}

