package com.sighted.sleep.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sighted.sleep.MainActivity
import com.sighted.sleep.SightedSleepApp
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.ui.startTimeText

/**
 * One-tap home-screen widget (spec F1.2).
 * - Awake: tap starts sleep in the background (no app opens).
 * - Sleeping: tap opens the app, which ends the sleep and shows the morning check-in.
 */
class SleepWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as SightedSleepApp
        val initial = app.sleepRepository.getActive()
        provideContent {
            // Follow the database live: a Glance session stays open for a while after each update,
            // and a value read once before provideContent would go stale (shows "Wake up" after waking).
            val active by app.sleepRepository.activeSession.collectAsState(initial = initial)
            GlanceTheme { WidgetContent(active) }
        }
    }
}

@Composable
private fun WidgetContent(active: SleepSession?) {
    val context = LocalContext.current
    val action = if (active == null) {
        actionRunCallback<StartSleepAction>()
    } else {
        actionStartActivity(
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_WAKE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
    val container = if (active == null) GlanceTheme.colors.secondaryContainer else GlanceTheme.colors.primaryContainer
    val onContainer = if (active == null) GlanceTheme.colors.onSecondaryContainer else GlanceTheme.colors.onPrimaryContainer

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(container)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (active == null) "Go to sleep" else "Wake up",
            style = TextStyle(color = onContainer, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = if (active == null) "Tap to start" else "Sleeping since ${active.startTimeText()}",
            style = TextStyle(color = onContainer, fontSize = 13.sp),
        )
    }
}

class StartSleepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as SightedSleepApp
        // The repository refreshes all widgets after the change.
        app.sleepRepository.startSleep()
    }
}

class SleepWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SleepWidget()
}
