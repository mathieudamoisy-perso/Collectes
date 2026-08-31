package com.smirtom.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smirtom.app.data.CalendarRepository
import com.smirtom.app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                NotificationHelper.createChannel(context)
                DailyCheckWorker.schedule(context)
                val repository = CalendarRepository(context)
                repository.ensureCalendarSynced(force = false)
                val reminderTimeMinutes = PreferencesManager(context).reminderTimeMinutes.first()
                repository.rescheduleReminders(reminderTimeMinutes)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
