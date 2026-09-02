package com.smirtom.app.data

import android.graphics.BitmapFactory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class CcvtCalendarParserTest {
    private fun parse2026(): List<CollectionDay> {
        val url = checkNotNull(javaClass.classLoader).getResource("calendars/bouconvillers_page.png")
        val bitmap = BitmapFactory.decodeStream(url.openStream())
        checkNotNull(bitmap) { "PNG fixture could not be decoded" }
        return CcvtCalendarParser.parseBitmap(bitmap, 2026)
    }

    private fun typesOn(date: LocalDate, events: List<CollectionDay>): List<WasteType> {
        return events.firstOrNull { it.date == date }?.wasteTypes.orEmpty()
    }

    @Test
    fun parsesBouconvillers2026CalendarFromFixture() {
        val events = parse2026()
        assertTrue(events.isNotEmpty())
    }

    @Test
    fun september9HasEmballagesAndOrdures() {
        val types = typesOn(LocalDate.of(2026, 9, 9), parse2026())
        assertTrue(WasteType.EMBALLAGES in types)
        assertTrue(WasteType.ORDURES in types)
    }

    @Test
    fun october7HasEmballagesAndOrdures() {
        val types = typesOn(LocalDate.of(2026, 10, 7), parse2026())
        assertTrue(WasteType.EMBALLAGES in types)
        assertTrue(WasteType.ORDURES in types)
    }

    @Test
    fun october8HasEncombrants() {
        val types = typesOn(LocalDate.of(2026, 10, 8), parse2026())
        assertTrue(WasteType.ENCOMBRANTS in types)
    }

    @Test
    fun october3HasNoCollectionFromPdfBackground() {
        val types = typesOn(LocalDate.of(2026, 10, 3), parse2026())
        assertTrue(types.isEmpty())
    }

    @Test
    fun octoberSaturdaysHaveNoRegularCollection() {
        val events = parse2026()
        listOf(3, 10, 17, 24, 31).forEach { day ->
            val types = typesOn(LocalDate.of(2026, 10, day), events)
            assertTrue("Oct $day should not have emballages", WasteType.EMBALLAGES !in types)
            assertTrue("Oct $day should not have ordures", WasteType.ORDURES !in types)
        }
    }

    @Test
    fun november30HasNoVerreFromPdfBackground() {
        val types = typesOn(LocalDate.of(2026, 11, 30), parse2026())
        assertTrue(types.isEmpty())
    }

    @Test
    fun november4HasVerreOnCollectionDay() {
        val types = typesOn(LocalDate.of(2026, 11, 4), parse2026())
        assertTrue(WasteType.EMBALLAGES in types)
        assertTrue(WasteType.ORDURES in types)
        assertTrue(WasteType.VERRE in types)
    }

    @Test
    fun december2HasVerreWithEmballagesAndOrdures() {
        val types = typesOn(LocalDate.of(2026, 12, 2), parse2026())
        assertTrue(WasteType.EMBALLAGES in types)
        assertTrue(WasteType.ORDURES in types)
        assertTrue(WasteType.VERRE in types)
    }

    @Test
    fun wednesdaysInOctoberHaveRegularCollections() {
        val events = parse2026()
        listOf(7, 14, 21, 28).forEach { day ->
            val types = typesOn(LocalDate.of(2026, 10, day), events)
            assertTrue("Oct $day", WasteType.EMBALLAGES in types)
            assertTrue("Oct $day", WasteType.ORDURES in types)
        }
    }

    @Test
    fun bouconvillersCommuneUsesCcvtSource() {
        val commune = VexinCommunes.bySlug("bouconvillers")
        assertNotNull(commune)
        assertTrue(commune!!.usesCcvtCalendarSource)
    }
}
