package com.smirtom.app.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Element
import java.io.File
import java.time.LocalDate

class SmirtomFetcher {
    companion object {
        const val DOWNLOADS_URL = "https://smirtomduvexin.net/telechargements/"
        const val DOCUMENTATION_URL = "https://smirtomduvexin.net/telechargements/documentation/"
    }

    fun findPdfUrl(year: Int, commune: VexinCommune): String {
        val listingPages = listOf(
            DOCUMENTATION_URL,
            DOWNLOADS_URL,
            "${DOCUMENTATION_URL}page/2/",
            "${DOWNLOADS_URL}page/2/",
            "${DOWNLOADS_URL}page/3/"
        )

        listingPages.forEach { pageUrl ->
            runCatching { findPdfOnPage(pageUrl, year, commune) }.getOrNull()?.let { return it }
        }

        runCatching { findPdfOnPage(commune.pageUrl, year, commune) }.getOrNull()?.let { return it }
        runCatching { findPdfFromMediaApi(year, commune) }.getOrNull()?.let { return it }

        throw CalendarFetchException(
            "Calendrier $year introuvable pour ${commune.displayName}"
        )
    }

    /**
     * URL à ouvrir dans le navigateur (page HTML SMIRTOM de préférence).
     * Ne télécharge aucun fichier dans l'application.
     */
    fun findCalendarUrlForBrowser(year: Int, commune: VexinCommune): String {
        val listingPages = listOf(
            DOCUMENTATION_URL,
            DOWNLOADS_URL,
            "${DOCUMENTATION_URL}page/2/",
            "${DOWNLOADS_URL}page/2/",
            "${DOWNLOADS_URL}page/3/"
        )
        listingPages.forEach { pageUrl ->
            runCatching { findHtmlListingOnPage(pageUrl, year, commune) }.getOrNull()
                ?.let { return it }
        }
        runCatching { findHtmlListingOnPage(commune.pageUrl, year, commune) }.getOrNull()
            ?.let { return it }

        val known = commune.officialCalendarUrl
        if (known.isNotBlank() && !known.contains(".pdf", ignoreCase = true)) {
            return known
        }
        return known.ifBlank { commune.pageUrl }
    }

    private fun findHtmlListingOnPage(pageUrl: String, year: Int, commune: VexinCommune): String? {
        val document = SmirtomHttp.document(pageUrl)
        document.select("a[href]").forEach { link ->
            val href = link.absUrl("href").ifBlank { return@forEach }
            if (!href.contains("/telechargement/", ignoreCase = true)) return@forEach
            if (href.contains(".pdf", ignoreCase = true)) return@forEach
            val haystack = haystackFor(link, href)
            if (CalendarPdfMatcher.matches(haystack, year, commune)) {
                return href
            }
        }
        return null
    }

    private fun findPdfOnPage(pageUrl: String, year: Int, commune: VexinCommune): String? {
        val document = SmirtomHttp.document(pageUrl)

        document.select("a[href]").forEach { link ->
            val href = link.absUrl("href").ifBlank { return@forEach }
            val haystack = haystackFor(link, href)
            if (href.contains(".pdf", ignoreCase = true) &&
                CalendarPdfMatcher.matches(haystack, year, commune)
            ) {
                return href
            }
        }

        document.select("a[href]").forEach { link ->
            val href = link.absUrl("href").ifBlank { return@forEach }
            if (!href.contains("/telechargement/", ignoreCase = true)) return@forEach
            if (href.contains(".pdf", ignoreCase = true)) return@forEach
            val haystack = haystackFor(link, href)
            if (CalendarPdfMatcher.matches(haystack, year, commune)) {
                runCatching { findPdfOnPage(href, year, commune) }.getOrNull()?.let { return it }
            }
        }

        return null
    }

    private fun haystackFor(link: Element, href: String): String {
        return buildString {
            append(link.text()).append(' ')
            append(href).append(' ')
            link.parents().take(8).forEach { parent ->
                append(parent.ownText()).append(' ')
                parent.selectFirst("h1, h2, h3, h4, h5")?.text()?.let { append(it).append(' ') }
            }
        }
    }

    private fun findPdfFromMediaApi(year: Int, commune: VexinCommune): String? {
        val url = "https://smirtomduvexin.net/wp-json/wp/v2/media".toHttpUrl().newBuilder()
            .addQueryParameter("search", "Calendrier ${commune.displayName}")
            .addQueryParameter("per_page", "20")
            .build()

        val body = SmirtomHttp.client.newCall(
            Request.Builder()
                .url(url)
                .header("User-Agent", SmirtomHttp.USER_AGENT)
                .get()
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string().orEmpty()
        }

        val sourceUrls = Regex(""""source_url"\s*:\s*"(https:[^"]+\.pdf)"""", RegexOption.IGNORE_CASE)
            .findAll(body)
            .map { it.groupValues[1].replace("\\/", "/") }
            .toList()

        return sourceUrls.firstOrNull { CalendarPdfMatcher.matches(it, year, commune) }
    }

    fun downloadPdf(url: String, targetFile: File): File {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", SmirtomHttp.USER_AGENT)
            .get()
            .build()
        SmirtomHttp.client.newCall(request).execute().use { response ->
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
