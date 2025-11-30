package com.ilseon.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    MarkdownText(
        markdown = markdown,
        modifier = modifier,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}