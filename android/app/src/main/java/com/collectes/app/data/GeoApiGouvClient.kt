package com.collectes.app.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

data class GeoCommune(
    val nom: String,
    val codeInsee: String,
    val codeDepartement: String,
    val codesPostaux: List<String>,
    val latitude: Double,
    val longitude: Double
) {
    val subtitle: String
        get() = codesPostaux.firstOrNull().orEmpty()
}

/**
 * Client de l'API Découpage administratif (geo.api.gouv.fr) :
 * recherche par nom / code postal et reverse géocodage lat/lon → commune.
 */
object GeoApiGouvClient {
    private const val BASE = "https://geo.api.gouv.fr/communes"
    private const val FIELDS = "nom,code,codesPostaux,codeDepartement,centre"
    private val postalCodeRegex = Regex("^\\d{5}$")

    /** Recherche unifiée : code postal (5 chiffres) ou nom de commune. */
    fun search(query: String, limit: Int = 12): List<GeoCommune> {
        val trimmed = query.trim()
        if (isPostalCode(trimmed)) return searchByPostalCode(trimmed)
        return searchByName(trimmed, limit)
    }

    fun isPostalCode(query: String): Boolean = postalCodeRegex.matches(query.trim())

    fun searchByPostalCode(codePostal: String): List<GeoCommune> {
        val code = codePostal.trim()
        if (!isPostalCode(code)) return emptyList()
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("codePostal", code)
            .addQueryParameter("fields", FIELDS)
            .build()
        return parseArray(get(url.toString()))
            .sortedBy { it.nom }
    }

    fun searchByName(query: String, limit: Int = 12): List<GeoCommune> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("nom", trimmed)
            .addQueryParameter("fields", FIELDS)
            .addQueryParameter("boost", "population")
            .addQueryParameter("limit", limit.toString())
            .build()
        return parseArray(get(url.toString()))
    }

    fun communeAt(latitude: Double, longitude: Double): GeoCommune? {
        val url = BASE.toHttpUrl().newBuilder()
            .addQueryParameter("lat", latitude.toString())
            .addQueryParameter("lon", longitude.toString())
            .addQueryParameter("fields", FIELDS)
            .addQueryParameter("limit", "1")
            .build()
        return parseArray(get(url.toString())).firstOrNull()
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", SmirtomHttp.USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        SmirtomHttp.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw CalendarFetchException("geo.api.gouv.fr HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    /** Parse léger (sans org.json) pour rester testable en JVM. */
    internal fun parseArray(json: String): List<GeoCommune> {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        if (!trimmed.startsWith("[")) return emptyList()
        return splitTopLevelObjects(trimmed.removePrefix("[").removeSuffix("]"))
            .mapNotNull { parseObject(it) }
    }

    private fun splitTopLevelObjects(body: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escape = false
        body.forEachIndexed { index, c ->
            when {
                escape -> escape = false
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> {
                    if (depth == 0) start = index
                    depth++
                }
                c == '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        result += body.substring(start, index + 1)
                        start = -1
                    }
                }
            }
        }
        return result
    }

    private fun parseObject(obj: String): GeoCommune? {
        val nom = stringField(obj, "nom")?.ifBlank { null } ?: return null
        val code = stringField(obj, "code")?.ifBlank { null } ?: return null
        val dept = stringField(obj, "codeDepartement").orEmpty()
        val cps = stringArrayField(obj, "codesPostaux")
        // GeoJSON centre.coordinates : [longitude, latitude]
        val coords = doubleArrayUnder(obj, "coordinates")
        val lon = coords.getOrNull(0) ?: 0.0
        val lat = coords.getOrNull(1) ?: 0.0
        return GeoCommune(
            nom = nom,
            codeInsee = code,
            codeDepartement = dept,
            codesPostaux = cps,
            latitude = lat,
            longitude = lon
        )
    }

    private fun stringField(obj: String, key: String): String? {
        val regex = """"$key"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        return regex.find(obj)?.groupValues?.get(1)?.unescapeJsonString()
    }

    private fun stringArrayField(obj: String, key: String): List<String> {
        val regex = """"$key"\s*:\s*\[(.*?)\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val inner = regex.find(obj)?.groupValues?.get(1) ?: return emptyList()
        return """"((?:\\.|[^"\\])*)"""".toRegex()
            .findAll(inner)
            .map { it.groupValues[1].unescapeJsonString() }
            .toList()
    }

    private fun doubleArrayUnder(obj: String, key: String): List<Double> {
        val regex = """"$key"\s*:\s*\[([^\]]*)\]""".toRegex()
        val inner = regex.find(obj)?.groupValues?.get(1) ?: return emptyList()
        return inner.split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
    }

    private fun String.unescapeJsonString(): String =
        replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\/", "/")
}
