package com.smirtom.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UpcomingEventsFilterTest {
    @Test
    fun upcomingListExcludesTodayAndPastDates() {
        val today = LocalDate.of(2026, 9, 1)
        val events = listOf(
            CollectionDay(LocalDate.of(2026, 8, 30), listOf(WasteType.ORDURES)),
            CollectionDay(LocalDate.of(2026, 8, 31), listOf(WasteType.EMBALLAGES)),
            CollectionDay(today, listOf(WasteType.VERRE)),
            CollectionDay(LocalDate.of(2026, 9, 2), listOf(WasteType.ORDURES)),
            CollectionDay(LocalDate.of(2026, 9, 8), listOf(WasteType.EMBALLAGES))
        )

        val upcoming = events.filter { it.date.isAfter(today) }

        assertFalse(upcoming.any { !it.date.isAfter(today) })
        assertTrue(upcoming.all { it.date.isAfter(today) })
        assertTrue(upcoming.any { it.date == LocalDate.of(2026, 9, 2) })
    }

    @Test
    fun cormeillesScheduleUsesThursdayMondayTuesday() {
        val rules = OfficialCommuneSchedules.rules(2026, "cormeilles-en-vexin")
        assertTrue(rules.orduresDay.name == "THURSDAY")
        assertTrue(rules.emballagesDay.name == "MONDAY")
        assertTrue(rules.verreDay.name == "TUESDAY")
    }

    @Test
    fun sannoisScheduleUsesThursdayTuesdayMonday() {
        val rules = OfficialCommuneSchedules.rules(2026, "sannois")
        assertTrue(rules.orduresDay.name == "THURSDAY")
        assertTrue(rules.emballagesDay.name == "TUESDAY")
        assertTrue(rules.verreDay.name == "MONDAY")
    }

    @Test
    fun ermontEaubonneScheduleUsesTuesdayThursdayFriday() {
        val rules = OfficialCommuneSchedules.rules(2026, "ermont-eaubonne")
        assertTrue(rules.orduresDay.name == "TUESDAY")
        assertTrue(rules.emballagesDay.name == "THURSDAY")
        assertTrue(rules.verreDay.name == "FRIDAY")
    }
}
