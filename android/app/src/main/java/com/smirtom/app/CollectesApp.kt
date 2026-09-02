package com.smirtom.app

import android.app.Application
import com.smirtom.app.data.CalendarRepository
import com.smirtom.app.data.PreferencesManager
import com.smirtom.app.notifications.DailyCheckWorker
import com.smirtom.app.notifications.NotificationHelper
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
