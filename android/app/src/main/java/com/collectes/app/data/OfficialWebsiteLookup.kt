package com.collectes.app.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/**
 * Sites officiels via Wikidata :
 * - commune : P374 (INSEE) → P856
 * - EPCI : P1616 (SIREN = codeEpci geo.api) → P856
 */
object OfficialWebsiteLookup {
    private const val SPARQL = "https://query.wikidata.org/sparql"

    fun websiteForInsee(codeInsee: String): String? {
        val code = codeInsee.trim()
        if (code.isBlank()) return null
        return sparqlSite(
            """
            SELECT ?site WHERE {
              ?item wdt:P374 "$code" .
              ?item wdt:P856 ?site .
            } LIMIT 1
            """.trimIndent()
        )
    }

    fun websiteForEpci(codeEpci: String): String? {
        val code = codeEpci.trim()
        if (code.isBlank()) return null
        return sparqlSite(
            """
            SELECT ?site WHERE {
              ?item wdt:P1616 "$code" .
              ?item wdt:P856 ?site .
            } LIMIT 1
            """.trimIndent()
        )
    }

    private fun sparqlSite(query: String): String? {
        val url = SPARQL.toHttpUrl().newBuilder()
            .addQueryParameter("format", "json")
            .addQueryParameter("query", query)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", SmirtomHttp.USER_AGENT)
            .header("Accept", "application/sparql-results+json")
            .get()
            .build()
        return runCatching {
            SmirtomHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                extractSiteUri(response.body?.string().orEmpty())
            }
        }.getOrNull()
    }

    internal fun extractSiteUri(json: String): String? {
        val regex = """"value"\s*:\s*"(https?://[^"]+)"""".toRegex()
        return regex.findAll(json)
            .map { it.groupValues[1] }
            .firstOrNull { it.startsWith("http", ignoreCase = true) }
    }
}
