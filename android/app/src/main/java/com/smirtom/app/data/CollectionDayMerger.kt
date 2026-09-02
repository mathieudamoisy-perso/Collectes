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
     * Les encombrants et végétaux peuvent coexister avec un bac régulier.
     */
    internal fun collapseTypes(types: List<WasteType>): List<WasteType> {
        val unique = types.distinct()
        val ancillary = unique.filter { it == WasteType.ENCOMBRANTS || it == WasteType.VEGETAUX }
        val regular = unique.filter { it != WasteType.ENCOMBRANTS && it != WasteType.VEGETAUX }
        val oneRegular = when {
            regular.size <= 1 -> regular
            WasteType.EMBALLAGES in regular -> listOf(WasteType.EMBALLAGES)
            WasteType.VERRE in regular -> listOf(WasteType.VERRE)
            else -> regular.take(1)
        }
        return (oneRegular + ancillary).sortedBy { it.ordinal }
    }
}
