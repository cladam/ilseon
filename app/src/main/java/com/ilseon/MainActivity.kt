package com.ilseon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ilseon.ui.screen.MainScreen
import com.ilseon.ui.theme.IlseonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IlseonTheme {
                MainScreen()
            }
        }
    }
}
