package com.ilseon.ui.components

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilseon.data.routine.RoutineRun
import com.ilseon.data.routine.RoutineWithSteps
import com.ilseon.ui.theme.BorderQuiet
import com.ilseon.ui.theme.LightGrey
import com.ilseon.ui.theme.MutedDetail
import com.ilseon.ui.theme.MutedTeal

/**
 * Hero Card shown on Dashboard when an active Routine time window is open.
 */
@Composable
fun RoutineHeroCard(
    routineWithSteps: RoutineWithSteps,
    todayRun: RoutineRun?,
    onStartRoutine: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = routineWithSteps.sortedSteps
    val currentIndex = todayRun?.currentStepIndex ?: 0
    val isStarted = currentIndex > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightGrey
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderQuiet)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MutedTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MutedTeal
                        )
                    }
                    Text(
                        text = "ROUTINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedTeal,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (routineWithSteps.totalEstimatedMinutes > 0) {
                    Text(
                        text = "~${routineWithSteps.totalEstimatedMinutes} mins",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedDetail
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = routineWithSteps.routine.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            routineWithSteps.routine.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedDetail,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartRoutine,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MutedTeal,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isStarted) "Continue Routine (Step ${currentIndex + 1}/${steps.size})" else "Start Routine",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Compact Floating Progress Bar shown on the Dashboard when a Routine session
 * is currently running in the background / minimized.
 */
@Composable
fun RoutineCompactBar(
    routineWithSteps: RoutineWithSteps,
    todayRun: RoutineRun,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = routineWithSteps.sortedSteps
    val currentIndex = todayRun.currentStepIndex.coerceIn(0, steps.size)
    val isComplete = currentIndex >= steps.size
    val currentStep = if (!isComplete && steps.isNotEmpty()) steps[currentIndex] else null
    val progress = if (steps.isNotEmpty()) currentIndex.toFloat() / steps.size else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightGrey
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderQuiet),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routineWithSteps.routine.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentStep?.title ?: "Complete!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1}/${steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedDetail
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Resume",
                        tint = MutedDetail
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MutedTeal,
                trackColor = BorderQuiet
            )
        }
    }
}
