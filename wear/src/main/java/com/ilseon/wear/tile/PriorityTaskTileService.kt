package com.ilseon.wear.tile

import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

@OptIn(ExperimentalHorologistApi::class)
class PriorityTaskTileService : SuspendingTileService() {

    companion object {
        private const val RESOURCES_VERSION = "1"
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        // Handle button taps — the clickable ID carries the action path
        val lastClickId = requestParams.currentState.lastClickableId
        val togglingRecording = lastClickId == WearTaskData.ACTION_TOGGLE_RECORDING

        if (togglingRecording) {
            // Centralized toggle: flips optimistic state + sends message to phone
            WearTaskDataLoader.toggleRecording(this)
            // Haptic confirmation so the user knows the tap registered
            val vibrator = getSystemService(VibratorManager::class.java)?.defaultVibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else if (lastClickId.isNotEmpty()) {
            WearTaskDataLoader.sendAction(this, lastClickId)
        }

        val task = WearTaskDataLoader.loadTaskData(this)
        // loadRecordingState now returns the optimistic value when a toggle is pending
        val isRecording = WearTaskDataLoader.loadRecordingState(this)

        val layout = PriorityTaskTileRenderer.buildLayout(task, isRecording, this)

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(15 * 60 * 1000L) // refresh every 15 min as safety net
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(layout)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return PriorityTaskTileRenderer.buildResources(this)
    }
}
