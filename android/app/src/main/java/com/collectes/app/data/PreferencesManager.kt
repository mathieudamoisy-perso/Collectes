package com.collectes.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val useBrandColorsKey = booleanPreferencesKey("use_brand_colors")
    private val reminderTypesKey = stringPreferencesKey("reminder_enabled_types")

    val reminderTimeMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        resolveReminderTimeMinutes(prefs)
    }

    val selectedCommune: Flow<VexinCommune> = context.dataStore.data.map { prefs ->
        resolveCommune(prefs)
    }

    /** True dès qu'une commune a été choisie explicitement (manuel ou géoloc). */
    val hasCompletedCommuneSetup: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs.contains(communeSlugKey)
    }

    val useBrandColors: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[useBrandColorsKey] ?: true
    }

    val enabledReminderTypes: Flow<Set<WasteType>> = context.dataStore.data.map { prefs ->
        resolveEnabledReminderTypes(prefs)
    }

    suspend fun warmUpSelectedCommune() {
        cachedSelectedCommune = getSelectedCommune()
    }

    fun peekSelectedCommune(): VexinCommune = cachedSelectedCommune ?: VexinCommunes.default

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
        cachedSelectedCommune = commune
    }

    suspend fun hasCompletedCommuneSetup(): Boolean = hasCompletedCommuneSetup.first()

    suspend fun setUseBrandColors(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[useBrandColorsKey] = enabled
        }
    }

    suspend fun setReminderTypeEnabled(type: WasteType, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = resolveEnabledReminderTypes(prefs).toMutableSet()
            if (enabled) current.add(type) else current.remove(type)
            prefs[reminderTypesKey] = encodeReminderTypes(current)
        }
    }

    suspend fun getReminderTimeMinutes(): Int = reminderTimeMinutes.first()

    suspend fun getSelectedCommune(): VexinCommune = selectedCommune.first()

    suspend fun getUseBrandColors(): Boolean = useBrandColors.first()

    suspend fun getEnabledReminderTypes(): Set<WasteType> = enabledReminderTypes.first()

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

    private fun resolveCommune(prefs: Preferences): VexinCommune {
        val slug = VexinCommunes.normalizeSlug(
            prefs[communeSlugKey] ?: VexinCommunes.default.slug
        )
        val commune = VexinCommunes.bySlug(slug) ?: VexinCommunes.default
        cachedSelectedCommune = commune
        return commune
    }

    private fun resolveEnabledReminderTypes(prefs: Preferences): Set<WasteType> {
        val raw = prefs[reminderTypesKey] ?: return WasteType.entries.toSet()
        return decodeReminderTypes(raw)
    }

    companion object {
        const val CALENDAR_LOGIC_VERSION = 21

        @Volatile
        var cachedSelectedCommune: VexinCommune? = null
            private set

        fun encodeReminderTypes(types: Set<WasteType>): String =
            types.sortedBy { it.ordinal }.joinToString(",") { it.name }

        fun decodeReminderTypes(raw: String): Set<WasteType> {
            if (raw.isBlank()) return emptySet()
            return raw.split(",")
                .mapNotNull { WasteType.fromStorage(it.trim()) }
                .toSet()
        }
    }
}
