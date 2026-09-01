package com.smirtom.app.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Vérifie que chaque commune a un calendrier PDF accessible (test réseau de bout en bout).
 */
class OfficialCalendarUrlsTest {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Test
    fun allCommuneCalendarPdfsAreReachable() {
        VexinCommunes.all.forEach { commune ->
            val request = Request.Builder()
                .url(commune.officialCalendarUrl)
                .header("User-Agent", SmirtomHttp.USER_AGENT)
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                assertTrue(
                    "Calendrier ${commune.displayName} injoignable (${response.code}): ${commune.officialCalendarUrl}",
                    response.isSuccessful
                )
                val contentType = response.header("Content-Type").orEmpty()
                assertTrue(
                    "Calendrier ${commune.displayName} n'est pas un PDF ($contentType)",
                    contentType.contains("pdf", ignoreCase = true) ||
                        commune.officialCalendarUrl.endsWith(".pdf", ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun smirtomCommuneInfoPagesAreReachable() {
        VexinCommunes.all
            .filter { it.infoPageUrl == null }
            .forEach { commune ->
                val request = Request.Builder()
                    .url(commune.pageUrl)
                    .header("User-Agent", SmirtomHttp.USER_AGENT)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    assertTrue(
                        "Page ${commune.displayName} injoignable (${response.code}): ${commune.pageUrl}",
                        response.isSuccessful
                    )
                }
            }
    }

    @Test
    fun sannoisInfoPageIsReachable() {
        val sannois = VexinCommunes.bySlug("sannois")!!
        val request = Request.Builder()
            .url(sannois.pageUrl)
            .header("User-Agent", SmirtomHttp.USER_AGENT)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(
                "Page Sannois injoignable (${response.code}): ${sannois.pageUrl}",
                response.isSuccessful
            )
        }
    }
}
