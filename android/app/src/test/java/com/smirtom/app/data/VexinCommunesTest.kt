package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexinCommunesTest {
    @Test
    fun includesAllSupportedCommunes() {
        assertEquals(6, VexinCommunes.all.size)
        assertEquals(
            listOf(
                "Magny-en-Vexin",
                "Théméricourt",
                "Cormeilles-en-Vexin",
                "Épiais-Rhus",
                "Sannois",
                "Ermont"
            ),
            VexinCommunes.all.map { it.displayName }
        )
    }

    @Test
    fun defaultIsMagnyEnVexin() {
        assertEquals("magny-en-vexin", VexinCommunes.default.slug)
        assertEquals("Magny-en-Vexin", VexinCommunes.default.displayName)
    }

    @Test
    fun slugLookupWorks() {
        assertNotNull(VexinCommunes.bySlug("themericourt"))
        assertEquals("Théméricourt", VexinCommunes.bySlug("themericourt")?.displayName)
        assertNotNull(VexinCommunes.bySlug("cormeilles-en-vexin"))
        assertNotNull(VexinCommunes.bySlug("epiais-rhus"))
        assertNotNull(VexinCommunes.bySlug("sannois"))
        assertNotNull(VexinCommunes.bySlug("ermont"))
        assertNotNull(VexinCommunes.bySlug("ermont-eaubonne"))
        assertNull(VexinCommunes.bySlug("nucourt"))
        assertNull(VexinCommunes.bySlug("valmondois"))
    }

    @Test
    fun magnyOfficialCalendarIsDirectPdf() {
        val url = VexinCommunes.default.officialCalendarUrl
        assertTrue(url.endsWith(".pdf"))
        assertTrue(url.contains("Magny-en-vexin", ignoreCase = true))
    }

    @Test
    fun themericourtOfficialCalendarIsDirectPdf() {
        val url = VexinCommunes.bySlug("themericourt")!!.officialCalendarUrl
        assertTrue(url.endsWith(".pdf"))
        assertTrue(url.contains("Themericourt", ignoreCase = true))
        assertTrue(url.contains("Avernes", ignoreCase = true))
    }

    @Test
    fun cormeillesOfficialCalendarIsDirectPdf() {
        val commune = VexinCommunes.bySlug("cormeilles-en-vexin")!!
        assertTrue(commune.officialCalendarUrl.endsWith(".pdf"))
        assertTrue(commune.officialCalendarUrl.contains("Cormeilles", ignoreCase = true))
        assertTrue(commune.pageUrl.contains("cormeilles-en-vexin"))
        assertNull(commune.infoPageUrl)
    }

    @Test
    fun epiaisRhusSharesCormeillesCalendarPdf() {
        val commune = VexinCommunes.bySlug("epiais-rhus")!!
        assertEquals("Épiais-Rhus", commune.displayName)
        assertTrue(commune.officialCalendarUrl.endsWith(".pdf"))
        assertTrue(commune.officialCalendarUrl.contains("Cormeilles-Epiais", ignoreCase = true))
        assertTrue(commune.pageUrl.contains("epiais-rhus"))
        assertNull(commune.infoPageUrl)
        assertTrue(commune.usesSmirtomNetwork)
    }

    @Test
    fun sannoisUsesExternalCalendarAndInfoPage() {
        val commune = VexinCommunes.bySlug("sannois")!!
        assertTrue(commune.officialCalendarUrl.endsWith(".pdf"))
        assertTrue(commune.officialCalendarUrl.contains("ville-sannois.fr"))
        assertTrue(commune.infoPageUrl!!.contains("ville-sannois.fr"))
        assertTrue(commune.pageUrl.contains("ville-sannois.fr"))
    }

    @Test
    fun ermontUsesExternalCalendarAndInfoPage() {
        val commune = VexinCommunes.bySlug("ermont")!!
        assertEquals("Ermont", commune.displayName)
        assertEquals("ermont", commune.slug)
        assertTrue(commune.officialCalendarUrl.endsWith(".pdf"))
        assertTrue(commune.officialCalendarUrl.contains("ermont.fr"))
        assertTrue(commune.infoPageUrl!!.contains("ermont.fr"))
        assertFalse(commune.usesSmirtomNetwork)
        assertFalse(commune.officialCalendarSubtitle().contains("SMIRTOM", ignoreCase = true))
        assertTrue(commune.officialCalendarSubtitle().contains("Emeraude", ignoreCase = true))
    }

    @Test
    fun allCommunesHaveUniqueSlugsAndPdfUrls() {
        val slugs = VexinCommunes.all.map { it.slug }
        assertEquals(slugs.size, slugs.distinct().size)
        VexinCommunes.all.forEach { commune ->
            assertTrue(
                "${commune.displayName} doit avoir une URL PDF",
                commune.officialCalendarUrl.isNotBlank()
            )
        }
    }

    @Test
    fun guideSourceLabelsMatchPdfSource() {
        val magny = VexinCommunes.default
        assertEquals("Communes du Vexin", magny.guideSourceTitle())
        assertNull(magny.guideSourceSubtitle())
        assertEquals("En savoir plus sur le site officiel", magny.guideInfoLinkLabel())

        val sannois = VexinCommunes.bySlug("sannois")!!
        assertEquals("Ville de Sannois", sannois.guideSourceTitle())
        assertTrue(sannois.guideSourceSubtitle()!!.contains("ville-sannois.fr"))
        assertFalse(sannois.guideSourceSubtitle()!!.contains("Emeraude", ignoreCase = true))
        assertTrue(sannois.guideInfoLinkLabel().contains("ville-sannois.fr"))
        assertNull(sannois.guideSecondaryInfoUrl())

        val ermont = VexinCommunes.bySlug("ermont")!!
        assertEquals("Syndicat Emeraude", ermont.guideSourceTitle())
        assertTrue(ermont.guideInfoLinkLabel().contains("syndicat-emeraude.fr"))
        assertNotNull(ermont.guideSecondaryInfoUrl())
    }

    @Test
    fun officialCalendarSubtitleReflectsPdfSource() {
        val magny = VexinCommunes.default
        assertTrue(magny.officialCalendarSubtitle().contains("calendrier officiel", ignoreCase = true))
        assertFalse(magny.officialCalendarSubtitle().contains("SMIRTOM", ignoreCase = true))

        val sannois = VexinCommunes.bySlug("sannois")!!
        assertFalse(sannois.officialCalendarSubtitle().contains("Emeraude", ignoreCase = true))
        assertTrue(sannois.officialCalendarSubtitle().contains("ville de Sannois", ignoreCase = true))

        val ermont = VexinCommunes.bySlug("ermont")!!
        assertTrue(ermont.officialCalendarSubtitle().contains("Emeraude", ignoreCase = true))
    }

    @Test
    fun guideTerritoryMatchesNetwork() {
        assertEquals(WasteGuideTerritory.SMIRTOM_VEXIN, VexinCommunes.default.guideTerritory)
        assertEquals(WasteGuideTerritory.SYNDICAT_EMERAUDE, VexinCommunes.bySlug("ermont")!!.guideTerritory)
        assertEquals(WasteGuideTerritory.SYNDICAT_EMERAUDE, VexinCommunes.bySlug("sannois")!!.guideTerritory)
    }
}
