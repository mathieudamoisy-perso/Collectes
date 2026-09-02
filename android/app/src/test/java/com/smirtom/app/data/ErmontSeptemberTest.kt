package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ErmontSeptemberTest {
    @Test
    fun ermontSeptember2026MatchesOfficialCalendar() {
        val text = loadFixture("ermont.txt")
        val rules = MunicipalCalendarParser.parseIfPresent(text, 2026)!!
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)

        assertEquals(
            listOf(WasteType.EMBALLAGES),
            events.single { it.date == LocalDate.of(2026, 9, 3) }.wasteTypes
        )
        assertEquals(
            listOf(WasteType.VEGETAUX),
            events.single { it.date == LocalDate.of(2026, 9, 7) }.wasteTypes
        )
        assertEquals(
            listOf(WasteType.ORDURES),
            events.single { it.date == LocalDate.of(2026, 9, 8) }.wasteTypes
        )
        assertEquals(
            listOf(WasteType.ENCOMBRANTS),
            events.single { it.date == LocalDate.of(2026, 9, 9) }.wasteTypes
        )
        assertEquals(
            listOf(WasteType.EMBALLAGES),
            events.single { it.date == LocalDate.of(2026, 9, 10) }.wasteTypes
        )
    }

    private fun loadFixture(name: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("calendars/$name")) {
            "Missing test fixture calendars/$name"
        }
        return stream.bufferedReader().readText()
    }
}
