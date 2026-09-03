package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WasteStreamGuidesTest {
    @Test
    fun smirtomTerritoryHasFiveGuidesWithAcceptedItems() {
        val guides = WasteStreamGuides.forTerritory(WasteGuideTerritory.SMIRTOM_VEXIN)
        assertEquals(5, guides.size)
        assertEquals(WasteType.EMBALLAGES, guides.first().type)
        guides.forEach { guide ->
            assertTrue("${guide.type} doit avoir au moins 2 items acceptés", guide.acceptedItems.size >= 2)
            assertTrue("${guide.type} doit avoir au moins 1 item refusé", guide.rejectedItems.isNotEmpty())
        }
    }

    @Test
    fun emeraudeTerritoryHasFiveGuidesWithAcceptedItems() {
        val guides = WasteStreamGuides.forTerritory(WasteGuideTerritory.SYNDICAT_EMERAUDE)
        assertEquals(5, guides.size)
        guides.forEach { guide ->
            assertTrue("${guide.type} doit avoir au moins 2 items acceptés", guide.acceptedItems.size >= 2)
        }
    }

    @Test
    fun forCommuneUsesTerritory() {
        val magny = VexinCommunes.bySlug("magny-en-vexin")!!
        val ermont = VexinCommunes.bySlug("ermont")!!

        assertEquals(
            WasteStreamGuides.forTerritory(WasteGuideTerritory.SMIRTOM_VEXIN),
            WasteStreamGuides.forCommune(magny)
        )
        assertEquals(
            WasteStreamGuides.forTerritory(WasteGuideTerritory.SYNDICAT_EMERAUDE),
            WasteStreamGuides.forCommune(ermont)
        )
    }

    @Test
    fun guideForReturnsMatchingType() {
        val guide = WasteStreamGuides.guideFor(WasteGuideTerritory.SMIRTOM_VEXIN, WasteType.VERRE)
        assertEquals(WasteType.VERRE, guide?.type)
        assertTrue(guide!!.acceptedItems.any { it.contains("Bouteilles", ignoreCase = true) })
    }
}
