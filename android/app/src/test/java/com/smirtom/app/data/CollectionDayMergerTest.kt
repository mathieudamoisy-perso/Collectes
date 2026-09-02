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

    @Test
    fun keepsEmballagesAndVegetauxOnSameDate() {
        val merged = CollectionDayMerger.merge(
            listOf(
                CollectionDay(LocalDate.of(2026, 9, 15), listOf(WasteType.EMBALLAGES)),
                CollectionDay(LocalDate.of(2026, 9, 15), listOf(WasteType.VEGETAUX))
            )
        )
        assertEquals(
            listOf(WasteType.EMBALLAGES, WasteType.VEGETAUX),
            merged.single().wasteTypes
        )
    }

    @Test
    fun keepsOnlyOneRegularBinOnSameDate() {
        val merged = CollectionDayMerger.merge(
            listOf(
                CollectionDay(LocalDate.of(2026, 1, 6), listOf(WasteType.ORDURES)),
                CollectionDay(LocalDate.of(2026, 1, 6), listOf(WasteType.EMBALLAGES))
            )
        )
        assertEquals(listOf(WasteType.EMBALLAGES), merged.single().wasteTypes)
    }
}
