package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class OfficialCalendarAlignmentTest {
    private val magny = VexinCommunes.bySlug("magny-en-vexin")!!
    private val themericourt = VexinCommunes.bySlug("themericourt")!!
    private val cormeilles = VexinCommunes.bySlug("cormeilles-en-vexin")!!
    private val sannois = VexinCommunes.bySlug("sannois")!!
    private val ermont = VexinCommunes.bySlug("ermont-eaubonne")!!

    @Test
    fun magnyMatchesOfficialPdfGrid() {
        val pdf = loadFixture("magny.txt")
        val rules = CalendarReconciler.reconcile(pdf, magnyPage(), magny, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)

        assertEquals(LocalDate.of(2026, 1, 27), rules.verreAnchor)
        assertEquals(
            listOf(WasteType.VERRE),
            events.single { it.date == LocalDate.of(2026, 9, 8) }.wasteTypes
        )
        assertPdfVerreDays(pdf, DayOfWeek.TUESDAY, 'b', events)
    }

    @Test
    fun themericourtMatchesOfficialPdfGrid() {
        val pdf = loadFixture("themericourt.txt")
        val rules = CalendarReconciler.reconcile(pdf, themericourtPage(), themericourt, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)

        assertEquals(DayOfWeek.WEDNESDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.THURSDAY, rules.verreDay)
        assertPdfVerreDays(pdf, DayOfWeek.THURSDAY, 'b', events)
    }

    @Test
    fun cormeillesUsesTuesdayVerreCycleFromMarch2024() {
        val pdf = loadFixture("cormeilles.txt")
        val rules = CalendarReconciler.reconcile(pdf, cormeillesPage(), cormeilles, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)

        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
        assertEquals(DayOfWeek.MONDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.TUESDAY, rules.verreDay)
        assertEquals(LocalDate.of(2026, 1, 12), rules.emballagesAnchor)
        assertEquals(LocalDate.of(2026, 1, 6), rules.verreAnchor)
        assertTrue(
            events.any {
                it.date == LocalDate.of(2026, 3, 3) && it.wasteTypes == listOf(WasteType.VERRE)
            }
        )
    }

    @Test
    fun sannoisPdfReconciliationMatchesEmeraudeCalendar() {
        val pdf = loadFixture("sannois.txt")
        val rules = CalendarReconciler.reconcile(pdf, null, sannois, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val encombrants = CalendarDateGenerator.encombrantsDates(2026, rules, includeNextYearJanuary = false)

        assertEquals(LocalDate.of(2026, 1, 1), events.first { WasteType.ORDURES in it.wasteTypes }.date)
        assertEquals(LocalDate.of(2026, 1, 6), events.first { WasteType.EMBALLAGES in it.wasteTypes }.date)
        assertEquals(LocalDate.of(2026, 1, 12), events.first { WasteType.VERRE in it.wasteTypes }.date)
        assertEquals(LocalDate.of(2026, 1, 7), encombrants.first())
    }

    @Test
    fun ermontPdfReconciliationMatchesEmeraudeCalendar() {
        val pdf = loadFixture("ermont.txt")
        val rules = CalendarReconciler.reconcile(pdf, null, ermont, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val encombrants = CalendarDateGenerator.encombrantsDates(2026, rules, includeNextYearJanuary = false)

        assertEquals(LocalDate.of(2026, 1, 6), events.first { WasteType.ORDURES in it.wasteTypes }.date)
        assertEquals(LocalDate.of(2026, 1, 1), events.first { WasteType.EMBALLAGES in it.wasteTypes }.date)
        assertEquals(LocalDate.of(2026, 1, 23), events.first { WasteType.VERRE in it.wasteTypes }.date)
        assertEquals(LocalDate.of(2026, 1, 14), encombrants.first())
    }

    private fun assertPdfVerreDays(
        pdf: String,
        verreDay: DayOfWeek,
        letter: Char,
        events: List<CollectionDay>
    ) {
        val pdfDays = PdfGridMarkers.letterDayNumbers(pdf, verreDay).let {
            if (letter == 'a') it.first else it.second
        }
        val generatedDays = events
            .filter { WasteType.VERRE in it.wasteTypes }
            .map { it.date.dayOfMonth }
            .toSet()
        val overlap = pdfDays.intersect(generatedDays)
        assertTrue(
            "Verre dates should largely match PDF grid (overlap=$overlap, pdf=$pdfDays)",
            overlap.size >= 8
        )
    }

    private fun magnyPage() = """
      Magny-en-Vexin
      Le lundi pour les ordures ménagères
      Le mardi toutes les 2 semaines pour les emballages/papiers
      Le mardi toutes les 4 semaines pour le verre
    """.trimIndent()

    private fun themericourtPage() = """
      Théméricourt
      Le mercredi pour les ordures ménagères
      Le mardi toutes les 2 semaines pour les emballages/papiers
      Le jeudi toutes les 4 semaines pour le verre
    """.trimIndent()

    private fun cormeillesPage() = """
      Cormeilles-en-Vexin
      Le jeudi pour les ordures ménagères
      Le lundi toutes les 2 semaines pour les emballages/papiers
      Le mercredi, puis le mardi à partir du 05/03/2024, toutes les 4 semaines pour le verre
    """.trimIndent()

    private fun loadFixture(name: String): String {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("calendars/$name")) {
            "Missing test fixture calendars/$name"
        }
        return stream.bufferedReader().readText()
    }
}
