package com.sighted.sleep.reminders

import android.content.Context
import android.content.Intent
import com.sighted.sleep.MainActivity

/**
 * Something the widget or a notification asks the app to do when it opens.
 * [token] is unique per request so each one is handled exactly once.
 */
data class AppRequest(val kind: Kind, val sessionId: Long? = null, val token: Long = System.nanoTime()) {
    enum class Kind { WAKE, CHECK_IN, EDIT }

    companion object {
        const val ACTION_WAKE = "com.sighted.sleep.action.WAKE"
        const val ACTION_CHECK_IN = "com.sighted.sleep.action.CHECK_IN"
        const val ACTION_EDIT = "com.sighted.sleep.action.EDIT"
        const val EXTRA_SESSION_ID = "session_id"

        fun intent(context: Context, action: String, sessionId: Long? = null): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .apply { sessionId?.let { putExtra(EXTRA_SESSION_ID, it) } }

        fun from(intent: Intent?): AppRequest? {
            val id = intent?.getLongExtra(EXTRA_SESSION_ID, -1L)?.takeIf { it >= 0 }
            return when (intent?.action) {
                ACTION_WAKE -> AppRequest(Kind.WAKE)
                ACTION_CHECK_IN -> id?.let { AppRequest(Kind.CHECK_IN, it) }
                ACTION_EDIT -> id?.let { AppRequest(Kind.EDIT, it) }
                else -> null
            }
        }
    }
}
