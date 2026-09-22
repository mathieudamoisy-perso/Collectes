package com.collectes.app

import android.app.Application
import com.collectes.app.data.CalendarRepository
import com.collectes.app.data.PreferencesManager
import com.collectes.app.notifications.DailyCheckWorker
import com.collectes.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CollectesApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        DailyCheckWorker.schedule(this)
        appScope.launch {
            PreferencesManager(this@CollectesApp).warmUpSelectedCommune()
            CalendarRepository(this@CollectesApp).warmUpHomeSnapshot()
            val reminderTimeMinutes =
                PreferencesManager(this@CollectesApp).reminderTimeMinutes.first()
            CalendarRepository(this@CollectesApp).rescheduleReminders(reminderTimeMinutes)
        }
    }
}
