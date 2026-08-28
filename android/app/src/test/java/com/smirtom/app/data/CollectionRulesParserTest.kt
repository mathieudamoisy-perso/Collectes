package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CollectionRulesParserTest {
    private val magnyCharmontPdf = """
      Collecte du verre toutes les 4 semaines Pour la commune de Charmont (mardi) Pour la commune de Magny en Vexin (mardi)
      A B
      Collecte des Emballages / Papiers toutes les 2 semaines le mardi
      OM Collecte des Ordures Ménagères toutes les semaines le lundi
      6 janv-26 13 janv-26 20 janv-26 27 janv-26
      Ma 8 B
      A A B Pour la commune de Magny en Vexin (mardi) B Pour la commune de Magny en Vexin (mardi)
      Pour la commune de Charmont (mardi)
    """.trimIndent()

    @Test
    fun extractsMagnyRulesFromPdfLikeText() {
        val rules = CollectionRulesParser.parse(magnyCharmontPdf, 2026, "Magny-en-Vexin")
        assertEquals(DayOfWeek.MONDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.TUESDAY, rules.verreDay)
        assertEquals(LocalDate.of(2026, 1, 6), rules.emballagesAnchor)
        assertEquals(LocalDate.of(2026, 1, 27), rules.verreAnchor)
    }

    @Test
    fun extractsCharmontVerreAsGroupA() {
        val rules = CollectionRulesParser.parse(magnyCharmontPdf, 2026, "Charmont")
        assertEquals(LocalDate.of(2026, 1, 6), rules.emballagesAnchor)
        assertEquals(LocalDate.of(2026, 1, 13), rules.verreAnchor)
    }

    @Test
    fun magnyVerreOffsetIsThreeWeeksAfterEmballages() {
        assertEquals(21L, CollectionRulesParser.verreOffsetDays(magnyCharmontPdf, "Magny-en-Vexin"))
        assertEquals(7L, CollectionRulesParser.verreOffsetDays(magnyCharmontPdf, "Charmont"))
    }

    @Test
    fun extractsMagnyRulesFromCommunePage() {
        val text = """
      Magny-en-Vexin
      Le lundi pour les ordures ménagères
      Le mardi toutes les 2 semaines pour les emballages/papiers
      Le mardi toutes les 4 semaines pour le verre
    """.trimIndent()

        val rules = CollectionRulesParser.parse(text, 2026, "Magny-en-Vexin")
        assertEquals(DayOfWeek.MONDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.TUESDAY, rules.verreDay)
        assertEquals(LocalDate.of(2026, 1, 6), rules.emballagesAnchor)
        assertEquals(LocalDate.of(2026, 1, 27), rules.verreAnchor)
    }

    @Test
    fun wyPageUsesTuesdayVerreAfter2024Change() {
        val text = """
      Wy-dit-Joli-Village
      Le mercredi pour les ordures ménagères
      Le mardi toutes les 2 semaines pour les emballages/papiers
      Le jeudi, puis le mardi à partir du 12/03/2024, toutes les 4 semaines pour le verre
    """.trimIndent()
        val rules = CollectionRulesParser.parse(text, 2026, "Wy-dit-Joli-Village")
        assertEquals(DayOfWeek.WEDNESDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.TUESDAY, rules.verreDay)
    }

    @Test
    fun magnyCalendarIncludes8SeptemberVerre() {
        val rules = CollectionRulesParser.parse(magnyCharmontPdf, 2026, "Magny-en-Vexin")
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val sept8 = events.single { it.date == LocalDate.of(2026, 9, 8) }
        assertEquals(listOf(WasteType.VERRE), sept8.wasteTypes)
        assertFalse(
            events.any {
                it.date == LocalDate.of(2026, 1, 13) && it.wasteTypes.contains(WasteType.VERRE)
            }
        )
        assertTrue(
            events.any {
                it.date == LocalDate.of(2026, 1, 27) && it.wasteTypes.contains(WasteType.VERRE)
            }
        )
    }

    @Test
    fun magnyFallbackUsesGroupBVerre() {
        val rules = CollectionRules.fallback(2026, "magny-en-vexin")
        assertEquals(LocalDate.of(2026, 1, 6), rules.emballagesAnchor)
        assertEquals(LocalDate.of(2026, 1, 27), rules.verreAnchor)
    }

    @Test
    fun charmontFallbackUsesGroupAVerre() {
        val rules = CollectionRules.fallback(2026, "charmont")
        assertEquals(LocalDate.of(2026, 1, 13), rules.verreAnchor)
    }

    @Test
    fun amblevilleFallbackUsesFridayOmFromOfficialPage() {
        val rules = CollectionRules.fallback(2026, "ambleville")
        assertEquals(DayOfWeek.FRIDAY, rules.orduresDay)
        assertEquals(DayOfWeek.MONDAY, rules.emballagesDay)
    }

    @Test
    fun themericourtFallbackUsesWednesdayOmAndThursdayVerre() {
        val rules = CollectionRules.fallback(2026, "themericourt")
        assertEquals(DayOfWeek.WEDNESDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.THURSDAY, rules.verreDay)
    }
}
