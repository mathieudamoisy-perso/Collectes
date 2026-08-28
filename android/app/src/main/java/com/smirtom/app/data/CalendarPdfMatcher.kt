package com.smirtom.app.data

import java.text.Normalizer
import java.util.Locale

object CalendarPdfMatcher {
    private val STOPWORDS = setOf("en", "sur", "le", "la", "les", "et", "du", "de", "des", "dit")

    private val SITE_ALIASES = mapOf(
        "vienne-en-arthies" to listOf("vienne en athies"),
        "butry-sur-oise" to listOf("butry s oise"),
        "bray-et-lu" to listOf("bray et lu"),
        "saint-clair-sur-epte" to listOf("st clair sur epte"),
        "saint-cyr-en-arthies" to listOf("st cyr en arthies"),
        "saint-gervais" to listOf("st gervais"),
        "epiais-rhus" to listOf("epiais rhus")
    )

    fun matches(haystack: String, year: Int, commune: VexinCommune): Boolean {
        val normalized = normalize(haystack)
        if (!normalized.contains("calendrier")) return false
        if (!normalized.contains(year.toString())) return false
        return matchesCommune(normalized, commune)
    }

    fun matchesCommune(normalizedHaystack: String, commune: VexinCommune): Boolean {
        val keys = communeKeys(commune)
        if (keys.any { normalizedHaystack.contains(it) }) return true

        val tokens = normalize(commune.displayName)
            .split(" ")
            .filter { it !in STOPWORDS && it.length > 2 }
        return tokens.isNotEmpty() && tokens.all { normalizedHaystack.contains(it) }
    }

    fun communeKeys(commune: VexinCommune): List<String> {
        val keys = mutableListOf(
            normalize(commune.displayName),
            normalize(commune.slug)
        )
        SITE_ALIASES[commune.slug]?.let { keys += it }
        return keys.distinct().filter { it.length >= 4 }
    }

    fun normalize(value: String): String {
        val noAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return noAccents
            .lowercase(Locale.FRENCH)
            .replace("saint-", "st ")
            .replace("saint ", "st ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
