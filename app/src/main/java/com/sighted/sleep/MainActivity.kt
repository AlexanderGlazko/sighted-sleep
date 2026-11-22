package com.sighted.sleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sighted.sleep.ui.AppNavigation
import com.sighted.sleep.ui.theme.SightedSleepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SightedSleepTheme {
                AppNavigation()
            }
        }
    }
}
