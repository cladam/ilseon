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
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.ilseon.wear.R

/**
 * Complication data source for "Quick Follow-Up / Emergency Check-In".
 *
 * Supports SHORT_TEXT, MONOCHROMATIC_IMAGE, and SMALL_IMAGE.
 * Tapping the complication launches [QuickUrgentTaskActivity], which provides
 * an immediate wrist tactile nudge and sends `/action/trigger-followup`
 * to the phone companion app without displaying UI.
 */
class QuickFollowUpComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "QuickFollowUpComplication"
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return buildComplicationData(type, isTapActionRequired = false)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "Quick follow-up complication requested, type=${request.complicationType}")
        return buildComplicationData(request.complicationType, isTapActionRequired = true)
    }

    private fun buildComplicationData(
        type: ComplicationType,
        isTapActionRequired: Boolean
    ): ComplicationData? {
        val tapAction = if (isTapActionRequired) createTapAction() else null
        val label = "CHECK-IN"
        val contentDesc = "Trigger Quick Follow-Up & Emergency Task"
        val followUpIcon = Icon.createWithResource(this, R.drawable.ic_complication_followup)
        val monoImage = MonochromaticImage.Builder(followUpIcon).build()

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
                    SmallImage.Builder(followUpIcon, SmallImageType.ICON).build(),
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
        val intent = Intent(this, QuickUrgentTaskActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
