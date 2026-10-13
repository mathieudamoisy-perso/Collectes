package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunesIndexParserTest {
    @Test
    fun parsesSeedIndexShape() {
        val json = """
            {
              "version": 1,
              "updatedAt": "2026-10-13",
              "communes": [
                {
                  "insee": "95355",
                  "slug": "magny-en-vexin",
                  "name": "Magny-en-Vexin",
                  "source": "smirtom",
                  "calendarUrl": "https://example.com/cal.pdf",
                  "lat": 49.162,
                  "lon": 1.8058
                },
                {
                  "insee": "95219",
                  "slug": "ermont",
                  "name": "Ermont",
                  "source": "direct",
                  "calendarUrl": "https://example.com/ermont.pdf",
                  "infoPageUrl": "https://example.com/info",
                  "lat": 48.98,
                  "lon": 2.25
                }
              ]
            }
        """.trimIndent()
        val index = CommunesIndexParser.parse(json)
        assertEquals(1, index.version)
        assertEquals(2, index.communes.size)
        val magny = index.byInsee("95355")
        assertNotNull(magny)
        assertEquals("smirtom", magny!!.source)
        assertTrue(magny.toVexinCommune().usesSmirtomNetwork)
        val ermont = index.byInsee("95219")!!.toVexinCommune()
        assertEquals("https://example.com/ermont.pdf", ermont.officialCalendarUrl)
        assertEquals("https://example.com/info", ermont.infoPageUrl)
    }
}
