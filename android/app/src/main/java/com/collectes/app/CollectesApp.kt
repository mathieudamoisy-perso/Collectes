package com.collectes.app

import android.app.Application
import com.collectes.app.data.CalendarRepository
import com.collectes.app.data.PreferencesManager
import com.collectes.app.notifications.DailyCheckWorker
import com.collectes.app.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CollectesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runBlocking(Dispatchers.IO) {
            PreferencesManager(this@CollectesApp).warmUpSelectedCommune()
            CalendarRepository(this@CollectesApp).warmUpHomeSnapshot()
        }
        NotificationHelper.createChannel(this)
        DailyCheckWorker.schedule(this)
    }
}
