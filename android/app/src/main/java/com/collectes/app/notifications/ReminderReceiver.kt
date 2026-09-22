package com.collectes.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.collectes.app.data.CalendarRepository
import com.collectes.app.data.PreferencesManager
import com.collectes.app.data.ReminderTypeFilter
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

                val fromExtras = intent.getStringArrayExtra(EXTRA_WASTE_TYPES)
                    ?.mapNotNull { WasteType.fromStorage(it) }
                    .orEmpty()
                val rawTypes = fromExtras.ifEmpty {
                    repository.getCollectionsOn(collectionDate)
                }
                val types = ReminderTypeFilter.filterTypes(rawTypes, enabledTypes)
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
