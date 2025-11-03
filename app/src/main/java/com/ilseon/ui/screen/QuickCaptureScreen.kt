package com.ilseon.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ilseon.ui.theme.IlseonTheme

@Composable
fun QuickCaptureScreen() {
    Text(text = "Quick Capture Screen")
}

@Preview(showBackground = true)
@Composable
fun QuickCaptureScreenPreview() {
    IlseonTheme {
        QuickCaptureScreen()
    }
}