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
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val communeSlugKey = stringPreferencesKey("commune_slug")

    val reminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[reminderHourKey] ?: DEFAULT_REMINDER_HOUR)
            .coerceIn(MIN_REMINDER_HOUR, MAX_REMINDER_HOUR)
    }

    val selectedCommune: Flow<VexinCommune> = context.dataStore.data.map { prefs ->
        val slug = prefs[communeSlugKey] ?: VexinCommunes.default.slug
        VexinCommunes.bySlug(slug) ?: VexinCommunes.default
    }

    suspend fun setReminderHour(hour: Int) {
        context.dataStore.edit { prefs ->
            prefs[reminderHourKey] = hour.coerceIn(MIN_REMINDER_HOUR, MAX_REMINDER_HOUR)
        }
    }

    suspend fun setCommune(commune: VexinCommune) {
        context.dataStore.edit { prefs ->
            prefs[communeSlugKey] = commune.slug
        }
    }

    suspend fun getReminderHour(): Int = reminderHour.first()

    suspend fun getSelectedCommune(): VexinCommune = selectedCommune.first()

    companion object {
        const val DEFAULT_REMINDER_HOUR = 9
        const val MIN_REMINDER_HOUR = 6
        const val MAX_REMINDER_HOUR = 12
    }
}
