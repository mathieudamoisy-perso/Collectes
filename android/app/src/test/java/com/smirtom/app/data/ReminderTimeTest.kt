package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTimeTest {
    @Test
    fun optionsAreEveryThirtyMinutesBetweenFiveAndTwentyThree() {
        val options = ReminderTime.options()

        assertEquals(37, options.size)
        assertEquals(5 * 60, options.first())
        assertEquals(23 * 60, options.last())
        assertTrue(options.zipWithNext().all { (left, right) -> right - left == 30 })
    }

    @Test
    fun defaultIsNoon() {
        assertEquals(12 * 60, ReminderTime.DEFAULT_MINUTES)
    }

    @Test
    fun formatUsesFrenchHourLabels() {
        assertEquals("9h00", ReminderTime.format(9 * 60))
        assertEquals("9h30", ReminderTime.format(9 * 60 + 30))
        assertEquals("12h00", ReminderTime.format(12 * 60))
    }

    @Test
    fun coerceSnapsToNearestSlot() {
        assertEquals(9 * 60, ReminderTime.coerce(9 * 60 + 10))
        assertEquals(9 * 60 + 30, ReminderTime.coerce(9 * 60 + 20))
    }
}
