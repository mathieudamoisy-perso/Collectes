package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EncombrantsFetcherTest {
    private val fetcher = EncombrantsFetcher()
    private val magny = VexinCommunes.default

    @Test
    fun toCollectionDaysUsesEncombrantsType() {
        val days = fetcher.toCollectionDays(listOf(LocalDate.of(2026, 5, 21)))
        assertEquals(1, days.size)
        assertEquals(WasteType.ENCOMBRANTS, days.first().wasteTypes.single())
    }

    @Test
    fun communePageUrlUsesSlug() {
        assertTrue(magny.pageUrl.contains("magny-en-vexin"))
    }
}
