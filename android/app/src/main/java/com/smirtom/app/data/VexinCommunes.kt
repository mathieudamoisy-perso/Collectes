package com.smirtom.app.data

data class VexinCommune(
    val slug: String,
    val displayName: String,
    val officialCalendarUrl: String = ""
) {
    val pageUrl: String
        get() = "https://smirtomduvexin.net/informations_utiles/$slug/"

    /** Termes pour retrouver le PDF calendrier sur le site SMIRTOM. */
    fun pdfSearchTerms(): List<String> = listOf(
        displayName,
        displayName.replace("-", " "),
        slug.replace("-", " ")
    )
}

object VexinCommunes {
    private val allCommunes = listOf(
        VexinCommune(
            slug = nameToSlug("Magny-en-Vexin"),
            displayName = "Magny-en-Vexin",
            officialCalendarUrl =
                "https://smirtomduvexin.net/telechargement/calendrier-2024-magny-en-vexin-charmont/"
        ),
        VexinCommune(
            slug = nameToSlug("Théméricourt"),
            displayName = "Théméricourt",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-09-Avernes-Themericourt-et-Wy.pdf"
        )
    )

    val all: List<VexinCommune> = allCommunes

    val default: VexinCommune = allCommunes.first { it.slug == "magny-en-vexin" }

    fun bySlug(slug: String): VexinCommune? {
        return allCommunes.find { it.slug.equals(slug, ignoreCase = true) }
    }

    fun nameToSlug(name: String): String {
        val normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(java.util.Locale.FRENCH)
            .replace("'", "-")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return normalized
    }
}
