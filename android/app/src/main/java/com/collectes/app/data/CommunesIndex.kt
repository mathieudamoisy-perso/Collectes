package com.collectes.app.data

data class CommunesIndex(
    val version: Int,
    val updatedAt: String,
    val communes: List<IndexedCommune>
) {
    fun byInsee(code: String): IndexedCommune? =
        communes.find { it.insee.equals(code, ignoreCase = true) }
}

data class IndexedCommune(
    val insee: String,
    val slug: String,
    val name: String,
    /** `smirtom` (réseau SMIRTOM) ou `direct` (PDF / page officielle). */
    val source: String,
    val calendarUrl: String = "",
    val infoPageUrl: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0
) {
    fun toVexinCommune(): VexinCommune {
        val smirtom = source.equals("smirtom", ignoreCase = true)
        return VexinCommune(
            slug = slug.ifBlank { VexinCommunes.nameToSlug(name) },
            displayName = name,
            officialCalendarUrl = calendarUrl,
            infoPageUrl = if (smirtom) null else infoPageUrl,
            latitude = lat,
            longitude = lon
        )
    }

    fun sourceLabel(): String = when {
        source.equals("smirtom", ignoreCase = true) -> "SMIRTOM du Vexin"
        else -> CommuneCoverageResolver.sourceLabelFor(toVexinCommune())
    }
}

object CommunesIndexParser {
    fun parse(json: String): CommunesIndex {
        val version = intField(json, "version") ?: 0
        val updatedAt = stringField(json, "updatedAt").orEmpty()
        val communesBlock = arrayBlock(json, "communes").orEmpty()
        val communes = splitTopLevelObjects(communesBlock).mapNotNull { parseCommune(it) }
        return CommunesIndex(version = version, updatedAt = updatedAt, communes = communes)
    }

    private fun parseCommune(obj: String): IndexedCommune? {
        val insee = stringField(obj, "insee")?.ifBlank { null } ?: return null
        val name = stringField(obj, "name")?.ifBlank { null } ?: return null
        val source = stringField(obj, "source")?.ifBlank { null } ?: "direct"
        val slug = stringField(obj, "slug")?.ifBlank { null } ?: VexinCommunes.nameToSlug(name)
        val calendarUrl = stringField(obj, "calendarUrl").orEmpty()
        val infoPageUrl = stringField(obj, "infoPageUrl")?.ifBlank { null }
        val lat = numberField(obj, "lat") ?: 0.0
        val lon = numberField(obj, "lon") ?: 0.0
        return IndexedCommune(
            insee = insee,
            slug = slug,
            name = name,
            source = source,
            calendarUrl = calendarUrl,
            infoPageUrl = infoPageUrl,
            lat = lat,
            lon = lon
        )
    }

    private fun stringField(obj: String, key: String): String? {
        val regex = """"$key"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        return regex.find(obj)?.groupValues?.get(1)?.unescapeJsonString()
    }

    private fun intField(obj: String, key: String): Int? {
        val regex = """"$key"\s*:\s*(-?\d+)""".toRegex()
        return regex.find(obj)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun numberField(obj: String, key: String): Double? {
        val regex = """"$key"\s*:\s*(-?\d+(?:\.\d+)?)""".toRegex()
        return regex.find(obj)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun arrayBlock(obj: String, key: String): String? {
        val marker = """"$key""""
        val keyIndex = obj.indexOf(marker)
        if (keyIndex < 0) return null
        val colon = obj.indexOf(':', keyIndex + marker.length)
        if (colon < 0) return null
        val start = obj.indexOf('[', colon)
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until obj.length) {
            val c = obj[i]
            when {
                escape -> escape = false
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '[' -> depth++
                c == ']' -> {
                    depth--
                    if (depth == 0) return obj.substring(start + 1, i)
                }
            }
        }
        return null
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

    private fun String.unescapeJsonString(): String =
        replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\/", "/")
}
