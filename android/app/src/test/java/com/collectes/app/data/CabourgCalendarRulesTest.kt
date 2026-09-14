package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CabourgCalendarRulesTest {
    private val commune = VexinCommunes.bySlug("cabourg")!!
    private val rules = OfficialCommuneSchedules.rules(2026, "cabourg")

    @Test
    fun reconcilerUsesNcpaRulesWithoutPdf() {
        val reconciled = CalendarReconciler.reconcile(
            pdfText = null,
            pageText = null,
            commune = commune,
            year = 2026
        )
        assertEquals(DayOfWeek.MONDAY, reconciled.orduresDay)
        assertEquals(setOf(DayOfWeek.FRIDAY), reconciled.orduresExtraDays)
        assertEquals(CollectionRecurrence.WEEKLY, reconciled.emballagesRecurrence)
        assertEquals(DayOfWeek.THURSDAY, reconciled.vegetauxSchedule!!.dayOfWeek)
    }

    @Test
    fun generatesMondayAndFridayOrduresOffSeason() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val ordures = events.filter { WasteType.ORDURES in it.wasteTypes }.map { it.date }.toSet()
        assertTrue(LocalDate.of(2026, 1, 5) in ordures) // lundi
        assertTrue(LocalDate.of(2026, 1, 9) in ordures) // vendredi
        assertFalse(LocalDate.of(2026, 1, 7) in ordures) // mercredi hors saison
    }

    @Test
    fun generatesWednesdayOrduresInJulyAndAugustOnly() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val ordures = events.filter { WasteType.ORDURES in it.wasteTypes }.map { it.date }.toSet()
        assertTrue(LocalDate.of(2026, 7, 1) in ordures) // mercredi juillet
        assertTrue(LocalDate.of(2026, 8, 5) in ordures) // mercredi août
        assertFalse(LocalDate.of(2026, 6, 3) in ordures) // mercredi juin
        assertFalse(LocalDate.of(2026, 9, 2) in ordures) // mercredi septembre
    }

    @Test
    fun generatesWeeklyMondayEmballagesAndCombinesWithOrdures() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val monday = events.find { it.date == LocalDate.of(2026, 1, 5) }!!
        assertTrue(WasteType.ORDURES in monday.wasteTypes)
        assertTrue(WasteType.EMBALLAGES in monday.wasteTypes)
    }

    @Test
    fun septemberMondayHasOrduresAndEmballages() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val monday = events.find { it.date == LocalDate.of(2026, 9, 21) }!!
        assertTrue(WasteType.ORDURES in monday.wasteTypes)
        assertTrue(WasteType.EMBALLAGES in monday.wasteTypes)
        assertFalse(WasteType.VEGETAUX in monday.wasteTypes)
    }

    @Test
    fun generatesVegetauxThursdaysInSeasonOnly() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val vegetaux = events.filter { WasteType.VEGETAUX in it.wasteTypes }.map { it.date }.toSet()
        assertTrue(LocalDate.of(2026, 3, 19) in vegetaux)
        assertTrue(LocalDate.of(2026, 11, 12) in vegetaux)
        assertFalse(LocalDate.of(2026, 3, 12) in vegetaux)
        assertFalse(LocalDate.of(2026, 11, 19) in vegetaux)
    }

    @Test
    fun doesNotGenerateVerreDoorCollection() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        assertTrue(events.none { WasteType.VERRE in it.wasteTypes })
    }

    @Test
    fun excludesChristmasAndNewYear() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = true)
        val dates = events.map { it.date }.toSet()
        assertFalse(LocalDate.of(2026, 12, 25) in dates) // vendredi
        assertFalse(LocalDate.of(2027, 1, 1) in dates) // vendredi
    }
}
