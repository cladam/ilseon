package com.ilseon.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.ilseon.wear.R
import com.ilseon.wear.tile.WearTaskDataLoader

/**
 * Complication data source for "Ilseon Voice Memo".
 *
 * Supports SHORT_TEXT, RANGED_VALUE, SMALL_IMAGE, and MONOCHROMATIC_IMAGE
 * so watch faces can pick whichever slot style they prefer. Tapping the
 * complication launches [ToggleRecordingActivity] which sends a
 * toggle-recording message to the phone app, reusing the same
 * [WearTaskDataLoader] that the Tile and main activity use.
 *
 * The RANGED_VALUE type shows a progress arc that fills when recording
 * is active, giving clear visual feedback on the watch face.
 */
class StartRecordingComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "VoiceMemoComplication"
    }

    // ─── Preview (shown in complication picker) ───────────────

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return buildComplicationData(type, isRecording = false, isTapActionRequired = false)
    }

    // ─── Live data ────────────────────────────────────────────

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val isRecording = try {
            WearTaskDataLoader.loadRecordingState(this)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read recording state", e)
            false
        }

        Log.d(TAG, "Complication update — isRecording=$isRecording, type=${request.complicationType}")
        return buildComplicationData(request.complicationType, isRecording, isTapActionRequired = true)
    }

    // ─── Helpers ──────────────────────────────────────────────

    private fun buildComplicationData(
        type: ComplicationType,
        isRecording: Boolean,
        isTapActionRequired: Boolean
    ): ComplicationData? {
        val tapAction = if (isTapActionRequired) createTapAction() else null
        val label = if (isRecording) "REC" else "Memo"
        val contentDesc = if (isRecording) "Stop recording" else "Start voice memo"
        val micIcon = Icon.createWithResource(this, R.drawable.ic_complication_mic)
        val monoImage = MonochromaticImage.Builder(micIcon).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(label).build(),
                    contentDescription = PlainComplicationText.Builder(contentDesc).build()
                )
                    .setMonochromaticImage(monoImage)
                    .apply { tapAction?.let { setTapAction(it) } }
                    .build()
            }

            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = if (isRecording) 1f else 0f,
                    min = 0f,
                    max = 1f,
                    contentDescription = PlainComplicationText.Builder(contentDesc).build()
                )
                    .setText(PlainComplicationText.Builder(label).build())
                    .setMonochromaticImage(monoImage)
                    .apply { tapAction?.let { setTapAction(it) } }
                    .build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monoImage,
                    PlainComplicationText.Builder(contentDesc).build()
                )
                    .apply { tapAction?.let { setTapAction(it) } }
                    .build()
            }

            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(micIcon, SmallImageType.ICON).build(),
                    PlainComplicationText.Builder(contentDesc).build()
                )
                    .apply { tapAction?.let { setTapAction(it) } }
                    .build()
            }

            else -> {
                Log.w(TAG, "Unsupported complication type: $type")
                null
            }
        }
    }

    private fun createTapAction(): PendingIntent {
        val intent = Intent(this, ToggleRecordingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
