package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EncombrantsFetcherTest {
    private val fetcher = EncombrantsFetcher()

    @Test
    fun fallbackDatesFor2026() {
        val dates = fetcher.fetchDates(2026)
        assertTrue(dates.contains(LocalDate.of(2026, 5, 21)))
        assertTrue(dates.contains(LocalDate.of(2026, 11, 18)))
    }

    @Test
    fun toCollectionDaysUsesEncombrantsType() {
        val days = fetcher.toCollectionDays(listOf(LocalDate.of(2026, 5, 21)))
        assertEquals(1, days.size)
        assertEquals(WasteType.ENCOMBRANTS, days.first().wasteTypes.single())
    }
}
