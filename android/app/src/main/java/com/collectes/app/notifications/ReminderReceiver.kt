package com.collectes.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.collectes.app.data.CalendarRepository
import com.collectes.app.data.PreferencesManager
import com.collectes.app.data.WasteType
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
                val preferences = PreferencesManager(context)
                val enabledTypes = preferences.getEnabledReminderTypes()
                val repository = CalendarRepository(context)

                val collectionDate = intent.getStringExtra(EXTRA_COLLECTION_DATE)?.let(LocalDate::parse)
                    ?: LocalDate.now().plusDays(1)

                val extrasTypes = intent.getStringArrayExtra(EXTRA_WASTE_TYPES)
                    ?.mapNotNull { WasteType.fromStorage(it) }
                    .orEmpty()
                val calendarAvailable = repository.hasCachedCalendar()
                val dbTypesForDate = if (calendarAvailable) {
                    repository.getCollectionsOn(collectionDate)
                } else {
                    emptyList()
                }
                val dbHasAnyEvents = calendarAvailable && repository.hasAnyCachedEvents()

                val types = ReminderFireResolver.resolveTypes(
                    calendarAvailable = calendarAvailable,
                    dbHasAnyEvents = dbHasAnyEvents,
                    dbTypesForDate = dbTypesForDate,
                    extrasTypes = extrasTypes,
                    enabledTypes = enabledTypes
                )
                if (types.isEmpty()) return@launch

                val message = ReminderScheduler.formatReminderMessage(collectionDate, types)
                NotificationHelper.showReminder(
                    context = context,
                    notificationId = collectionDate.toEpochDay().toInt(),
                    wasteTypes = types,
                    message = message
                )
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
