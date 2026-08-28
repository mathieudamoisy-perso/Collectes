package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VexinCommunesTest {
    @Test
    fun includesOnlyPostalCode95420() {
        assertEquals(14, VexinCommunes.all.size)
        assertEquals(
            listOf(
                "Ambleville",
                "Arthies",
                "Banthelu",
                "Charmont",
                "Cléry-en-Vexin",
                "Genainville",
                "Hodent",
                "La Chapelle-en-Vexin",
                "Magny-en-Vexin",
                "Maudétour-en-Vexin",
                "Nucourt",
                "Omerville",
                "Saint-Gervais",
                "Wy-dit-Joli-Village"
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
        assertNotNull(VexinCommunes.bySlug("nucourt"))
        assertEquals("Nucourt", VexinCommunes.bySlug("nucourt")?.displayName)
        assertNull(VexinCommunes.bySlug("valmondois"))
    }
}
