package com.smirtom.app.util

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
        deviceModel: String = Build.MODEL
    ): String = """
        • Application : Collectes v$appVersion
        • Commune : $communeName
        • Android : $androidRelease (API $androidSdk)
        • Appareil : $deviceManufacturer $deviceModel
    """.trimIndent()

    fun buildWhatsAppBody(
        appVersion: String,
        communeName: String,
        androidRelease: String = Build.VERSION.RELEASE,
        androidSdk: Int = Build.VERSION.SDK_INT,
        deviceManufacturer: String = Build.MANUFACTURER,
        deviceModel: String = Build.MODEL
    ): String = buildFeedbackBody(
        appVersion,
        communeName,
        androidRelease,
        androidSdk,
        deviceManufacturer,
        deviceModel
    )

    fun buildFeedbackBody(
        appVersion: String,
        communeName: String,
        androidRelease: String = Build.VERSION.RELEASE,
        androidSdk: Int = Build.VERSION.SDK_INT,
        deviceManufacturer: String = Build.MANUFACTURER,
        deviceModel: String = Build.MODEL
    ): String = "\n\n---\n${buildTechnicalInfo(
        appVersion,
        communeName,
        androidRelease,
        androidSdk,
        deviceManufacturer,
        deviceModel
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

    fun openDeveloperEmail(
        context: Context,
        recipient: String,
        subject: String,
        appVersion: String,
        communeName: String
    ): Result<Unit> {
        val body = buildFeedbackBody(appVersion, communeName)
        val mailtoIntent = Intent(
            Intent.ACTION_SENDTO,
            buildMailtoUri(recipient, subject, body)
        )
        if (mailtoIntent.resolveActivity(context.packageManager) != null) {
            return runCatching { context.startActivity(mailtoIntent) }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (sendIntent.resolveActivity(context.packageManager) == null) {
            return Result.failure(IllegalStateException("No email app available"))
        }
        return runCatching {
            context.startActivity(Intent.createChooser(sendIntent, null))
        }
    }

    fun openDeveloperWhatsApp(
        context: Context,
        phoneE164: String,
        appVersion: String,
        communeName: String
    ): Result<Unit> {
        val body = buildWhatsAppBody(appVersion, communeName)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildWhatsAppUrl(phoneE164, body))).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return runCatching { context.startActivity(intent) }
    }
}
