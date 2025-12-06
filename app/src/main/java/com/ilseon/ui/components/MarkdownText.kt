package com.ilseon.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    MarkdownText(
        markdown = markdown,
        modifier = modifier,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onLinkClicked = { link ->
            // Let the system handle all links. This will correctly trigger the deep link.
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        }
    )
}