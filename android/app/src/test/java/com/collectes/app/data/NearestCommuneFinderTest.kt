package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearestCommuneFinderTest {
    @Test
    fun findsCormeillesNearItsCenter() {
        val match = NearestCommuneFinder.findNearest(49.0942, 1.9925)
        assertNotNull(match)
        assertEquals("cormeilles-en-vexin", match!!.commune.slug)
        assertTrue(match.distanceKm < 1.0)
    }

    @Test
    fun findsErmontNearItsCenter() {
        val match = NearestCommuneFinder.findNearest(48.9892, 2.2581)
        assertNotNull(match)
        assertEquals("ermont", match!!.commune.slug)
    }

    @Test
    fun findsCabourgNearItsCenter() {
        val match = NearestCommuneFinder.findNearest(49.2883, -0.1164)
        assertNotNull(match)
        assertEquals("cabourg", match!!.commune.slug)
    }

    @Test
    fun sannoisCloserThanErmontFromSannoisCenter() {
        val match = NearestCommuneFinder.findNearest(48.9719, 2.2569)
        assertNotNull(match)
        assertEquals("sannois", match!!.commune.slug)
    }

    @Test
    fun haversineIsSymmetricAndPositive() {
        val a = NearestCommuneFinder.haversineKm(49.0, 2.0, 49.1, 2.1)
        val b = NearestCommuneFinder.haversineKm(49.1, 2.1, 49.0, 2.0)
        assertEquals(a, b, 0.001)
        assertTrue(a > 0)
    }

    @Test
    fun allCommunesHaveCoordinates() {
        VexinCommunes.all.forEach { commune ->
            assertTrue(
                "${commune.slug} missing coordinates",
                commune.latitude != 0.0 || commune.longitude != 0.0
            )
        }
    }
}
