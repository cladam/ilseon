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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ilseon.SettingsViewModel
import com.ilseon.util.UsageStatsReader
import java.util.Locale

@Composable
fun SettingsScreen(
    onCompletedTasksClick: () -> Unit,
    onAboutClick: () -> Unit,
    onExportClick: () -> Unit,
    onArchiveClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val nudgeNotificationsEnabled by viewModel.nudgeNotificationsEnabled.collectAsState()
    val naggingNotificationsEnabled by viewModel.naggingNotificationsEnabled.collectAsState()
    val bluetoothSstEnabled by viewModel.bluetoothSstEnabled.collectAsState()
    val sstLanguage by viewModel.sstLanguage.collectAsState()

    SettingsScreenContent(
        onCompletedTasksClick = onCompletedTasksClick,
        onAboutClick = onAboutClick,
        onExportClick = onExportClick,
        onArchiveClick = onArchiveClick,
        nudgeNotificationsEnabled = nudgeNotificationsEnabled,
        onNudgeNotificationsChange = viewModel::setNudgeNotificationsEnabled,
        naggingNotificationsEnabled = naggingNotificationsEnabled,
        onNaggingNotificationsChange = viewModel::setNaggingNotificationsEnabled,
        bluetoothSstEnabled = bluetoothSstEnabled,
        onBluetoothSstEnabledChange = viewModel::setBluetoothSstEnabled,
        sstLanguage = sstLanguage,
        onSstLanguageChange = viewModel::setSstLanguage
    )
}

@Composable
private fun SettingsScreenContent(
    onCompletedTasksClick: () -> Unit,
    onAboutClick: () -> Unit,
    onExportClick: () -> Unit,
    onArchiveClick: () -> Unit,
    nudgeNotificationsEnabled: Boolean,
    onNudgeNotificationsChange: (Boolean) -> Unit,
    naggingNotificationsEnabled: Boolean,
    onNaggingNotificationsChange: (Boolean) -> Unit,
    bluetoothSstEnabled: Boolean,
    onBluetoothSstEnabledChange: (Boolean) -> Unit,
    sstLanguage: String,
    onSstLanguageChange: (String) -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = sstLanguage,
            onLanguageSelected = {
                onSstLanguageChange(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NotificationSettingsCard(
                nudgeNotificationsEnabled = nudgeNotificationsEnabled,
                onNudgeNotificationsChange = onNudgeNotificationsChange,
                naggingNotificationsEnabled = naggingNotificationsEnabled,
                onNaggingNotificationsChange = onNaggingNotificationsChange
            )
        }
        item {
            PermissionsSettingsCard()
        }
        item {
            SpeechToTextSettingsCard(
                bluetoothSstEnabled = bluetoothSstEnabled,
                onBluetoothSstEnabledChange = onBluetoothSstEnabledChange,
                sstLanguage = sstLanguage,
                onLanguageClick = { showLanguageDialog = true }
            )
        }
        item {
            DataManagementCard(
                onCompletedTasksClick = onCompletedTasksClick,
                onExportClick = onExportClick,
                onArchiveClick = onArchiveClick
            )
        }
        item {
            AboutCard(
                onAboutClick = onAboutClick
            )
        }
    }
}

@Composable
private fun PermissionsSettingsCard() {
    val context = LocalContext.current
    val usageStatsReader = remember { UsageStatsReader(context) }
    val hasPermission by remember { mutableStateOf(usageStatsReader.hasUsageStatsPermission()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsItem(
                icon = Icons.Default.PhonelinkSetup,
                title = "Usage Access",
                subtitle = if (hasPermission) "Granted" else "Required for phone pickup tracking",
                onClick = {
                    usageStatsReader.requestUsageStatsPermission()
                }
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf("en-GB" to "British English", "sv-SE" to "Swedish")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 12.dp),
                        color = if (code == currentLanguage) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NotificationSettingsCard(
    nudgeNotificationsEnabled: Boolean,
    onNudgeNotificationsChange: (Boolean) -> Unit,
    naggingNotificationsEnabled: Boolean,
    onNaggingNotificationsChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSwitchItem(
                icon = Icons.Default.Notifications,
                title = "Nudge Notifications",
                subtitle = "Receive a reminder before a task starts",
                checked = nudgeNotificationsEnabled,
                onCheckedChange = onNudgeNotificationsChange
            )
            SettingsSwitchItem(
                icon = Icons.Default.Repeat,
                title = "Nagging Notifications",
                subtitle = "Repeat high-priority task reminders",
                checked = naggingNotificationsEnabled,
                onCheckedChange = onNaggingNotificationsChange
            )
        }
    }
}

@Composable
private fun SpeechToTextSettingsCard(
    bluetoothSstEnabled: Boolean,
    onBluetoothSstEnabledChange: (Boolean) -> Unit,
    sstLanguage: String,
    onLanguageClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Speech to Text",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSwitchItem(
                icon = Icons.Default.Bluetooth,
                title = "Bluetooth speech-to-text",
                subtitle = "Use speech-to-text while on Bluetooth",
                checked = bluetoothSstEnabled,
                onCheckedChange = onBluetoothSstEnabledChange
            )
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = Locale(sstLanguage.split("-")[0], sstLanguage.split("-")[1]).displayName,
                onClick = onLanguageClick
            )
        }
    }
}

@Composable
private fun DataManagementCard(
    onCompletedTasksClick: () -> Unit,
    onExportClick: () -> Unit,
    onArchiveClick: () -> Unit
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
                icon = Icons.Default.History,
                title = "Completed Tasks",
                subtitle = "Review your accomplishments",
                onClick = onCompletedTasksClick
            )
            SettingsItem(
                icon = Icons.Default.Download,
                title = "Export Data",
                subtitle = "Save your data to a file",
                onClick = onExportClick
            )
            SettingsItem(
                icon = Icons.Default.Archive,
                title = "Archive Recurring Tasks",
                subtitle = "Manage your recurring tasks",
                onClick = onArchiveClick
            )
        }
    }
}

@Composable
private fun AboutCard(
    onAboutClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About Ilseon",
                subtitle = "Learn more about the app",
                onClick = onAboutClick
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

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) })
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
