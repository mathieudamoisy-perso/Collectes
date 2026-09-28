package com.collectes.app.notifications

import com.collectes.app.data.WasteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderFireResolverTest {
    private val allEnabled = WasteType.entries.toSet()

    @Test
    fun calendarWithEventsTrustsEmptyDateAndSuppressesOrphanExtras() {
        val types = ReminderFireResolver.resolveTypes(
            calendarAvailable = true,
            dbHasAnyEvents = true,
            dbTypesForDate = emptyList(),
            extrasTypes = listOf(WasteType.EMBALLAGES),
            enabledTypes = allEnabled
        )
        assertTrue(types.isEmpty())
    }

    @Test
    fun calendarWithEventsUsesDbTypes() {
        val types = ReminderFireResolver.resolveTypes(
            calendarAvailable = true,
            dbHasAnyEvents = true,
            dbTypesForDate = listOf(WasteType.ORDURES),
            extrasTypes = listOf(WasteType.EMBALLAGES),
            enabledTypes = allEnabled
        )
        assertEquals(listOf(WasteType.ORDURES), types)
    }

    @Test
    fun wipedCalendarFallsBackToExtras() {
        val types = ReminderFireResolver.resolveTypes(
            calendarAvailable = true,
            dbHasAnyEvents = false,
            dbTypesForDate = emptyList(),
            extrasTypes = listOf(WasteType.EMBALLAGES),
            enabledTypes = allEnabled
        )
        assertEquals(listOf(WasteType.EMBALLAGES), types)
    }

    @Test
    fun missingCalendarFallsBackToExtras() {
        val types = ReminderFireResolver.resolveTypes(
            calendarAvailable = false,
            dbHasAnyEvents = false,
            dbTypesForDate = emptyList(),
            extrasTypes = listOf(WasteType.VERRE),
            enabledTypes = allEnabled
        )
        assertEquals(listOf(WasteType.VERRE), types)
    }

    @Test
    fun respectsEnabledTypesOnFallback() {
        val types = ReminderFireResolver.resolveTypes(
            calendarAvailable = false,
            dbHasAnyEvents = false,
            dbTypesForDate = emptyList(),
            extrasTypes = listOf(WasteType.EMBALLAGES, WasteType.VERRE),
            enabledTypes = setOf(WasteType.EMBALLAGES)
        )
        assertEquals(listOf(WasteType.EMBALLAGES), types)
    }
}
