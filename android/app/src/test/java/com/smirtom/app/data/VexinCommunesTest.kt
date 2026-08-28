package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VexinCommunesTest {
    @Test
    fun includesAllVexinCommunes() {
        assertTrue(VexinCommunes.all.size >= 80)
    }

    @Test
    fun defaultIsMagnyEnVexin() {
        assertEquals("magny-en-vexin", VexinCommunes.default.slug)
        assertEquals("Magny-en-Vexin", VexinCommunes.default.displayName)
    }

    @Test
    fun slugLookupWorks() {
        assertNotNull(VexinCommunes.bySlug("valmondois"))
        assertEquals("Valmondois", VexinCommunes.bySlug("valmondois")?.displayName)
    }
}
