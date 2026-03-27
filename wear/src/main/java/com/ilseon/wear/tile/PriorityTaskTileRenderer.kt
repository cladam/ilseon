package com.ilseon.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ResourceBuilders
import com.ilseon.wear.R

/**
 * Builds the protolayout for the Priority Task tile.
 *
 * Layout (round watch face):
 *  ┌───────────────────┐
 *  │   ★ Task Title    │
 *  │   ─────────────   │  ← divider (teal or red if overdue)
 *  │   Description...  │
 *  │                   │
 *  │       [🎤]        │  ← voice recording toggle
 *  └───────────────────┘
 */
object PriorityTaskTileRenderer {

    // --- Ilseon palette ---
    private const val COLOR_BACKGROUND = 0xFF121212.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFFE0E0E0.toInt()
    private const val COLOR_TEXT_SECONDARY = 0xFF9E9E9E.toInt()
    private const val COLOR_TEAL = 0xFF5A9B80.toInt()
    private const val COLOR_RED = 0xFFB35F5F.toInt()
    private const val COLOR_BUTTON_BG = 0xFF1E1E1E.toInt()
    private const val COLOR_RECORDING = 0xFFEF5350.toInt()

    const val ID_ICON_MIC = "icon_mic"

    // ─── Public API ───────────────────────────────────────────

    fun buildLayout(task: WearTaskData?, isRecording: Boolean, context: Context): LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(COLOR_BACKGROUND))
                            .build()
                    )
                    .setPadding(
                        Padding.Builder()
                            .setAll(dp(14f))
                            .build()
                    )
                    .build()
            )
            .addContent(
                Column.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .apply {
                        if (task != null) {
                            addContent(taskContent(task))
                        } else {
                            addContent(emptyContent())
                        }
                        // Push button to bottom
                        addContent(
                            Spacer.Builder()
                                .setWidth(expand())
                                .setHeight(expand())
                                .build()
                        )
                        addContent(recordingButton(isRecording))
                    }
                    .build()
            )
            .build()
    }

    fun buildResources(context: Context): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .addIdToImageMapping(
                ID_ICON_MIC,
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.ic_outline_mic_24)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // ─── Task content ─────────────────────────────────────────

    private fun taskContent(task: WearTaskData): LayoutElement {
        val dividerColor = if (task.isOverdue) COLOR_RED else COLOR_TEAL

        return Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_START)
            .apply {
                addContent(titleRow(task))
                addContent(spacer(6f))
                addContent(divider(dividerColor))
                addContent(spacer(6f))
                task.description?.let {
                    addContent(
                        Text.Builder()
                            .setText(it)
                            .setMaxLines(3)
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(13f))
                                    .setColor(argb(COLOR_TEXT_SECONDARY))
                                    .build()
                            )
                            .build()
                    )
                }
            }
            .build()
    }

    private fun titleRow(task: WearTaskData): LayoutElement {
        val titleText = if (task.isUrgent) "🔥 ${task.title}" else task.title
        return Text.Builder()
            .setText(titleText)
            .setMaxLines(2)
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(16f))
                    .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                    .setColor(argb(COLOR_TEXT_PRIMARY))
                    .build()
            )
            .build()
    }

    private fun divider(color: Int): LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(dp(2f))
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(color))
                            .setCorner(Corner.Builder().setRadius(dp(1f)).build())
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // ─── Empty state ──────────────────────────────────────────

    private fun emptyContent(): LayoutElement {
        return Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(spacer(16f))
            .addContent(
                Text.Builder()
                    .setText("No priority task")
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(15f))
                            .setColor(argb(COLOR_TEXT_PRIMARY))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // ─── Recording button ─────────────────────────────────────

    private fun recordingButton(isRecording: Boolean): LayoutElement {
        val bgColor = if (isRecording) COLOR_RECORDING else COLOR_BUTTON_BG
        val tintColor = if (isRecording) 0xFFFFFFFF.toInt() else COLOR_TEAL

        return Column.Builder()
            .setWidth(wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Box.Builder()
                    .setWidth(dp(40f))
                    .setHeight(dp(40f))
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .setModifiers(
                        Modifiers.Builder()
                            .setBackground(
                                Background.Builder()
                                    .setColor(argb(bgColor))
                                    .setCorner(Corner.Builder().setRadius(dp(20f)).build())
                                    .build()
                            )
                            .setClickable(
                                Clickable.Builder()
                                    .setId(WearTaskData.ACTION_TOGGLE_RECORDING)
                                    .setOnClick(ActionBuilders.LoadAction.Builder().build())
                                    .build()
                            )
                            .build()
                    )
                    .addContent(
                        Image.Builder()
                            .setWidth(dp(22f))
                            .setHeight(dp(22f))
                            .setResourceId(ID_ICON_MIC)
                            .setColorFilter(
                                LayoutElementBuilders.ColorFilter.Builder()
                                    .setTint(argb(tintColor))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(spacer(3f))
            .addContent(
                Text.Builder()
                    .setText(if (isRecording) "Recording…" else "Voice Memo")
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(10f))
                            .setColor(argb(if (isRecording) COLOR_RECORDING else COLOR_TEXT_SECONDARY))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    // ─── Helpers ──────────────────────────────────────────────

    private fun spacer(heightDp: Float): LayoutElement {
        return Spacer.Builder()
            .setWidth(expand())
            .setHeight(dp(heightDp))
            .build()
    }
}
