package com.smirtom.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    private val reminderHourKey = intPreferencesKey("reminder_hour")

    val reminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[reminderHourKey] ?: DEFAULT_REMINDER_HOUR
    }

    suspend fun setReminderHour(hour: Int) {
        context.dataStore.edit { prefs ->
            prefs[reminderHourKey] = hour.coerceIn(0, 23)
        }
    }

    suspend fun getReminderHour(): Int = reminderHour.first()

    companion object {
        const val DEFAULT_REMINDER_HOUR = 19
    }
}
