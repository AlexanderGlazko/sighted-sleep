package com.sighted.sleep

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableLongStateOf
import com.sighted.sleep.ui.AppNavigation
import com.sighted.sleep.ui.theme.SightedSleepTheme

class MainActivity : ComponentActivity() {

    /** Bumped each time the widget asks to end the current sleep. */
    private val wakeRequest = mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only handle the launch intent on a fresh start, not after rotation/recreation.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            SightedSleepTheme {
                AppNavigation(wakeRequest = wakeRequest.longValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_WAKE) {
            wakeRequest.longValue = System.nanoTime()
            intent.action = null
        }
    }

    companion object {
        const val ACTION_WAKE = "com.sighted.sleep.action.WAKE"
    }
}
