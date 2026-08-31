package com.smirtom.app.notifications

import com.smirtom.app.data.WasteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class NotificationTestHelperTest {
    @Test
    fun randomCollectionEventPicksExactlyOneType() {
        val random = Random(42)
        val today = LocalDate.of(2026, 8, 31)

        repeat(20) {
            val (_, wasteTypes) = NotificationTestHelper.randomCollectionEvent(today, random)
            assertEquals(1, wasteTypes.size)
            assertTrue(wasteTypes.first() in WasteType.entries)
        }
    }

    @Test
    fun randomCollectionEventDateIsTomorrow() {
        val random = Random(7)
        val today = LocalDate.of(2026, 8, 31)

        repeat(20) {
            val (collectionDate, _) = NotificationTestHelper.randomCollectionEvent(today, random)
            assertEquals(today.plusDays(1), collectionDate)
        }
    }
}
