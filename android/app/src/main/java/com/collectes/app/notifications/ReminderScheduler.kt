package com.collectes.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.collectes.app.data.CollectionDay
import com.collectes.app.data.ReminderTypeFilter
import com.collectes.app.data.WasteType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val zoneId = ZoneId.of("Europe/Paris")
    private val schedulePrefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun scheduleUpcomingReminders(
        events: List<CollectionDay>,
        reminderTimeMinutes: Int,
        enabledTypes: Set<WasteType> = WasteType.entries.toSet()
    ) {
        val filtered = ReminderTypeFilter.filterEvents(events, enabledTypes)
        val now = LocalDateTime.now(zoneId)
        val today = LocalDate.now(zoneId)
        val horizon = today.plusDays(SCHEDULE_HORIZON_DAYS)
        val hour = reminderTimeMinutes / 60
        val minute = reminderTimeMinutes % 60

        val toSchedule = filtered.filter { event ->
            val reminderDateTime = event.date.minusDays(1).atTime(hour, minute)
            reminderDateTime.isAfter(now) && !event.date.isAfter(horizon)
        }
        val fingerprint = buildFingerprint(toSchedule, reminderTimeMinutes, enabledTypes)
        if (fingerprint == schedulePrefs.getString(KEY_FINGERPRINT, null)) return

        filtered.forEach { cancelReminder(it) }
        toSchedule.forEach { event ->
            val reminderDateTime = event.date.minusDays(1).atTime(hour, minute)
            scheduleReminder(event, reminderDateTime)
        }
        schedulePrefs.edit().putString(KEY_FINGERPRINT, fingerprint).apply()
    }

    private fun cancelReminder(event: CollectionDay) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = event.date.toEpochDay().toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
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
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "reminder_schedule"
        private const val KEY_FINGERPRINT = "fingerprint"
        const val SCHEDULE_HORIZON_DAYS = 30L

        fun formatReminderMessage(collectionDate: LocalDate, wasteTypes: List<WasteType>): String {
            val formattedDate = collectionDate.format(
                DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
            )
            val bins = wasteTypes.joinToString(" + ") { it.notificationLabel }
            return "Demain ($formattedDate) : sortir $bins"
        }

        private fun buildFingerprint(
            events: List<CollectionDay>,
            reminderTimeMinutes: Int,
            enabledTypes: Set<WasteType>
        ): String {
            val typesKey = enabledTypes.sortedBy { it.ordinal }.joinToString(",") { it.name }
            val eventsKey = events.joinToString(";") { event ->
                "${event.date.toEpochDay()}:${event.wasteTypes.sortedBy { it.ordinal }.joinToString(",") { it.name }}"
            }
            return "$reminderTimeMinutes|$typesKey|$eventsKey"
        }
    }
}
