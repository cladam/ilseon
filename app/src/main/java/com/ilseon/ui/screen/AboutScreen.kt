package com.ilseon.ui.screen

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilseon.ui.theme.IlseonTheme

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val versionName = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        "N/A"
    }
    AboutScreenContent(versionName = versionName)
}

@Composable
private fun AboutScreenContent(versionName: String?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AppInfoCard(versionName = versionName) }
        item { DeveloperInfoCard() }
    }
}

@Composable
private fun AppInfoCard(versionName: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ilseon",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A minimalist executive function assistant designed to reduce mental overload.",
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            InfoRow(icon = Icons.Default.Info, text = "Version ${versionName ?: "N/A"}")
        }
    }
}

@Composable
private fun DeveloperInfoCard() {
    val uriHandler = LocalUriHandler.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Claes Adamsson (@cladam)",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Software Engineer & Engineering Manager",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            InfoRow(
                icon = Icons.Default.Code,
                text = "View on GitHub",
                onClick = {
                    uriHandler.openUri("https://github.com/cladam")
                }
            )
            InfoRow(
                icon = Icons.Default.Language,
                text = "More About the Developer",
                onClick = {
                    uriHandler.openUri("https://cladam.github.io/")
                }
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, onClick: (() -> Unit)? = null) {
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
