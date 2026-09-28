package com.collectes.app.notifications

import com.collectes.app.data.WasteType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReminderSchedulerTest {
    @Test
    fun formatReminderMessageForSingleType() {
        val message = ReminderScheduler.formatReminderMessage(
            LocalDate.of(2026, 9, 1),
            listOf(WasteType.EMBALLAGES)
        )
        assertEquals(
            "Demain (mardi 1 septembre) : sortir Emballages / papiers (jaune)",
            message
        )
    }

    @Test
    fun formatReminderMessageForMultipleTypes() {
        val message = ReminderScheduler.formatReminderMessage(
            LocalDate.of(2026, 9, 1),
            listOf(WasteType.ORDURES, WasteType.VERRE)
        )
        assertEquals(
            "Demain (mardi 1 septembre) : sortir Ordures ménagères (gris) + Verre (vert)",
            message
        )
    }

    @Test
    fun formatReminderMessageUsesFilteredTypesOnly() {
        val message = ReminderScheduler.formatReminderMessage(
            LocalDate.of(2026, 9, 1),
            listOf(WasteType.ORDURES)
        )
        assertEquals(
            "Demain (mardi 1 septembre) : sortir Ordures ménagères (gris)",
            message
        )
    }

    @Test
    fun encodeDecodeScheduledEpochDaysRoundTrip() {
        val days = setOf(20_000L, 20_014L, 20_007L)
        val encoded = ReminderScheduler.encodeEpochDays(days)
        assertEquals("20000,20007,20014", encoded)
        assertEquals(days, ReminderScheduler.decodeEpochDays(encoded))
    }
}
