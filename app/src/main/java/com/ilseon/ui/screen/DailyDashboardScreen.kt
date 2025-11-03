package com.ilseon.ui.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ilseon.ui.theme.IlseonTheme

@Composable
fun DailyDashboardScreen() {
    Text(text = "Daily Dashboard Screen")
}

@Preview(showBackground = true)
@Composable
fun DailyDashboardScreenPreview() {
    IlseonTheme {
        DailyDashboardScreen()
    }
}
