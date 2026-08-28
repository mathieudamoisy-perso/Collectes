package com.smirtom.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WasteTypeTest {
    @Test
    fun notificationLabelOmitsColorForEncombrants() {
        assertEquals("Encombrants", WasteType.ENCOMBRANTS.notificationLabel)
    }

    @Test
    fun notificationLabelKeepsBinColorForOtherTypes() {
        assertEquals("Ordures ménagères (gris)", WasteType.ORDURES.notificationLabel)
        assertEquals("Verre (vert)", WasteType.VERRE.notificationLabel)
    }
}
