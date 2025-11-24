package com.ilseon.ui.screen

import android.R.attr.enabled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.AnalyticsViewModel
import com.ilseon.TimeInterval
import kotlin.math.log10

// Data class to hold simulated analysis results
data class AnalyticsData(
    val focusDistribution: Map<String, Float>, // Context name to percentage
    val averageTimeBlockMinutes: Int,
    val averageDurationMinutes: Int,
    val topKeywords: List<Pair<String, Int>>, // Keyword and count
    val overdueTasksCount: Int,
    val interruptedTasksCount: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToCompletedTasks: () -> Unit
) {
    val data by viewModel.analyticsData.collectAsState()
    val selectedInterval by viewModel.selectedInterval.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Focus Patterns") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

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
                    AnalyticsCard(title = "Focus Distribution") {
                        if (data!!.focusDistribution.isEmpty()) {
                            Text("No data for this period.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            FocusDistributionChart(data!!.focusDistribution)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AnalyticsCard(
                            title = "Avg. Time Block",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${data!!.averageTimeBlockMinutes} min",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        AnalyticsCard(
                            title = "Avg. Duration",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${data!!.averageDurationMinutes} min",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    AnalyticsCard(title = "Overdue Tasks") {
                        Text(
                            text = "${data!!.overdueTasksCount}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    AnalyticsCard(title = "Interrupted Time Blocks") {
                        Text(
                            text = "${data!!.interruptedTasksCount}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    AnalyticsCard(title = "Top Reflection Keywords") {
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
                color = MaterialTheme.colorScheme.secondary,
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
fun AnalyticsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp) // Ensures consistent height
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun FocusDistributionChart(distribution: Map<String, Float>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        distribution.entries.sortedByDescending { it.value }.forEach { (context, percentage) ->
            val barColor = MaterialTheme.colorScheme.secondary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = context,
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = percentage)
                            .background(barColor)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    )
}
