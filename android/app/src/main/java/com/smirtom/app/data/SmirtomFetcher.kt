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
    }

    fun findPdfUrl(year: Int, commune: VexinCommune): String {
        val fromPage = runCatching { findPdfUrlFromDownloadsPage(year, commune) }.getOrNull()
        if (fromPage != null) return fromPage

        throw CalendarFetchException(
            "Calendrier $year introuvable pour ${commune.displayName}"
        )
    }

    private fun findPdfUrlFromDownloadsPage(year: Int, commune: VexinCommune): String {
        val document = Jsoup.connect(DOWNLOADS_URL)
            .userAgent("SmirtomApp/1.0")
            .timeout(30_000)
            .get()

        val yearToken = "Calendrier $year"
        val searchTerms = commune.pdfSearchTerms()

        val candidates = document.select("a[href]").mapNotNull { link ->
            val href = link.absUrl("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!href.contains(".pdf", ignoreCase = true)) return@mapNotNull null

            val title = buildString {
                append(link.text())
                link.parent()?.text()?.let { append(" ").append(it) }
                append(" ").append(href)
            }

            val matchesCommune = searchTerms.any { term ->
                title.contains(term, ignoreCase = true) ||
                    href.contains(term.replace(" ", "-"), ignoreCase = true)
            }

            if (title.contains(yearToken, ignoreCase = true) && matchesCommune) href else null
        }.distinct()

        return candidates.firstOrNull()
            ?: throw CalendarFetchException(
                "Calendrier $year introuvable pour ${commune.displayName}"
            )
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

    fun pdfCacheFile(cacheDir: File, year: Int, communeSlug: String): File {
        return File(cacheDir, "calendar-$year-$communeSlug.pdf")
    }

    fun currentYear(): Int = LocalDate.now().year
}

class CalendarFetchException(message: String) : Exception(message)
