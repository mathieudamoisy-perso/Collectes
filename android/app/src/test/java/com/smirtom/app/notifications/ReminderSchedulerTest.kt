package com.smirtom.app.notifications

import com.smirtom.app.data.WasteType
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
}
