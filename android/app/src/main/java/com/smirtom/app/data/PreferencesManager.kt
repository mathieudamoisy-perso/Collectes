package com.smirtom.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    private val reminderMinutesKey = intPreferencesKey("reminder_minutes_of_day")
    private val legacyReminderHourKey = intPreferencesKey("reminder_hour")
    private val communeSlugKey = stringPreferencesKey("commune_slug")
    private val calendarLogicVersionKey = intPreferencesKey("calendar_logic_version")

    val reminderTimeMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        resolveReminderTimeMinutes(prefs)
    }

    val selectedCommune: Flow<VexinCommune> = context.dataStore.data.map { prefs ->
        val slug = prefs[communeSlugKey] ?: VexinCommunes.default.slug
        VexinCommunes.bySlug(slug) ?: VexinCommunes.default
    }

    suspend fun setReminderTime(minutesOfDay: Int) {
        context.dataStore.edit { prefs ->
            prefs[reminderMinutesKey] = ReminderTime.coerce(minutesOfDay)
            prefs.remove(legacyReminderHourKey)
        }
    }

    suspend fun setCommune(commune: VexinCommune) {
        context.dataStore.edit { prefs ->
            prefs[communeSlugKey] = commune.slug
        }
    }

    suspend fun getReminderTimeMinutes(): Int = reminderTimeMinutes.first()

    suspend fun getSelectedCommune(): VexinCommune = selectedCommune.first()

    suspend fun getCalendarLogicVersion(): Int =
        context.dataStore.data.first()[calendarLogicVersionKey] ?: 0

    suspend fun setCalendarLogicVersion(version: Int) {
        context.dataStore.edit { prefs ->
            prefs[calendarLogicVersionKey] = version
        }
    }

    private fun resolveReminderTimeMinutes(prefs: Preferences): Int {
        prefs[reminderMinutesKey]?.let { return ReminderTime.coerce(it) }
        prefs[legacyReminderHourKey]?.let { hour ->
            return ReminderTime.coerce(hour * 60)
        }
        return ReminderTime.DEFAULT_MINUTES
    }

    companion object {
        const val CALENDAR_LOGIC_VERSION = 3
    }
}
