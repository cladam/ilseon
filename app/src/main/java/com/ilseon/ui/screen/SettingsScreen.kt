package com.ilseon.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilseon.ui.theme.IlseonTheme

@Composable
fun SettingsScreen(
    onManageContextsClick: () -> Unit,
    onCompletedTasksClick: () -> Unit
) {
    SettingsScreenContent(
        onManageContextsClick = onManageContextsClick,
        onCompletedTasksClick = onCompletedTasksClick
    )
}

@Composable
private fun SettingsScreenContent(
    onManageContextsClick: () -> Unit,
    onCompletedTasksClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DataManagementCard(
                onManageContextsClick = onManageContextsClick,
                onCompletedTasksClick = onCompletedTasksClick
            )
        }
        // Add other settings cards here in the future
    }
}

@Composable
private fun DataManagementCard(
    onManageContextsClick: () -> Unit,
    onCompletedTasksClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsItem(
                icon = Icons.Default.Category,
                title = "Manage Contexts",
                subtitle = "Add or remove task contexts",
                onClick = onManageContextsClick
            )
            SettingsItem(
                icon = Icons.Default.History,
                title = "Completed Tasks",
                subtitle = "Review your accomplishments",
                onClick = onCompletedTasksClick
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    IlseonTheme {
        SettingsScreenContent(onManageContextsClick = {}, onCompletedTasksClick = {})
    }
}
