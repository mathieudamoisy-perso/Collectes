package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class ThemericourtSeptemberTest {
    private val themericourt = VexinCommunes.bySlug("themericourt")!!

    @Test
    fun september8HasNoEmballagesInOfficialPdfGrid() {
        val pdf = loadFixture("themericourt.txt")
        val rules = CalendarReconciler.reconcile(
            pdf,
            """
              Théméricourt
              Le mercredi pour les ordures ménagères
              Le mardi toutes les 2 semaines pour les emballages/papiers
              Le jeudi toutes les 4 semaines pour le verre
            """.trimIndent(),
            themericourt,
            2026
        )
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val sept8 = events.filter { it.date == LocalDate.of(2026, 9, 8) }

        assertEquals(LocalDate.of(2026, 1, 6), rules.emballagesAnchor)
        assertFalse(
            "Sept 8 should not have emballages (events=$sept8)",
            sept8.any { WasteType.EMBALLAGES in it.wasteTypes }
        )
    }

    @Test
    fun september1HasEmballagesInOfficialPdfGrid() {
        val pdf = loadFixture("themericourt.txt")
        val rules = CalendarReconciler.reconcile(
            pdf,
            """
              Théméricourt
              Le mercredi pour les ordures ménagères
              Le mardi toutes les 2 semaines pour les emballages/papiers
              Le jeudi toutes les 4 semaines pour le verre
            """.trimIndent(),
            themericourt,
            2026
        )
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)

        assertEquals(
            listOf(WasteType.EMBALLAGES),
            events.single { it.date == LocalDate.of(2026, 9, 1) }.wasteTypes
        )
    }

    private fun loadFixture(name: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("calendars/$name")) {
            "Missing test fixture calendars/$name"
        }
        return stream.bufferedReader().readText()
    }
}
