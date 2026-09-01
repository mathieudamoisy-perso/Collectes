package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PdfGridMarkersEmballagesTest {
    @Test
    fun cormeillesEmballagesAnchorAlignsWithSeptember7() {
        val pdf = javaClass.classLoader!!.getResourceAsStream("calendars/cormeilles.txt")!!
            .bufferedReader().readText()
        val anchor = PdfGridMarkers.pickEmballagesAnchor(pdf, 2026, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 1, 12), anchor)
    }
}
