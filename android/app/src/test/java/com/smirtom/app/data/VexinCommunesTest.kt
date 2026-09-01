package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexinCommunesTest {
    @Test
    fun includesMagnyThemericourtCormeillesAndSannois() {
        assertEquals(4, VexinCommunes.all.size)
        assertEquals(
            listOf("Magny-en-Vexin", "Théméricourt", "Cormeilles-en-Vexin", "Sannois"),
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
        assertNotNull(VexinCommunes.bySlug("sannois"))
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
    fun sannoisUsesExternalCalendarAndInfoPage() {
        val commune = VexinCommunes.bySlug("sannois")!!
        assertTrue(commune.officialCalendarUrl.endsWith(".pdf"))
        assertTrue(commune.officialCalendarUrl.contains("ville-sannois.fr"))
        assertTrue(commune.infoPageUrl!!.contains("ville-sannois.fr"))
        assertTrue(commune.pageUrl.contains("ville-sannois.fr"))
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
}
