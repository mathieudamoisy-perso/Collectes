package com.collectes.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.collectes.app.data.CalendarRepository
import com.collectes.app.data.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class DailyCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.createChannel(applicationContext)
        val repository = CalendarRepository(applicationContext)
        val preferences = PreferencesManager(applicationContext)

        repository.ensureCalendarSynced(force = false)

        val reminderTimeMinutes = preferences.reminderTimeMinutes.first()
        repository.rescheduleReminders(reminderTimeMinutes)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "collectes_daily_check"
        private const val LEGACY_WORK_NAME = "smirtom_daily_check"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(LEGACY_WORK_NAME)
            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
