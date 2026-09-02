package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MunicipalCalendarParserTest {
    @Test
    fun parsesSannoisPavillonsScheduleFromPdf() {
        val text = loadFixture("sannois.txt")
        val rules = MunicipalCalendarParser.parseIfPresent(text, 2026)!!

        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(CollectionRecurrence.MONTHLY_NTH_WEEKDAY, rules.verreRecurrence)
        assertEquals(DayOfWeek.MONDAY, rules.verreDay)
        assertEquals(2, rules.verreMonthOrdinal)
        assertEquals(DayOfWeek.WEDNESDAY, rules.encombrantsDay)
        assertEquals(1, rules.encombrantsMonthOrdinal)
        assertEquals(DayOfWeek.TUESDAY, rules.vegetauxSchedule?.dayOfWeek)
    }

    @Test
    fun sannoisSeptember8HasEmballagesAndVegetauxForPavillons() {
        val rules = MunicipalCalendarParser.parseIfPresent(loadFixture("sannois.txt"), 2026)!!
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val sept8 = events.single { it.date == LocalDate.of(2026, 9, 8) }

        assertEquals(CollectionRecurrence.WEEKLY, rules.emballagesRecurrence)
        assertEquals(
            listOf(WasteType.EMBALLAGES, WasteType.VEGETAUX),
            sept8.wasteTypes
        )
    }

    @Test
    fun sannoisSeptember15HasEmballagesAndVegetauxForPavillons() {
        val rules = MunicipalCalendarParser.parseIfPresent(loadFixture("sannois.txt"), 2026)!!
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val sept15 = events.single { it.date == LocalDate.of(2026, 9, 15) }

        assertEquals(
            listOf(WasteType.EMBALLAGES, WasteType.VEGETAUX),
            sept15.wasteTypes
        )
    }

    @Test
    fun sannoisFebruaryVegetauxOnlyOnSpecialTuesday() {
        val rules = MunicipalCalendarParser.parseIfPresent(loadFixture("sannois.txt"), 2026)!!
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val februaryVegetaux = events
            .filter { it.date.monthValue == 2 && WasteType.VEGETAUX in it.wasteTypes }
            .map { it.date }

        assertEquals(listOf(LocalDate.of(2026, 2, 17)), februaryVegetaux)
    }

    @Test
    fun parsesErmontPavillonsScheduleFromPdf() {
        val text = loadFixture("ermont.txt")
        val rules = MunicipalCalendarParser.parseIfPresent(text, 2026)!!

        assertEquals(DayOfWeek.TUESDAY, rules.orduresDay)
        assertEquals(DayOfWeek.THURSDAY, rules.emballagesDay)
        assertEquals(CollectionRecurrence.MONTHLY_NTH_WEEKDAY, rules.verreRecurrence)
        assertEquals(DayOfWeek.FRIDAY, rules.verreDay)
        assertEquals(4, rules.verreMonthOrdinal)
        assertEquals(DayOfWeek.WEDNESDAY, rules.encombrantsDay)
        assertEquals(2, rules.encombrantsMonthOrdinal)
        assertEquals(DayOfWeek.MONDAY, rules.vegetauxSchedule?.dayOfWeek)
        assertEquals(CollectionRecurrence.WEEKLY, rules.emballagesRecurrence)
    }

    @Test
    fun sannoisGeneratedVerreMatchesSecondMondayOfMonth() {
        val rules = MunicipalCalendarParser.parseIfPresent(loadFixture("sannois.txt"), 2026)!!
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val verreDates = events.filter { WasteType.VERRE in it.wasteTypes }.map { it.date }

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 12),
                LocalDate.of(2026, 2, 9),
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 4, 13),
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 6, 8),
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 12, 14)
            ),
            verreDates
        )
    }

    @Test
    fun ermontGeneratedVerreMatchesFourthFridayOfMonth() {
        val rules = MunicipalCalendarParser.parseIfPresent(loadFixture("ermont.txt"), 2026)!!
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val verreDates = events.filter { WasteType.VERRE in it.wasteTypes }.map { it.date }

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 23),
                LocalDate.of(2026, 2, 27),
                LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 4, 24),
                LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 6, 26),
                LocalDate.of(2026, 7, 24),
                LocalDate.of(2026, 8, 28),
                LocalDate.of(2026, 9, 25),
                LocalDate.of(2026, 10, 23),
                LocalDate.of(2026, 11, 27),
                LocalDate.of(2026, 12, 25)
            ),
            verreDates
        )
    }

    private fun loadFixture(name: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("calendars/$name")) {
            "Missing test fixture calendars/$name"
        }
        return stream.bufferedReader().readText()
    }
}
