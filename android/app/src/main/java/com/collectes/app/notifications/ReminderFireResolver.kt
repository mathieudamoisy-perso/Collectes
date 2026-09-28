package com.collectes.app.notifications

import com.collectes.app.data.ReminderTypeFilter
import com.collectes.app.data.WasteType

/**
 * Décide quels types notifier au déclenchement d’une alarme.
 * - Calendrier local exploitable → source de vérité (coupe les notifs orphelines).
 * - Calendrier absent / vidé → extras planifiés (évite de perdre une notif légitime).
 */
object ReminderFireResolver {
    fun resolveTypes(
        calendarAvailable: Boolean,
        dbHasAnyEvents: Boolean,
        dbTypesForDate: List<WasteType>,
        extrasTypes: List<WasteType>,
        enabledTypes: Set<WasteType>
    ): List<WasteType> {
        val raw = when {
            calendarAvailable && dbHasAnyEvents -> dbTypesForDate
            extrasTypes.isNotEmpty() -> extrasTypes
            else -> dbTypesForDate
        }
        return ReminderTypeFilter.filterTypes(raw, enabledTypes)
    }
}
