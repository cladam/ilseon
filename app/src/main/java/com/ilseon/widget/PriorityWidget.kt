package com.ilseon.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ilseon.ActionTrampolineActivity
import com.ilseon.MainActivity
import com.ilseon.R
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskRepository
import com.ilseon.ui.theme.MutedDetail
import com.ilseon.ui.theme.MutedTeal
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


object WidgetActionColors {
    val TaskPrimary = Color(0xFFB35F5F)
    val TaskBackground = Color(0xFFB35F5F).copy(alpha = 0.12f)
    val TaskBorder = Color(0xFFB35F5F).copy(alpha = 0.25f)

    val IdeaPrimary = Color(0xFFC08A3E)
    val IdeaBackground = Color(0xFFC08A3E).copy(alpha = 0.12f)
    val IdeaBorder = Color(0xFFC08A3E).copy(alpha = 0.25f)

    val VoicePrimary = Color(0xFF5A9B80)
    val VoiceBackground = Color(0xFF5A9B80).copy(alpha = 0.12f)
    val VoiceBorder = Color(0xFF5A9B80).copy(alpha = 0.25f)
}

/**
 * Encapsulates the visual identity of widget actions.
 * Simplifies UI code by providing a single source of truth for "Intent" colors.
 */
enum class WidgetAction(
    val iconRes: Int,
    val description: String,
    val intentAction: String,
    val baseColor: Color
) {
    Task(R.drawable.ic_outline_add_task_24, "New Task", "com.ilseon.action.NEW_TASK", Color(0xFFB35F5F)),
    Idea(R.drawable.ic_outline_lightbulb_24, "New Idea", "com.ilseon.action.NEW_IDEA", Color(0xFFC08A3E)),
    Voice(R.drawable.ic_outline_mic_24, "New Voice Memo", "com.ilseon.action.NEW_VOICE_MEMO", Color(0xFF5A9B80));

    val primary get() = ColorProvider(baseColor)
    val background get() = ColorProvider(baseColor.copy(alpha = 0.12f))
    val border get() = ColorProvider(baseColor.copy(alpha = 0.25f))
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PriorityWidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

class PriorityWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, PriorityWidgetEntryPoint::class.java)
        val taskRepository = entryPoint.taskRepository()
        val task = taskRepository.getCurrentPriorityTaskForWidget()

        provideContent {
            GlanceTheme(WidgetTheme.colors) {
                PriorityWidgetContent(task)
            }
        }
    }

    @Composable
    private fun ActionIconButton(
        actionType: WidgetAction,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val context = LocalContext.current
        val intent = Intent(context, ActionTrampolineActivity::class.java).apply {
            action = actionType.intentAction
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // We use a Box with padding to simulate a border, which is more reliable in Glance
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(32.dp)
                .cornerRadius(8.dp)
                .background(actionType.border)
                .clickable(actionStartActivity(intent))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(1.dp) // The "Border" thickness
                    .cornerRadius(7.dp)
                    .background(actionType.background)
            ) {
                Image(
                    provider = ImageProvider(actionType.iconRes),
                    contentDescription = actionType.description,
                    colorFilter = ColorFilter.tint(actionType.primary),
                    modifier = GlanceModifier.size(18.dp)
                )
            }
        }
    }

    @Composable
    private fun PriorityWidgetContent(task: Task?) {
        val isOverdue = task?.dueTime?.let { it < System.currentTimeMillis() } ?: false
        val dividerColor = if (isOverdue) GlanceTheme.colors.error else ColorProvider(WidgetAction.Voice.baseColor)
        Log.d("PriorityWidget", "PriorityWidgetContent: $task")

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            // Header Row (App Icon + Global Actions)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Image(
                    provider = ImageProvider(R.drawable.img),
                    contentDescription = "Ilseon",
                    modifier = GlanceModifier.size(32.dp)
                )

                if (task == null) {
                    Text(
                        text = "Ilseon",
                        modifier = GlanceModifier.padding(start = 8.dp),
                        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 14.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionIconButton(WidgetAction.Task)
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    ActionIconButton(WidgetAction.Idea)
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    ActionIconButton(WidgetAction.Voice)
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (task != null) {
                TaskSection(task, dividerColor)
            } else {
                EmptySection()
            }
        }
    }

    @Composable
    private fun TaskSection(task: Task, dividerColor: ColorProvider) {
        Log.d("PriorityWidget", "TaskSection: $task")
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.isUrgent) {
                    Image(
                        provider = ImageProvider(android.R.drawable.btn_star_big_on),
                        contentDescription = "Urgent",
                        modifier = GlanceModifier.size(18.dp).padding(end = 6.dp)
                    )
                }
                Text(
                    text = task.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(dividerColor)) {}
            Spacer(modifier = GlanceModifier.height(6.dp))

            task.description?.let {
                Text(
                    text = it,
                    maxLines = 2,
                    style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 13.sp)
                )
            }
        }
    }

    @Composable
    private fun EmptySection() {
        Text(
            text = "No priority task set.",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}