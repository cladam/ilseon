package com.ilseon.widget

import android.content.Context
import android.content.Intent
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
import com.ilseon.data.userstatus.UserStatusRepository
import com.ilseon.ui.theme.MutedDetail
import com.ilseon.ui.theme.MutedTeal
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PriorityWidgetEntryPoint {
    fun taskRepository(): TaskRepository
    fun userStatusRepository(): UserStatusRepository
}

class PriorityWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, PriorityWidgetEntryPoint::class.java)
        val taskRepository = entryPoint.taskRepository()
        val userStatusRepository = entryPoint.userStatusRepository()

        val activeFocusBlock = taskRepository.getActiveFocusBlock().firstOrNull()
        val task = if (activeFocusBlock != null) {
            taskRepository.getIncompleteTasksByContext(activeFocusBlock.contextId).firstOrNull()
                ?.firstOrNull()
        } else {
            taskRepository.getCurrentPriorityTask().firstOrNull()
        }

        val isCurrentFocus = task?.isCurrentPriority ?: false
        val isOverdue = task?.dueTime?.let { it < System.currentTimeMillis() } ?: false
        provideContent {
            GlanceTheme(WidgetTheme.colors) {
                PriorityWidgetContent(task, isCurrentFocus, isOverdue)
            }
        }
    }
    @Composable
    private fun PriorityWidgetContent(task: Task?, isCurrentFocus: Boolean, isOverdue: Boolean) {
        val dividerColor = if (isOverdue) GlanceTheme.colors.error.getColor(LocalContext.current) else MutedTeal
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            if (task != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.img),
                        contentDescription = "App Icon",
                        modifier = GlanceModifier.size(32.dp).padding(end = 4.dp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row(modifier = GlanceModifier.wrapContentWidth()) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_outline_add_task_24),
                            contentDescription = "New Task",
                            colorFilter = ColorFilter.tint(ColorProvider(MutedDetail)),
                            modifier = GlanceModifier.size(24.dp).clickable(
                                actionStartActivity(
                                    Intent(context, ActionTrampolineActivity::class.java)
                                        .setAction("com.ilseon.action.NEW_TASK")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(6.dp))
                        Image(
                            provider = ImageProvider(R.drawable.ic_outline_lightbulb_24),
                            contentDescription = "New Idea",
                            colorFilter = ColorFilter.tint(ColorProvider(MutedDetail)),
                            modifier = GlanceModifier.size(24.dp).clickable(
                                actionStartActivity(
                                    Intent(context, ActionTrampolineActivity::class.java)
                                        .setAction("com.ilseon.action.NEW_IDEA")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(6.dp))
                        Image(
                            provider = ImageProvider(R.drawable.ic_outline_mic_24),
                            contentDescription = "New Voice Memo",
                            colorFilter = ColorFilter.tint(ColorProvider(MutedDetail)),
                            modifier = GlanceModifier.size(24.dp).clickable(
                                actionStartActivity(
                                    Intent(context, ActionTrampolineActivity::class.java)
                                        .setAction("com.ilseon.action.NEW_VOICE_MEMO")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isUrgent) {
                        Image(
                            provider = ImageProvider(android.R.drawable.btn_star_big_on),
                            contentDescription = "Urgent",
                            modifier = GlanceModifier.size(20.dp).padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = task.title,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(dividerColor)
                ) {}
                Spacer(modifier = GlanceModifier.height(8.dp))

                task.description?.let {
                    Text(
                        text = it,
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 14.sp
                        )
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.img),
                        contentDescription = "App Icon",
                        modifier = GlanceModifier.size(36.dp).padding(end = 8.dp)
                    )
                    Text(
                        text = "Ilseon",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row {
                        Image(
                            provider = ImageProvider(R.drawable.ic_outline_add_task_24),
                            contentDescription = "New Task",
                            modifier = GlanceModifier.size(28.dp).clickable(
                                actionStartActivity(
                                    Intent(context, ActionTrampolineActivity::class.java).apply {
                                        action = "com.ilseon.action.NEW_TASK"
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(8.dp))
                        Image(
                            provider = ImageProvider(R.drawable.ic_outline_lightbulb_24),
                            contentDescription = "New Idea",
                            modifier = GlanceModifier.size(28.dp).clickable(
                                actionStartActivity(
                                    Intent(context, ActionTrampolineActivity::class.java).apply {
                                        action = "com.ilseon.action.NEW_IDEA"
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(8.dp))
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_btn_speak_now),
                            contentDescription = "New Voice Memo",
                            modifier = GlanceModifier.size(28.dp).clickable(
                                actionStartActivity(
                                    Intent(context, ActionTrampolineActivity::class.java).apply {
                                        action = "com.ilseon.action.NEW_VOICE_MEMO"
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            )
                        )
                    }

                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "No priority task set.",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

}