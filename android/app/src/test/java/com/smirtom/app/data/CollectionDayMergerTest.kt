package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CollectionDayMergerTest {
    @Test
    fun mergesSameDateDifferentTypes() {
        val merged = CollectionDayMerger.merge(
            listOf(
                CollectionDay(LocalDate.of(2026, 5, 21), listOf(WasteType.ORDURES)),
                CollectionDay(LocalDate.of(2026, 5, 21), listOf(WasteType.ENCOMBRANTS))
            )
        )
        assertEquals(1, merged.size)
        assertEquals(
            listOf(WasteType.ORDURES, WasteType.ENCOMBRANTS),
            merged.first().wasteTypes
        )
    }
}
