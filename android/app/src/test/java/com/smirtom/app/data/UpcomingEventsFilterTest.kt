package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class UpcomingEventsFilterTest {
    @Test
    fun upcomingListIncludesTodayAndExcludesPastDates() {
        val today = LocalDate.of(2026, 9, 1)
        val events = listOf(
            CollectionDay(LocalDate.of(2026, 8, 30), listOf(WasteType.ORDURES)),
            CollectionDay(LocalDate.of(2026, 8, 31), listOf(WasteType.EMBALLAGES)),
            CollectionDay(today, listOf(WasteType.VERRE)),
            CollectionDay(LocalDate.of(2026, 9, 2), listOf(WasteType.ORDURES)),
            CollectionDay(LocalDate.of(2026, 9, 8), listOf(WasteType.EMBALLAGES))
        )

        val upcoming = events.filter { !it.date.isBefore(today) }

        assertFalse(upcoming.any { it.date.isBefore(today) })
        assertTrue(upcoming.any { it.date == today })
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
    fun sannoisScheduleUsesMonthlyVerre() {
        val rules = OfficialCommuneSchedules.rules(2026, "sannois")
        assertEquals(CollectionRecurrence.MONTHLY_NTH_WEEKDAY, rules.verreRecurrence)
        assertEquals(DayOfWeek.MONDAY, rules.verreDay)
        assertEquals(2, rules.verreMonthOrdinal)
    }

    @Test
    fun ermontEaubonneScheduleUsesMonthlyVerre() {
        val rules = OfficialCommuneSchedules.rules(2026, "ermont-eaubonne")
        assertEquals(CollectionRecurrence.MONTHLY_NTH_WEEKDAY, rules.verreRecurrence)
        assertEquals(DayOfWeek.FRIDAY, rules.verreDay)
        assertEquals(4, rules.verreMonthOrdinal)
    }
}
