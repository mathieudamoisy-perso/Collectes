package com.smirtom.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smirtom.app.data.CalendarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                NotificationHelper.createChannel(context)

                val wasteTypes = NotificationHelper.wasteTypesFromExtras(
                    intent.getStringArrayExtra(EXTRA_WASTE_TYPES)
                )
                val collectionDate = intent.getStringExtra(EXTRA_COLLECTION_DATE)?.let(LocalDate::parse)

                if (wasteTypes.isNotEmpty() && collectionDate != null) {
                    val repository = CalendarRepository(context)
                    val scheduledTypes = repository.getCollectionsOn(collectionDate)
                    if (scheduledTypes.toSet() != wasteTypes.toSet()) {
                        return@launch
                    }
                    val message = ReminderScheduler.formatReminderMessage(collectionDate, wasteTypes)
                    NotificationHelper.showReminder(
                        context = context,
                        notificationId = collectionDate.toEpochDay().toInt(),
                        wasteTypes = wasteTypes,
                        message = message
                    )
                } else {
                    val repository = CalendarRepository(context)
                    val tomorrowTypes = repository.getTomorrowCollections()
                    if (tomorrowTypes.isNotEmpty()) {
                        val tomorrow = LocalDate.now().plusDays(1)
                        val message = ReminderScheduler.formatReminderMessage(tomorrow, tomorrowTypes)
                        NotificationHelper.showReminder(
                            context = context,
                            notificationId = tomorrow.toEpochDay().toInt(),
                            wasteTypes = tomorrowTypes,
                            message = message
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_WASTE_TYPES = "extra_waste_types"
        const val EXTRA_COLLECTION_DATE = "extra_collection_date"
    }
}
