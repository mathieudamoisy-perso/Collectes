package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class BlaincourtCalendarRulesTest {
    private val commune = VexinCommunes.bySlug("blaincourt-les-precy")!!
    private val rules = OfficialCommuneSchedules.rules(2026, "blaincourt-les-precy")

    @Test
    fun reconcilerUsesThelloiseRulesWithoutPdf() {
        val reconciled = CalendarReconciler.reconcile(
            pdfText = null,
            pageText = null,
            commune = commune,
            year = 2026
        )
        assertEquals(DayOfWeek.THURSDAY, reconciled.orduresDay)
        assertEquals(DayOfWeek.THURSDAY, reconciled.emballagesDay)
        assertEquals(CollectionRecurrence.WEEKLY, reconciled.emballagesRecurrence)
        assertEquals(DayOfWeek.THURSDAY, reconciled.vegetauxSchedule!!.dayOfWeek)
    }

    @Test
    fun generatesWeeklyThursdayOrduresAndEmballages() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val thursday = events.find { it.date == LocalDate.of(2026, 1, 8) }!!
        assertTrue(WasteType.ORDURES in thursday.wasteTypes)
        assertTrue(WasteType.EMBALLAGES in thursday.wasteTypes)
    }

    @Test
    fun generatesVegetauxThursdaysInSeasonOnly() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val vegetaux = events.filter { WasteType.VEGETAUX in it.wasteTypes }.map { it.date }.toSet()
        assertTrue(LocalDate.of(2026, 4, 2) in vegetaux) // premier jeudi après le 30/03
        assertTrue(LocalDate.of(2026, 11, 26) in vegetaux) // dernier jeudi avant le 27/11
        assertFalse(LocalDate.of(2026, 3, 26) in vegetaux)
        assertFalse(LocalDate.of(2026, 12, 3) in vegetaux)
    }

    @Test
    fun doesNotGenerateVerreDoorCollection() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        assertTrue(events.none { WasteType.VERRE in it.wasteTypes })
    }

    @Test
    fun postponesNewYearThursdayToSaturday() {
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val dates = events.associateBy { it.date }
        assertFalse(LocalDate.of(2026, 1, 1) in dates)
        val saturday = dates[LocalDate.of(2026, 1, 3)]!!
        assertTrue(WasteType.ORDURES in saturday.wasteTypes)
        assertTrue(WasteType.EMBALLAGES in saturday.wasteTypes)
    }
}
