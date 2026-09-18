package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SyncStatusFormatterTest {
    @Test
    fun formatsLastSyncLabelWithParisDate() {
        val instant = LocalDate.of(2026, 9, 18)
            .atStartOfDay(ZoneId.of("Europe/Paris"))
            .toInstant()
        val text = SyncStatusFormatter.format(instant)
        assertTrue(text.startsWith("Dernière synchro : "))
        assertTrue(text.contains("18"))
        assertTrue(text.contains("2026"))
    }

    @Test
    fun encodeDecodeReminderTypesRoundTrip() {
        val types = setOf(WasteType.VERRE, WasteType.ORDURES)
        val encoded = PreferencesManager.encodeReminderTypes(types)
        assertEquals("ORDURES,VERRE", encoded)
        assertEquals(types, PreferencesManager.decodeReminderTypes(encoded))
    }

    @Test
    fun decodeBlankMeansNoneEnabled() {
        assertTrue(PreferencesManager.decodeReminderTypes("").isEmpty())
        assertTrue(PreferencesManager.decodeReminderTypes("   ").isEmpty())
    }

    @Test
    fun decodeIgnoresUnknownTokens() {
        assertEquals(
            setOf(WasteType.EMBALLAGES),
            PreferencesManager.decodeReminderTypes("EMBALLAGES,UNKNOWN,EMBALLAGES")
        )
    }

    @Test
    fun formatUsesDerniereSynchroPrefix() {
        val text = SyncStatusFormatter.format(Instant.parse("2026-03-01T12:00:00Z"))
        assertTrue(text.startsWith("Dernière synchro : "))
    }
}
