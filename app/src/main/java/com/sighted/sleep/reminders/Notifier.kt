package com.sighted.sleep.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sighted.sleep.R

/** Posts reminder notifications (spec F6). Silently does nothing without notification permission. */
object Notifier {
    private const val CHANNEL = "reminders"
    const val ID_BEDTIME = 1
    const val ID_MORNING = 2
    const val ID_STILL_SLEEPING = 3

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Bedtime, morning check-in and \"Still sleeping?\" reminders"
                },
            )
        }
    }

    fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun post(
        context: Context,
        id: Int,
        title: String,
        text: String,
        tap: Intent,
        actions: List<Pair<String, Intent>> = emptyList(),
    ) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(activity(context, id * 10, tap))
        actions.forEachIndexed { i, (label, intent) -> builder.addAction(0, label, activity(context, id * 10 + i + 1, intent)) }
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post: nothing to do.
        }
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun activity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
