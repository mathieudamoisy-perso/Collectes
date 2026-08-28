package com.smirtom.app.data

object CollectionDayMerger {
    fun merge(days: List<CollectionDay>): List<CollectionDay> {
        return days
            .groupBy { it.date }
            .map { (date, group) ->
                CollectionDay(
                    date = date,
                    wasteTypes = collapseTypes(
                        group.flatMap { it.wasteTypes }
                    )
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Un seul bac régulier par jour (ordures, emballages ou verre).
     * Les encombrants peuvent coexister : ce n’est pas un bac hebdomadaire.
     */
    internal fun collapseTypes(types: List<WasteType>): List<WasteType> {
        val unique = types.distinct()
        val encombrants = unique.filter { it == WasteType.ENCOMBRANTS }
        val regular = unique.filter { it != WasteType.ENCOMBRANTS }
        val oneRegular = when {
            regular.size <= 1 -> regular
            WasteType.EMBALLAGES in regular -> listOf(WasteType.EMBALLAGES)
            WasteType.VERRE in regular -> listOf(WasteType.VERRE)
            else -> regular.take(1)
        }
        return (oneRegular + encombrants).sortedBy { it.ordinal }
    }
}
