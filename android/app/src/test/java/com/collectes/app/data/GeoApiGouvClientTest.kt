package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoApiGouvClientTest {
    @Test
    fun parsesGeoJsonCentreAsLatLon() {
        val json = """
            [{
              "nom":"Magny-en-Vexin",
              "code":"95355",
              "codesPostaux":["95420"],
              "codeDepartement":"95",
              "centre":{"type":"Point","coordinates":[1.8058,49.162]}
            }]
        """.trimIndent()
        val communes = GeoApiGouvClient.parseArray(json)
        assertEquals(1, communes.size)
        assertEquals("Magny-en-Vexin", communes[0].nom)
        assertEquals("95355", communes[0].codeInsee)
        assertEquals(49.162, communes[0].latitude, 0.0001)
        assertEquals(1.8058, communes[0].longitude, 0.0001)
        assertEquals("95420", communes[0].subtitle)
    }

    @Test
    fun emptyJsonReturnsEmptyList() {
        assertTrue(GeoApiGouvClient.parseArray("").isEmpty())
        assertTrue(GeoApiGouvClient.parseArray("[]").isEmpty())
    }

    @Test
    fun detectsPostalCode() {
        assertTrue(GeoApiGouvClient.isPostalCode("95420"))
        assertTrue(GeoApiGouvClient.isPostalCode(" 75001 "))
        assertEquals(false, GeoApiGouvClient.isPostalCode("9542"))
        assertEquals(false, GeoApiGouvClient.isPostalCode("Magny"))
        assertEquals(false, GeoApiGouvClient.isPostalCode("95 420"))
    }
}
