package dna.familytree.feature.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Receives the alarm of [ReminderScheduler]. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_ALARM) return
        try {
            ReminderScheduler.onAlarm(context)
        } catch (e: Exception) {
            Log.w("ReminderReceiver", "Reminders alarm failed", e)
        }
    }
}
