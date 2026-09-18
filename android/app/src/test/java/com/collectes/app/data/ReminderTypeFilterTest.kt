package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderTypeFilterTest {
    private val day = LocalDate.of(2026, 9, 20)

    @Test
    fun keepsAllWhenEveryTypeEnabled() {
        val events = listOf(
            CollectionDay(day, listOf(WasteType.ORDURES, WasteType.VERRE))
        )
        val filtered = ReminderTypeFilter.filterEvents(events, WasteType.entries.toSet())
        assertEquals(events, filtered)
    }

    @Test
    fun dropsDisabledTypesFromMixedDay() {
        val events = listOf(
            CollectionDay(day, listOf(WasteType.ORDURES, WasteType.VERRE, WasteType.EMBALLAGES))
        )
        val filtered = ReminderTypeFilter.filterEvents(
            events,
            setOf(WasteType.ORDURES, WasteType.EMBALLAGES)
        )
        assertEquals(
            listOf(CollectionDay(day, listOf(WasteType.ORDURES, WasteType.EMBALLAGES))),
            filtered
        )
    }

    @Test
    fun dropsDayWhenNoEnabledTypeRemains() {
        val events = listOf(
            CollectionDay(day, listOf(WasteType.VERRE)),
            CollectionDay(day.plusDays(1), listOf(WasteType.ORDURES))
        )
        val filtered = ReminderTypeFilter.filterEvents(events, setOf(WasteType.ORDURES))
        assertEquals(
            listOf(CollectionDay(day.plusDays(1), listOf(WasteType.ORDURES))),
            filtered
        )
    }

    @Test
    fun emptyEnabledSetYieldsNoEvents() {
        val events = listOf(CollectionDay(day, listOf(WasteType.ORDURES)))
        assertTrue(ReminderTypeFilter.filterEvents(events, emptySet()).isEmpty())
    }

    @Test
    fun filterTypesKeepsEnabledOnly() {
        val types = listOf(WasteType.ORDURES, WasteType.VERRE)
        assertEquals(
            listOf(WasteType.VERRE),
            ReminderTypeFilter.filterTypes(types, setOf(WasteType.VERRE))
        )
    }
}
