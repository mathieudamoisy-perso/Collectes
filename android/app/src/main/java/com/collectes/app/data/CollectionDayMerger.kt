package com.collectes.app.data

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
     * Ordures peuvent coexister avec emballages/verre (ex. Cabourg, CCVT).
     * Emballages et verre restent exclusifs (alternance SMIRTOM).
     * Encombrants / végétaux peuvent coexister avec un bac régulier.
     */
    internal fun collapseTypes(types: List<WasteType>): List<WasteType> {
        val unique = types.distinct()
        val ancillary = unique.filter { it == WasteType.ENCOMBRANTS || it == WasteType.VEGETAUX }
        val recycling = when {
            WasteType.EMBALLAGES in unique -> listOf(WasteType.EMBALLAGES)
            WasteType.VERRE in unique -> listOf(WasteType.VERRE)
            else -> emptyList()
        }
        val regular = buildList {
            if (WasteType.ORDURES in unique) add(WasteType.ORDURES)
            addAll(recycling)
        }
        return (regular + ancillary).sortedBy { it.ordinal }
    }
}
