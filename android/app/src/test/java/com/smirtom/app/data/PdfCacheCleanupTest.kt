package com.smirtom.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfCacheCleanupTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun deleteCachedPdfsRemovesOnlyCalendarFiles() {
        val calendar = folder.newFile("calendar-2026-cormeilles-en-vexin.pdf")
        val other = folder.newFile("settings.json")
        calendar.writeText("stale")
        other.writeText("keep")

        SmirtomFetcher().deleteCachedPdfs(folder.root)

        assertFalse(calendar.exists())
        assertTrue(other.exists())
    }
}
