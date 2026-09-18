package com.collectes.app.data

object ReminderTypeFilter {
    fun filterEvents(
        events: List<CollectionDay>,
        enabledTypes: Set<WasteType>
    ): List<CollectionDay> {
        if (enabledTypes.isEmpty()) return emptyList()
        if (enabledTypes.size == WasteType.entries.size) return events
        return events.mapNotNull { day ->
            val kept = day.wasteTypes.filter { it in enabledTypes }
            if (kept.isEmpty()) null else day.copy(wasteTypes = kept)
        }
    }

    fun filterTypes(
        types: List<WasteType>,
        enabledTypes: Set<WasteType>
    ): List<WasteType> = types.filter { it in enabledTypes }
}
