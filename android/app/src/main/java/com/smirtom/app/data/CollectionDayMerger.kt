package com.smirtom.app.data

object CollectionDayMerger {
    fun merge(days: List<CollectionDay>): List<CollectionDay> {
        return days
            .groupBy { it.date }
            .map { (date, group) ->
                CollectionDay(
                    date = date,
                    wasteTypes = group
                        .flatMap { it.wasteTypes }
                        .distinct()
                        .sortedBy { it.ordinal }
                )
            }
            .sortedBy { it.date }
    }
}
