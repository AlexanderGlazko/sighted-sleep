package com.sighted.sleep

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.sighted.sleep.reminders.AppRequest
import com.sighted.sleep.ui.AppNavigation
import com.sighted.sleep.ui.theme.SightedSleepTheme

class MainActivity : ComponentActivity() {

    /** Latest request from the widget or a notification (wake, check-in, edit). */
    private val request = mutableStateOf<AppRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only handle the launch intent on a fresh start, not after rotation/recreation.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            SightedSleepTheme {
                AppNavigation(request = request.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        AppRequest.from(intent)?.let {
            request.value = it
            intent?.action = null
        }
    }
}
