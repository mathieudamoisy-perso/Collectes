package com.smirtom.app

import android.app.Application
import com.smirtom.app.notifications.DailyCheckWorker
import com.smirtom.app.notifications.NotificationHelper

class SmirtomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        DailyCheckWorker.schedule(this)
    }
}
