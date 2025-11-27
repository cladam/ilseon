package com.ilseon.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ilseon.ActionTrampolineActivity
import com.ilseon.MainActivity
import com.ilseon.R
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskRepository
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
}

class PriorityWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val taskRepository = EntryPointAccessors.fromApplication(context, PriorityWidgetEntryPoint::class.java).taskRepository()
        val task = taskRepository.getCurrentPriorityTask().firstOrNull()
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
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .clickable(actionStartActivity<MainActivity>())
                .padding(16.dp)
        ) {
            if (task != null) {
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
                        text = if (isCurrentFocus) "Ilseon Current Focus" else "Current Priority",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(
                        provider = ImageProvider(android.R.drawable.ic_input_add),
                        contentDescription = "Quick Capture",
                        modifier = GlanceModifier.size(24.dp).clickable(actionStartActivity<ActionTrampolineActivity>())
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = task.title,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // This Box is the horizontal divider line
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(dividerColor)
                ) {} // The empty content block fixes the error

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
                        provider = ImageProvider(R.drawable.ic_launcher),
                        contentDescription = "App Icon",
                        modifier = GlanceModifier.size(24.dp).padding(end = 8.dp)
                    )
                    Text(
                        text = "Ilseon",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 12.sp
                        )
                    )
                     Spacer(modifier = GlanceModifier.defaultWeight())
                     Image(
                         provider = ImageProvider(android.R.drawable.ic_input_add),
                         contentDescription = "Quick Capture",
                         modifier = GlanceModifier.size(24.dp).clickable(actionStartActivity<ActionTrampolineActivity>())
                     )
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