package com.ilseon.wear.tile

import android.content.Context
import androidx.annotation.DrawableRes
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
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
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
 * Layout (round watch face, ~192dp usable):
 *  ┌───────────────────┐
 *  │   ★ Task Title    │
 *  │   ─────────────   │   ← divider (teal or red if overdue)
 *  │   Description...  │
 *  │                   │
 *  │  [Task][Idea][Mic]│   ← 3 shortcut buttons
 *  └───────────────────┘
 */
object PriorityTaskTileRenderer {

    // --- Ilseon palette (matches widget) ---
    private const val COLOR_BACKGROUND = 0xFF121212.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFFE0E0E0.toInt()
    private const val COLOR_TEXT_SECONDARY = 0xFF9E9E9E.toInt()
    private const val COLOR_TEAL = 0xFF5A9B80.toInt()
    private const val COLOR_RED = 0xFFB35F5F.toInt()
    private const val COLOR_AMBER = 0xFFC08A3E.toInt()
    private const val COLOR_BUTTON_BG = 0xFF1E1E1E.toInt()

    // Resource IDs for the tile image resources
    const val ID_ICON_TASK = "icon_task"
    const val ID_ICON_IDEA = "icon_idea"
    const val ID_ICON_MIC = "icon_mic"

    // ─── Public API ───────────────────────────────────────────

    fun buildLayout(task: WearTaskData?, context: Context): LayoutElement {
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
                        // Flexible spacer pushes buttons to bottom
                        addContent(
                            Spacer.Builder()
                                .setWidth(expand())
                                .setHeight(expand())
                                .build()
                        )
                        addContent(actionButtonRow())
                    }
                    .build()
            )
            .build()
    }

    fun buildResources(context: Context): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .addIdToImageMapping(
                ID_ICON_TASK,
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.ic_outline_add_task_24)
                            .build()
                    )
                    .build()
            )
            .addIdToImageMapping(
                ID_ICON_IDEA,
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(R.drawable.ic_outline_lightbulb_24)
                            .build()
                    )
                    .build()
            )
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
                // Title row (with urgency indicator)
                addContent(titleRow(task))
                // Divider
                addContent(spacer(6f))
                addContent(divider(dividerColor))
                addContent(spacer(6f))
                // Description
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
            .addContent(spacer(4f))
            .addContent(
                Text.Builder()
                    .setText("Use the buttons below\nto capture something")
                    .setMaxLines(2)
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(12f))
                            .setColor(argb(COLOR_TEXT_SECONDARY))
                            .build()
                    )
                    .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                    .build()
            )
            .build()
    }

    // ─── Action button row ────────────────────────────────────

    private fun actionButtonRow(): LayoutElement {
        return Row.Builder()
            .setWidth(wrap())
            .setHeight(wrap())
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(actionButton(ID_ICON_TASK, COLOR_RED, WearTaskData.ACTION_NEW_TASK))
            .addContent(spacerH(10f))
            .addContent(actionButton(ID_ICON_IDEA, COLOR_AMBER, WearTaskData.ACTION_NEW_IDEA))
            .addContent(spacerH(10f))
            .addContent(actionButton(ID_ICON_MIC, COLOR_TEAL, WearTaskData.ACTION_NEW_VOICE_MEMO))
            .build()
    }

    private fun actionButton(
        imageId: String,
        tintColor: Int,
        actionPath: String
    ): LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(dp(36f))
            .setHeight(dp(36f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(COLOR_BUTTON_BG))
                            .setCorner(Corner.Builder().setRadius(dp(18f)).build())
                            .build()
                    )
                    .setClickable(
                        Clickable.Builder()
                            .setId(actionPath)
                            .setOnClick(
                                ActionBuilders.LoadAction.Builder().build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(
                Image.Builder()
                    .setWidth(dp(20f))
                    .setHeight(dp(20f))
                    .setResourceId(imageId)
                    .setColorFilter(
                        LayoutElementBuilders.ColorFilter.Builder()
                            .setTint(argb(tintColor))
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

    private fun spacerH(widthDp: Float): LayoutElement {
        return Spacer.Builder()
            .setWidth(dp(widthDp))
            .setHeight(dp(1f))
            .build()
    }
}


