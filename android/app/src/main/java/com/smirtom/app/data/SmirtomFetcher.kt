package com.smirtom.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class SmirtomFetcher(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val DOWNLOADS_URL = "https://smirtomduvexin.net/telechargements/"
        const val COMMUNE_SEARCH = "Magny en Vexin"
        const val SECTOR_HINT = "Magny en Vexin"
    }

    suspend fun findPdfUrl(year: Int): String {
        val fromPage = runCatching { findPdfUrlFromDownloadsPage(year) }.getOrNull()
        if (fromPage != null) return fromPage

        val fallback = fallbackPdfUrl(year)
        if (fallback != null && pdfExists(fallback)) return fallback

        throw CalendarFetchException("Calendrier $year introuvable pour $COMMUNE_SEARCH")
    }

    private fun findPdfUrlFromDownloadsPage(year: Int): String {
        val document = Jsoup.connect(DOWNLOADS_URL)
            .userAgent("SmirtomApp/1.0")
            .timeout(30_000)
            .get()

        val yearToken = "Calendrier $year"
        val candidates = document.select("a[href]").mapNotNull { link ->
            val href = link.absUrl("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!href.contains(".pdf", ignoreCase = true)) return@mapNotNull null

            val title = buildString {
                append(link.text())
                link.parent()?.text()?.let { append(" ").append(it) }
                append(" ").append(href)
            }

            if (title.contains(yearToken, ignoreCase = true) &&
                (title.contains(SECTOR_HINT, ignoreCase = true) ||
                    href.contains("Magny", ignoreCase = true))
            ) {
                href
            } else {
                null
            }
        }.distinct()

        return candidates.firstOrNull()
            ?: throw CalendarFetchException("Calendrier $year introuvable sur la page téléchargements")
    }

    private fun fallbackPdfUrl(year: Int): String? {
        return when (year) {
            2026 -> "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-01-Magny-en-vexin-Charmont.pdf"
            else -> null
        }
    }

    private fun pdfExists(url: String): Boolean {
        val request = Request.Builder().url(url).head().build()
        okHttpClient.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }

    fun downloadPdf(url: String, targetFile: File): File {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw CalendarFetchException("Échec du téléchargement (${response.code})")
            }
            val body = response.body ?: throw CalendarFetchException("Réponse vide")
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
        return targetFile
    }

    fun pdfCacheFile(cacheDir: File, year: Int): File {
        return File(cacheDir, "calendar-$year.pdf")
    }

    fun currentYear(): Int = LocalDate.now().year
}

class CalendarFetchException(message: String) : Exception(message)
