package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class OrduresParseBugTest {
    private val cormeilles = VexinCommunes.bySlug("cormeilles-en-vexin")!!

    @Test
    fun cormeillesPdfParseUsesThursdayNotMondayDefault() {
        val pdf = loadFixture("cormeilles.txt")
        val rules = CollectionRulesParser.parseIfPresent(pdf, 2026, cormeilles.displayName)!!
        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
    }

    @Test
    fun cormeillesPdfOnlyReconcileUsesThursday() {
        val pdf = loadFixture("cormeilles.txt")
        val rules = CalendarReconciler.reconcile(pdf, null, cormeilles, 2026)
        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        val today = LocalDate.of(2026, 9, 1)
        val first = events.filter { it.date.isAfter(today) }.minByOrNull { it.date }!!
        assertEquals(LocalDate.of(2026, 9, 3), first.date)
        assertEquals(WasteType.ORDURES, first.wasteTypes.single())
    }

    @Test
    fun legendOrduresHeaderDoesNotForceMondayDefault() {
        val text = """
          Cormeilles-en-Vexin et ùpiais-Rhus
          ORDURES MùNAGùRES
          Textiles sanitaires
          Collecte des Emballages / Papiers toutes les 2 semaines le lundi
          OM Collecte des Ordures Mùnagùres toutes les semaines le jeudi
        """.trimIndent()
        val rules = CollectionRulesParser.parse(text, 2026, cormeilles.displayName)
        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
    }

    @Test
    fun reconcileOverridesPdfMondayWithOfficialThursdayForCormeilles() {
        val pdf = """
          Cormeilles-en-Vexin et Epiais-Rhus
          ORDURES MENAGERES
          Textiles sanitaires
          Collecte des Emballages / Papiers toutes les 2 semaines le lundi
          ordures emballages verre
        """.trimIndent()
        val rules = CalendarReconciler.reconcile(
            pdfText = pdf,
            pageText = null,
            commune = cormeilles,
            year = 2026
        )
        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
    }

    private fun loadFixture(name: String): String {
        return checkNotNull(javaClass.classLoader!!.getResourceAsStream("calendars/$name")) {
            "Missing calendars/$name"
        }.bufferedReader().readText()
    }
}
