package com.ilseon.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ilseon.ui.theme.Teal
import com.ilseon.ui.theme.TealAccent

// Data class to hold simulated analysis results
data class AnalyticsData(
    val focusDistribution: Map<String, Float>, // Context name to percentage
    val averageTimeBlockMinutes: Int,
    val averageDurationMinutes: Int,
    val topKeywords: List<Pair<String, Int>> // Keyword and count
)

// Mock Data for the Preview
private fun generateMockData() = AnalyticsData(
    focusDistribution = mapOf(
        "Work" to 0.55f,
        "Family" to 0.25f,
        "Health" to 0.10f,
        "Personal" to 0.10f
    ),
    averageTimeBlockMinutes = 230,
    averageDurationMinutes = 120,
    topKeywords = listOf(
        "Distracted" to 12,
        "Flow" to 8,
        "Forgot" to 6,
        "Ok" to 4
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    data: AnalyticsData = generateMockData(),
    onNavigateBack: () -> Unit,
    onNavigateToCompletedTasks: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Focus Patterns") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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

            // 1. Time Scope Selector
            item {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Placeholder for a simple dropdown filter (e.g., "This Week", "Last Month", "All time")
                    Text("This Week ⌄", color = TealAccent, fontWeight = FontWeight.SemiBold)
                }
            }

            // 2. Focus Distribution (Pie Chart Simulation)
            item {
                AnalyticsCard(title = "Focus Distribution") {
                    FocusDistributionChart(data.focusDistribution)
                }
            }

            // 3. Average Focus Time Insight (Time Blindness Check)
            item {
                AnalyticsCard(title = "Average Time Block") {
                    Text(
                        text = "${data.averageTimeBlockMinutes} minutes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TealAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Breakdown: Work: 65 min / Health: 30 min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                AnalyticsCard(title = "Average Duration") {
                    Text(
                        text = "${data.averageDurationMinutes} minutes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TealAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Breakdown: Work: 60 min / Family: 30 min / Health: 30 min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Reflection Keywords (Pattern Recognition)
            item {
                AnalyticsCard(title = "Top Reflection Keywords") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.topKeywords.forEach { (keyword, count) ->
                            KeywordChip(keyword, count)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onNavigateToCompletedTasks) {
                        Text("> View All Completed Tasks")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, content: @Composable (ColumnScope.() -> Unit)) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun FocusDistributionChart(distribution: Map<String, Float>) {
    // This simulates a horizontal bar chart showing distribution
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        distribution.entries.sortedByDescending { it.value }.forEach { (context, percentage) ->
            val barColor = Teal
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
fun KeywordChip(keyword: String, count: Int) {
    AssistChip(
        onClick = { /* Handle filtering or drilling down */ },
        label = { Text("$keyword ($count)") },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            labelColor = MaterialTheme.colorScheme.onSurface,
        )
    )
}
