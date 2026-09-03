package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CalendarReconcilerTest {
    private val magny = VexinCommunes.bySlug("magny-en-vexin")!!
    private val ambleville = VexinCommune("ambleville", "Ambleville")
    private val clery = VexinCommune("clery-en-vexin", "Cléry-en-Vexin")
    private val charmont = VexinCommune("charmont", "Charmont")

    private val magnyPage = """
      Magny-en-Vexin
      La collecte de vos déchets d'effectue :
      Le lundi pour les ordures ménagères
      Le mardi toutes les 2 semaines pour les emballages/papiers (en mélange dans le bac jaune).
      Le mardi toutes les 4 semaines pour le verre (dans le bac vert).
      La collecte des objets encombrants a lieu 2 fois par an
      21/05/2026
      18/11/2026
      Point(s) d'apport(s) volontaire
    """.trimIndent()

    private val magnyPdf = """
      Collecte du verre toutes les 4 semaines Pour la commune de Charmont (mardi) Pour la commune de Magny en Vexin (mardi)
      A B
      Collecte des Emballages / Papiers toutes les 2 semaines le mardi
      OM Collecte des Ordures Ménagères toutes les semaines le lundi
      6 janv-26 13 janv-26 20 janv-26 27 janv-26
      Ma 1 B Ma 3 B Ma 6 B Ma 8 B Ma 11 B Ma 14 B Ma 16 B Ma 19 B Ma 21 B Ma 24 B Ma 27 B Ma 29 B
      Ma 2 A Ma 5 A Ma 7 A Ma 10 A Ma 12 A Ma 13 A Ma 15 A Ma 17 A Ma 20 A Ma 22 A Ma 25 A Ma 28 A Ma 30 A
    """.trimIndent()

    private val amblevillePage = """
      Ambleville
      Le vendredi pour les ordures ménagères
      Le lundi toutes les 2 semaines pour les emballages/papiers
      Le lundi toutes les 4 semaines pour le verre
    """.trimIndent()

    @Test
    fun magnyCrossSourcesPutVerreOn8September() {
        val rules = CalendarReconciler.reconcile(magnyPdf, magnyPage, magny, 2026)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        assertEquals(DayOfWeek.MONDAY, rules.orduresDay)
        assertEquals(DayOfWeek.TUESDAY, rules.emballagesDay)
        assertEquals(LocalDate.of(2026, 1, 27), rules.verreAnchor)
        assertEquals(
            listOf(WasteType.VERRE),
            events.single { it.date == LocalDate.of(2026, 9, 8) }.wasteTypes
        )
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
    fun pdfGridLettersPickCharmontGroupA() {
        val rules = CalendarReconciler.reconcile(magnyPdf, magnyPage, charmont, 2026)
        assertEquals(LocalDate.of(2026, 1, 13), rules.verreAnchor)
        val events = CalendarDateGenerator.generate(2026, rules, includeNextYearJanuary = false)
        assertTrue(
            events.any {
                it.date == LocalDate.of(2026, 1, 13) && it.wasteTypes.contains(WasteType.VERRE)
            }
        )
        assertFalse(
            events.any {
                it.date == LocalDate.of(2026, 9, 8) && it.wasteTypes.contains(WasteType.VERRE)
            }
        )
    }

    @Test
    fun amblevillePageIsNotCopiedFromMagnyMondayOm() {
        val rules = CalendarReconciler.reconcile(null, amblevillePage, ambleville, 2026)
        assertEquals(DayOfWeek.FRIDAY, rules.orduresDay)
        assertEquals(DayOfWeek.MONDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.MONDAY, rules.verreDay)
    }

    @Test
    fun officialPagesCatalogKeepsCleryOnThursdayOm() {
        val rules = OfficialCommuneSchedules.rules(2026, clery.slug)
        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
        assertEquals(DayOfWeek.MONDAY, rules.emballagesDay)
    }

    @Test
    fun magnyEncombrantsMatchCommunePage() {
        val dates = EncombrantsFetcher().parseDatesFromText(magnyPage, 2026)
        assertEquals(
            listOf(LocalDate.of(2026, 5, 21), LocalDate.of(2026, 11, 18)),
            dates
        )
    }

    @Test
    fun cormeillesPageParsesThursdayOmAndEncombrants() {
        val cormeilles = VexinCommune("cormeilles-en-vexin", "Cormeilles-en-Vexin")
        val page = """
          Cormeilles-en-Vexin
          Le jeudi pour les ordures ménagères
          Le lundi toutes les 2 semaines pour les emballages/papiers
          Le mardi toutes les 4 semaines pour le verre
          La collecte des objets encombrants a lieu 2 fois par an
          11/03/2026
          30/09/2026
        """.trimIndent()
        val rules = CalendarReconciler.reconcile(null, page, cormeilles, 2026)
        assertEquals(DayOfWeek.THURSDAY, rules.orduresDay)
        assertEquals(DayOfWeek.MONDAY, rules.emballagesDay)
        assertEquals(DayOfWeek.TUESDAY, rules.verreDay)
        val encombrants = EncombrantsFetcher().parseDatesFromText(page, 2026)
        assertEquals(
            listOf(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 9, 30)),
            encombrants
        )
    }

    @Test
    fun pdfGridPrefersMagnyLetterB() {
        assertEquals('b', PdfGridMarkers.preferredLetter(magnyPdf, "Magny-en-Vexin"))
        assertEquals('a', PdfGridMarkers.preferredLetter(magnyPdf, "Charmont"))
        assertEquals(
            'b',
            PdfGridMarkers.preferredLetter(
                "B Pour les communes d'Avernes et Théméricourt (jeudi)",
                "Théméricourt"
            )
        )
        val (groupA, groupB) = PdfGridMarkers.letterDayNumbers(magnyPdf, DayOfWeek.TUESDAY)
        assertTrue(groupB.contains(8))
        assertTrue(groupB.contains(27))
        assertTrue(groupA.contains(13))
        assertFalse(groupB.contains(13))
    }
}
