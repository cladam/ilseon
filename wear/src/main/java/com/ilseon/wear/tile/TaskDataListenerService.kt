package com.ilseon.wear.tile

import android.content.ComponentName
import android.util.Log
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.ilseon.wear.complication.StartRecordingComplicationService

/**
 * Listens for DataClient changes from the phone and triggers a tile refresh
 * so the Priority Task tile always shows up-to-date data.
 * Also requests a complication update when the recording state changes.
 */
class TaskDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "TaskDataListener"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "Data changed, ${dataEvents.count} event(s)")

        var shouldRefreshTile = false
        var shouldRefreshComplication = false

        for (event in dataEvents) {
            val path = event.dataItem.uri.path
            if (path == WearTaskData.PATH_PRIORITY_TASK) {
                shouldRefreshTile = true
            }
            if (path == WearTaskData.PATH_RECORDING_STATE) {
                shouldRefreshTile = true
                shouldRefreshComplication = true
                // Real state arrived from phone — drop the optimistic override
                WearTaskDataLoader.clearOptimisticState()
            }
        }

        if (shouldRefreshTile) {
            Log.d(TAG, "Requesting tile update")
            TileService.getUpdater(this)
                .requestUpdate(PriorityTaskTileService::class.java)
        }

        if (shouldRefreshComplication) {
            Log.d(TAG, "Requesting complication update")
            ComplicationDataSourceUpdateRequester.create(
                this,
                ComponentName(this, StartRecordingComplicationService::class.java)
            ).requestUpdateAll()
        }
    }
}

