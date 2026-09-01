package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CormeillesUpcomingDatesTest {
    private val cormeilles = VexinCommunes.bySlug("cormeilles-en-vexin")!!

    @Test
    fun septemberShowsOrduresOn3BeforeEmballagesOn7() {
        val pdf = loadFixture("cormeilles.txt")
        val page = """
          Cormeilles-en-Vexin
          Le jeudi pour les ordures ménagères
          Le lundi toutes les 2 semaines pour les emballages/papiers
          Le mercredi, puis le mardi à partir du 05/03/2024, toutes les 4 semaines pour le verre
        """.trimIndent()
        val rules = CalendarReconciler.reconcile(pdf, page, cormeilles, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
            .associateBy { it.date }

        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
        assertEquals(listOf(WasteType.ORDURES), events[LocalDate.of(2026, 9, 3)]?.wasteTypes)
        assertEquals(listOf(WasteType.EMBALLAGES), events[LocalDate.of(2026, 9, 7)]?.wasteTypes)

        assertEquals(LocalDate.of(2026, 1, 12), rules.emballagesAnchor)
        val today = LocalDate.of(2026, 9, 1)
        val upcoming = events.keys.filter { it.isAfter(today) }.sorted()
        assertEquals(LocalDate.of(2026, 9, 3), upcoming.first())
    }

    @Test
    fun marchVerreIsOn3Not7() {
        val pdf = loadFixture("cormeilles.txt")
        val rules = CalendarReconciler.reconcile(pdf, null, cormeilles, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)

        assertTrue(
            events.any {
                it.date == LocalDate.of(2026, 3, 3) && it.wasteTypes == listOf(WasteType.VERRE)
            }
        )
        assertTrue(
            events.none {
                it.date == LocalDate.of(2026, 3, 7) && it.wasteTypes.contains(WasteType.VERRE)
            }
        )
    }

    private fun loadFixture(name: String): String {
        return checkNotNull(javaClass.classLoader!!.getResourceAsStream("calendars/$name")) {
            "Missing calendars/$name"
        }.bufferedReader().readText()
    }
}
