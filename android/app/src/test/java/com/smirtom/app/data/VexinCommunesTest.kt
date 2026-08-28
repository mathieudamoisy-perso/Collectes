package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexinCommunesTest {
    @Test
    fun includesOnlyMagnyAndThemericourt() {
        assertEquals(2, VexinCommunes.all.size)
        assertEquals(
            listOf("Magny-en-Vexin", "Théméricourt"),
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
}
