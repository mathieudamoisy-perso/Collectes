package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EncombrantsFetcherTest {
    private val fetcher = EncombrantsFetcher()
    private val magny = VexinCommunes.bySlug("magny-en-vexin")!!

    @Test
    fun toCollectionDaysUsesEncombrantsType() {
        val days = fetcher.toCollectionDays(listOf(LocalDate.of(2026, 5, 21)))
        assertEquals(1, days.size)
        assertEquals(WasteType.ENCOMBRANTS, days.first().wasteTypes.single())
    }

    @Test
    fun magnyEncombrantsMatchOfficialPage() {
        val text = """
          La collecte des objets encombrants a lieu 2 fois par an
          21/05/2026
          18/11/2026
          Point(s) d'apport(s) volontaire
        """.trimIndent()
        val dates = EncombrantsFetcher().parseDatesFromText(text, 2026)
        assertEquals(
            listOf(LocalDate.of(2026, 5, 21), LocalDate.of(2026, 11, 18)),
            dates
        )
    }

    @Test
    fun communePageUrlUsesSlug() {
        assertTrue(magny.pageUrl.contains("magny-en-vexin"))
    }
}
