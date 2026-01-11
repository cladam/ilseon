package com.ilseon.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.AnalyticsViewModel
import com.ilseon.TimeInterval
import com.ilseon.ui.components.AppCard
import com.ilseon.ui.theme.BlueTeal
import com.ilseon.ui.theme.MutedGreen
import com.ilseon.ui.theme.MutedRed
import com.ilseon.ui.theme.MutedTeal
import com.ilseon.ui.theme.QuietAmber
import com.ilseon.ui.theme.SlateBlue

// Data class to hold simulated analysis results
data class AnalyticsData(
    val focusDistribution: Map<String, Float>, // Context name to percentage
    val averageTimeBlockMinutes: Int,
    val averageDurationMinutes: Int,
    val topKeywords: List<Pair<String, Int>>, // Keyword and count
    val overdueTasksCount: Int,
    val interruptedTasksCount: Int,
    val ideasCount: Int,
    val voiceMemosCount: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToCompletedTasks: () -> Unit
) {
    val data by viewModel.analyticsData.collectAsState()
    val momentumData by viewModel.momentumData.collectAsState()
    val selectedInterval by viewModel.selectedInterval.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "My Momentum",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            item {
                MomentumTimeline(momentumData)
            }

            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "My Focus Patterns",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            item {
                TimeIntervalDropdown(
                    selectedInterval = selectedInterval,
                    onIntervalSelected = { viewModel.selectTimeInterval(it) }
                )
            }

            if (data == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item {
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Focus Distribution",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (data!!.focusDistribution.isEmpty()) {
                                Text("No data for this period.", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                FocusDistributionChart(data!!.focusDistribution)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Avg. Time Block",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${data!!.averageTimeBlockMinutes} min",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        AppCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Avg. Duration",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${data!!.averageDurationMinutes} min",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Ideas Captured",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${data!!.ideasCount}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        AppCard(
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Voice Memos",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${data!!.voiceMemosCount}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Overdue Tasks",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${data!!.overdueTasksCount}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Interrupted Time Blocks",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${data!!.interruptedTasksCount}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Top Reflection Keywords",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (data!!.topKeywords.isEmpty()) {
                                Text("No reflection keywords found for this period.", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    data!!.topKeywords.forEach { (keyword, count) ->
                                        KeywordChip(keyword)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onNavigateToCompletedTasks) {
                                Text("View All Completed Tasks")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private fun TimeInterval.toDisplayString(): String {
    return when (this) {
        TimeInterval.WEEK -> "This Week"
        TimeInterval.MONTH -> "This Month"
        TimeInterval.YEAR -> "This Year"
        TimeInterval.ALL_TIME -> "All Time"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeIntervalDropdown(
    selectedInterval: TimeInterval,
    onIntervalSelected: (TimeInterval) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val items = TimeInterval.entries.toTypedArray()

    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Text(
                text = "${selectedInterval.toDisplayString()} ⌄",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .width(120.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(120.dp)
            ) {
                items.forEach { interval ->
                    DropdownMenuItem(
                        text = { Text(interval.toDisplayString()) },
                        onClick = {
                            onIntervalSelected(interval)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FocusDistributionChart(distribution: Map<String, Float>) {
    val total = distribution.values.sum()
    val sortedDistribution = remember(distribution) {
        distribution.entries.sortedByDescending { it.value }
    }

    val chartColors = remember(sortedDistribution.size) {
        listOf(
            MutedTeal,
            QuietAmber,
            MutedRed,
            BlueTeal,
            MutedGreen,
            SlateBlue
        ).let { baseColors ->
            if (sortedDistribution.size > baseColors.size) {
                baseColors + (baseColors.indices).flatMap {
                    listOf(baseColors[it].copy(alpha = 0.7f), baseColors[it].copy(alpha = 0.4f))
                }
            } else {
                baseColors
            }
        }.take(sortedDistribution.size)
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                sortedDistribution.forEachIndexed { index, item ->
                    val sweepAngle = (item.value / total) * 360f * animationProgress
                    drawArc(
                        color = chartColors[index],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += (item.value / total) * 360f
                }
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedDistribution.forEachIndexed { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(chartColors[index], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.key}: ${String.format("%.1f", entry.value / total * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


@Composable
fun KeywordChip(keyword: String) {
    AssistChip(
        onClick = { /* Handle filtering or drilling down */ },
        label = { Text(keyword) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    )
}
