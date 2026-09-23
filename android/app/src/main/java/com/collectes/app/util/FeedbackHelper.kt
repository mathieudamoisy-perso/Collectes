package com.collectes.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object FeedbackHelper {
    fun buildTechnicalInfo(
        appVersion: String,
        communeName: String,
        androidRelease: String = Build.VERSION.RELEASE,
        androidSdk: Int = Build.VERSION.SDK_INT,
        deviceManufacturer: String = Build.MANUFACTURER,
        deviceModel: String = Build.MODEL,
        canPostNotifications: Boolean? = null,
        canScheduleExactAlarms: Boolean? = null,
        ignoringBatteryOptimizations: Boolean? = null,
        communeSetupDone: Boolean? = null
    ): String {
        val lines = mutableListOf(
            "• Application : Collectes v$appVersion",
            "• Commune : $communeName",
            "• Android : $androidRelease (API $androidSdk)",
            "• Appareil : $deviceManufacturer $deviceModel"
        )
        communeSetupDone?.let { lines.add("• Setup commune : ${ouiNon(it)}") }
        canPostNotifications?.let { lines.add("• Notifications : ${ouiNon(it)}") }
        canScheduleExactAlarms?.let { lines.add("• Alarmes exactes : ${ouiNon(it)}") }
        ignoringBatteryOptimizations?.let { lines.add("• Batterie non restreinte : ${ouiNon(it)}") }
        return lines.joinToString("\n")
    }

    fun buildWhatsAppBody(
        appVersion: String,
        communeName: String,
        androidRelease: String = Build.VERSION.RELEASE,
        androidSdk: Int = Build.VERSION.SDK_INT,
        deviceManufacturer: String = Build.MANUFACTURER,
        deviceModel: String = Build.MODEL,
        canPostNotifications: Boolean? = null,
        canScheduleExactAlarms: Boolean? = null,
        ignoringBatteryOptimizations: Boolean? = null,
        communeSetupDone: Boolean? = null
    ): String = buildFeedbackBody(
        appVersion,
        communeName,
        androidRelease,
        androidSdk,
        deviceManufacturer,
        deviceModel,
        canPostNotifications,
        canScheduleExactAlarms,
        ignoringBatteryOptimizations,
        communeSetupDone
    )

    fun buildFeedbackBody(
        appVersion: String,
        communeName: String,
        androidRelease: String = Build.VERSION.RELEASE,
        androidSdk: Int = Build.VERSION.SDK_INT,
        deviceManufacturer: String = Build.MANUFACTURER,
        deviceModel: String = Build.MODEL,
        canPostNotifications: Boolean? = null,
        canScheduleExactAlarms: Boolean? = null,
        ignoringBatteryOptimizations: Boolean? = null,
        communeSetupDone: Boolean? = null
    ): String = "\n\n---\n${buildTechnicalInfo(
        appVersion,
        communeName,
        androidRelease,
        androidSdk,
        deviceManufacturer,
        deviceModel,
        canPostNotifications,
        canScheduleExactAlarms,
        ignoringBatteryOptimizations,
        communeSetupDone
    )}"

    fun buildMailtoUri(recipient: String, subject: String, body: String): Uri =
        Uri.parse("mailto:$recipient").buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()

    fun buildWhatsAppUrl(phoneE164: String, message: String): String =
        "https://wa.me/$phoneE164?text=${encodeUrlComponent(message)}"

    private fun encodeUrlComponent(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private fun ouiNon(value: Boolean): String = if (value) "oui" else "non"

    fun openDeveloperEmail(
        context: Context,
        recipient: String,
        subject: String,
        appVersion: String,
        communeName: String,
        canPostNotifications: Boolean? = null,
        canScheduleExactAlarms: Boolean? = null,
        ignoringBatteryOptimizations: Boolean? = null,
        communeSetupDone: Boolean? = null
    ): Result<Unit> {
        val body = buildFeedbackBody(
            appVersion = appVersion,
            communeName = communeName,
            canPostNotifications = canPostNotifications,
            canScheduleExactAlarms = canScheduleExactAlarms,
            ignoringBatteryOptimizations = ignoringBatteryOptimizations,
            communeSetupDone = communeSetupDone
        )
        val intent = Intent(
            Intent.ACTION_SENDTO,
            buildMailtoUri(recipient, subject, body)
        )
        return runCatching { context.startActivity(intent) }
    }

    fun openDeveloperWhatsApp(
        context: Context,
        phoneE164: String,
        appVersion: String,
        communeName: String,
        canPostNotifications: Boolean? = null,
        canScheduleExactAlarms: Boolean? = null,
        ignoringBatteryOptimizations: Boolean? = null,
        communeSetupDone: Boolean? = null
    ): Result<Unit> {
        val body = buildWhatsAppBody(
            appVersion = appVersion,
            communeName = communeName,
            canPostNotifications = canPostNotifications,
            canScheduleExactAlarms = canScheduleExactAlarms,
            ignoringBatteryOptimizations = ignoringBatteryOptimizations,
            communeSetupDone = communeSetupDone
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildWhatsAppUrl(phoneE164, body))).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return runCatching { context.startActivity(intent) }
    }
}
