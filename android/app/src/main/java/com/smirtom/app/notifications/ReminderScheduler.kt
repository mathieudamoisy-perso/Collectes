package com.smirtom.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.smirtom.app.data.CollectionDay
import com.smirtom.app.data.WasteType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val zoneId = ZoneId.of("Europe/Paris")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    fun scheduleUpcomingReminders(events: List<CollectionDay>, reminderHour: Int) {
        cancelAllReminders()
        val now = LocalDateTime.now(zoneId)

        events.forEach { event ->
            val reminderDateTime = event.date.minusDays(1).atTime(reminderHour, 0)
            if (reminderDateTime.isAfter(now)) {
                scheduleReminder(event, reminderDateTime)
            }
        }
    }

    private fun scheduleReminder(event: CollectionDay, reminderDateTime: LocalDateTime) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_WASTE_TYPES, event.wasteTypes.map { it.name }.toTypedArray())
            putExtra(ReminderReceiver.EXTRA_COLLECTION_DATE, event.date.toString())
        }

        val requestCode = event.date.toEpochDay().toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = reminderDateTime.atZone(zoneId).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAllReminders() {
        // Android ne permet pas d'annuler toutes les alarmes sans les PendingIntent d'origine.
        // Les alarmes passées sont ignorées par le receiver via la date.
    }

    companion object {
        fun formatReminderMessage(collectionDate: LocalDate, wasteTypes: List<WasteType>): String {
            val formattedDate = collectionDate.format(
                DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
            )
            val bins = wasteTypes.joinToString(" + ") { "${it.label} (${it.colorName})" }
            return "Demain ($formattedDate) : sortir $bins"
        }
    }
}
