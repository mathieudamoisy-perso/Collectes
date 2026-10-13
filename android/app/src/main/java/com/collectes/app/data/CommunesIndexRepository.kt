package com.collectes.app.data

import android.content.Context
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Index national des communes couvertes.
 * - fallback assets (offline / avant push GitHub)
 * - cache local
 * - remote GitHub raw (mise à jour sans republier l’APK)
 */
class CommunesIndexRepository(private val context: Context) {
    fun ensureLoaded(forceRefresh: Boolean = false): CommunesIndex {
        if (!forceRefresh) {
            cached.get()?.let { return it }
        }
        val bundled = loadBundled()
        val cachedFile = loadCachedFile()
        val bestLocal = newer(cachedFile, bundled) ?: bundled ?: emptyIndex()
        cached.set(bestLocal)

        val remote = runCatching { fetchRemote() }.getOrNull()
        if (remote != null && remote.version >= bestLocal.version && remote.communes.isNotEmpty()) {
            saveCache(remote)
            cached.set(remote)
            return remote
        }
        return bestLocal
    }

    fun peek(): CommunesIndex? = cached.get()

    fun findByInsee(codeInsee: String): IndexedCommune? {
        val index = cached.get() ?: ensureLoaded()
        return index.byInsee(codeInsee)
    }

    private fun loadBundled(): CommunesIndex? = runCatching {
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            .let(CommunesIndexParser::parse)
    }.getOrNull()

    private fun loadCachedFile(): CommunesIndex? {
        val file = cacheFile()
        if (!file.exists()) return null
        return runCatching {
            CommunesIndexParser.parse(file.readText())
        }.getOrNull()
    }

    private fun fetchRemote(): CommunesIndex {
        val request = Request.Builder()
            .url(REMOTE_URL)
            .header("User-Agent", SmirtomHttp.USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        SmirtomHttp.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw CalendarFetchException("Index communes HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            return CommunesIndexParser.parse(body)
        }
    }

    private fun saveCache(index: CommunesIndex) {
        runCatching {
            // On resérialise simplement le JSON distant déjà validé via le fichier cache précédent :
            // ici on réécrit depuis le remote body n’est plus dispo — on garde le parse round-trip minimal.
            cacheFile().writeText(toJson(index))
        }
    }

    private fun cacheFile(): File = File(context.filesDir, CACHE_NAME)

    private fun newer(a: CommunesIndex?, b: CommunesIndex?): CommunesIndex? = when {
        a == null -> b
        b == null -> a
        a.version >= b.version -> a
        else -> b
    }

    private fun emptyIndex(): CommunesIndex =
        CommunesIndex(version = 0, updatedAt = "", communes = emptyList())

    /** Sérialisation minimale pour le cache disque. */
    internal fun toJson(index: CommunesIndex): String {
        val items = index.communes.joinToString(",\n") { c ->
            buildString {
                append("    {")
                append("\"insee\":").append(jsonString(c.insee)).append(',')
                append("\"slug\":").append(jsonString(c.slug)).append(',')
                append("\"name\":").append(jsonString(c.name)).append(',')
                append("\"source\":").append(jsonString(c.source)).append(',')
                if (c.calendarUrl.isNotBlank()) {
                    append("\"calendarUrl\":").append(jsonString(c.calendarUrl)).append(',')
                }
                if (c.infoPageUrl != null) {
                    append("\"infoPageUrl\":").append(jsonString(c.infoPageUrl)).append(',')
                }
                append("\"lat\":").append(c.lat).append(',')
                append("\"lon\":").append(c.lon)
                append('}')
            }
        }
        return """
            |{
            |  "version": ${index.version},
            |  "updatedAt": ${jsonString(index.updatedAt)},
            |  "communes": [
            |$items
            |  ]
            |}
        """.trimMargin()
    }

    private fun jsonString(value: String): String =
        "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n") + "\""

    companion object {
        const val ASSET_NAME = "communes-index.json"
        const val CACHE_NAME = "communes-index-cache.json"
        const val REMOTE_URL =
            "https://raw.githubusercontent.com/mathieudamoisy-perso/Collectes/main/data/communes-index.json"

        private val cached = AtomicReference<CommunesIndex?>(null)

        @Volatile
        private var singleton: CommunesIndexRepository? = null

        fun get(context: Context): CommunesIndexRepository {
            return singleton ?: synchronized(this) {
                singleton ?: CommunesIndexRepository(context.applicationContext).also {
                    singleton = it
                }
            }
        }
    }
}
