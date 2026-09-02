package com.smirtom.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackHelperTest {
    @Test
    fun buildTechnicalInfoIncludesDiagnosticsOnly() {
        val info = FeedbackHelper.buildTechnicalInfo(
            appVersion = "1.2.3",
            communeName = "Cormeilles-en-Vexin",
            androidRelease = "14",
            androidSdk = 34,
            deviceManufacturer = "Google",
            deviceModel = "Pixel 8"
        )

        assertTrue(info.contains("Collectes v1.2.3"))
        assertTrue(info.contains("Cormeilles-en-Vexin"))
        assertTrue(info.contains("Android : 14 (API 34)"))
        assertTrue(info.contains("Appareil : Google Pixel 8"))
        assertTrue(!info.contains("Bonjour"))
        assertTrue(!info.contains("[Décrivez votre message ici]"))
    }

    @Test
    fun buildWhatsAppBodyLeavesRoomForMessageAboveTechnicalInfo() {
        val body = FeedbackHelper.buildWhatsAppBody(
            appVersion = "1.2.3",
            communeName = "Cormeilles-en-Vexin",
            androidRelease = "14",
            androidSdk = 34,
            deviceManufacturer = "Google",
            deviceModel = "Pixel 8"
        )

        assertTrue(body.startsWith("\n\n---\n"))
        assertTrue(body.contains("Collectes v1.2.3"))
        assertTrue(body.contains("Cormeilles-en-Vexin"))
        assertTrue(!body.contains("Bonjour"))
        assertTrue(!body.contains("[Décrivez votre message ici]"))
    }

    @Test
    fun buildFeedbackBodyLeavesRoomForMessageAboveTechnicalInfo() {
        val body = FeedbackHelper.buildFeedbackBody(
            appVersion = "1.2.3",
            communeName = "Cormeilles-en-Vexin",
            androidRelease = "14",
            androidSdk = 34,
            deviceManufacturer = "Google",
            deviceModel = "Pixel 8"
        )

        assertTrue(body.startsWith("\n\n---\n"))
        assertTrue(body.contains("Collectes v1.2.3"))
        assertTrue(body.contains("Cormeilles-en-Vexin"))
        assertTrue(!body.contains("Bonjour"))
        assertTrue(!body.contains("[Décrivez votre message ici]"))
        assertEquals(body, FeedbackHelper.buildWhatsAppBody(
            appVersion = "1.2.3",
            communeName = "Cormeilles-en-Vexin",
            androidRelease = "14",
            androidSdk = 34,
            deviceManufacturer = "Google",
            deviceModel = "Pixel 8"
        ))
    }

    @Test
    fun buildWhatsAppUrlUsesWaMeFormat() {
        val url = FeedbackHelper.buildWhatsAppUrl(
            phoneE164 = "33769399285",
            message = "Bonjour"
        )

        assertEquals("https://wa.me/33769399285?text=Bonjour", url)
    }
}
