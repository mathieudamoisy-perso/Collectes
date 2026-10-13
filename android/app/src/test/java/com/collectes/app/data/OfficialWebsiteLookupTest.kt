package com.collectes.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfficialWebsiteLookupTest {
    @Test
    fun extractSiteUriFromSparqlJson() {
        val json = """
            {
              "results": {
                "bindings": [
                  { "site": { "type": "uri", "value": "https://www.beaumontsuroise.fr" } }
                ]
              }
            }
        """.trimIndent()
        assertEquals(
            "https://www.beaumontsuroise.fr",
            OfficialWebsiteLookup.extractSiteUri(json)
        )
    }

    @Test
    fun extractSiteUriReturnsNullWhenEmpty() {
        assertNull(OfficialWebsiteLookup.extractSiteUri("""{"results":{"bindings":[]}}"""))
    }
}
