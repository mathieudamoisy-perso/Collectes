package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CollectionRulesParserTest {
    @Test
    fun extractsMagnyRulesFromPdfLikeText() {
        val text = """
      Collecte du verre toutes les 4 semaines Pour la commune de Magny en Vexin (mardi)
      Collecte des Emballages / Papiers toutes les 2 semaines le mardi
      OM Collecte des Ordures Ménagères toutes les semaines le lundi
      6 janv-26 13 janv-26 20 janv-26
    """.trimIndent()

        val rules = CollectionRulesParser.parse(text, 2026, "Magny-en-Vexin")
        assertEquals(DayOfWeek.MONDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.TUESDAY, rules.verreDay)
        assertEquals(LocalDate.of(2026, 1, 6), rules.emballagesAnchor)
        assertEquals(LocalDate.of(2026, 1, 13), rules.verreAnchor)
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
    }
}
