package com.smirtom.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPdfMatcherTest {
    private val magny = VexinCommunes.default

    @Test
    fun matchesMagnyPdfUrl() {
        val haystack =
            "Télécharger ce document https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-01-Magny-en-vexin-Charmont.pdf"
        assertTrue(CalendarPdfMatcher.matches(haystack, 2026, magny))
    }

    @Test
    fun matchesMagnyListingTitle() {
        val haystack = "Calendrier 2026 Magny en Vexin / Charmont Calendriers du ramassage"
        assertTrue(CalendarPdfMatcher.matches(haystack, 2026, magny))
    }

    @Test
    fun rejectsOtherYear() {
        val haystack = "Calendrier 2025 Magny en Vexin / Charmont"
        assertFalse(CalendarPdfMatcher.matches(haystack, 2026, magny))
    }

    @Test
    fun rejectsUnrelatedPdf() {
        val haystack = "Rapport annuel 2026 https://smirtomduvexin.net/wp-content/uploads/2026/07/rapport.pdf"
        assertFalse(CalendarPdfMatcher.matches(haystack, 2026, magny))
    }
}
