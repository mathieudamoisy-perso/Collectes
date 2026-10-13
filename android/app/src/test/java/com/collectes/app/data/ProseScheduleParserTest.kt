package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProseScheduleParserTest {
    @Test
    fun parsesAnnecyStyleMemo() {
        val text = """
            ANNECY
            ORDURES MÉNAGÈRES INCINÉRABLES
            Lundi et jeudi 5h à 12h
            EMBALLAGES ET PAPIERS
            Vendredi 5h à 12h
            EMBALLAGES EN VERRE
            Apport volontaire
        """.trimIndent()
        val rules = ProseScheduleParser.parseIfPresent(text, 2026, "Annecy")
        assertNotNull(rules)
        assertEquals(DayOfWeek.MONDAY, rules!!.orduresDay)
        assertEquals(setOf(DayOfWeek.THURSDAY), rules.orduresExtraDays)
        assertEquals(DayOfWeek.FRIDAY, rules.emballagesDay)
        assertEquals(CollectionRecurrence.WEEKLY, rules.emballagesRecurrence)

        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        assertTrue(events.any { it.date == LocalDate.of(2026, 1, 5) && WasteType.ORDURES in it.wasteTypes })
        assertTrue(events.any { it.date == LocalDate.of(2026, 1, 8) && WasteType.ORDURES in it.wasteTypes })
        assertTrue(events.any { it.date == LocalDate.of(2026, 1, 9) && WasteType.EMBALLAGES in it.wasteTypes })
    }

    @Test
    fun parsesVerreSecondAndFourthFriday() {
        val text = """
            Collecte porte à porte
            Ordures ménagères : mercredi
            Emballages : lundi
            Verre : 2ème et 4ème vendredi du mois
        """.trimIndent()
        val rules = ProseScheduleParser.parseIfPresent(text, 2026)!!
        assertEquals(DayOfWeek.WEDNESDAY, rules.orduresDay)
        assertEquals(DayOfWeek.MONDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.FRIDAY, rules.verreDay)
        assertEquals(setOf(2, 4), rules.verreMonthOrdinals)
        assertEquals(CollectionRecurrence.MONTHLY_NTH_WEEKDAY, rules.verreRecurrence)

        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        // jan 2026 : 2nd Friday=9, 4th Friday=23
        assertTrue(events.any { it.date == LocalDate.of(2026, 1, 9) && WasteType.VERRE in it.wasteTypes })
        assertTrue(events.any { it.date == LocalDate.of(2026, 1, 23) && WasteType.VERRE in it.wasteTypes })
    }
}
