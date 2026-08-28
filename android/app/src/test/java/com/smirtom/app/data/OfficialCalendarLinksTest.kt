package com.smirtom.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialCalendarLinksTest {
    @Test
    fun onlineViewerOpensPdfThroughGoogleDocsNotDownloadPage() {
        val pdf =
            "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-01-Magny-en-vexin-Charmont.pdf"
        val viewUrl = OfficialCalendarLinks.onlineViewerUrl(pdf)
        assertTrue(viewUrl.startsWith("https://docs.google.com/gview?embedded=true&url="))
        assertTrue(viewUrl.contains("Calendrier-01-Magny"))
        assertFalse(viewUrl.contains("/telechargement/calendrier-2024"))
    }
}
