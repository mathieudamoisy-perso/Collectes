package com.smirtom.app.data

data class VexinCommune(
    val slug: String,
    val displayName: String,
    val officialCalendarUrl: String = "",
    /** Page d'info collecte ; par défaut la fiche SMIRTOM du Vexin. */
    val infoPageUrl: String? = null
) {
    val pageUrl: String
        get() = infoPageUrl ?: "https://smirtomduvexin.net/informations_utiles/$slug/"

    /** Commune couverte par le SMIRTOM du Vexin (sinon source externe, ex. mairie). */
    val usesSmirtomNetwork: Boolean
        get() = infoPageUrl == null

    /** Sous-titre du lien « Calendrier officiel » dans les réglages. */
    fun officialCalendarSubtitle(): String = if (usesSmirtomNetwork) {
        "PDF SMIRTOM du Vexin — $displayName"
    } else {
        "PDF officiel de $displayName"
    }

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
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-01-Magny-en-vexin-Charmont.pdf"
        ),
        VexinCommune(
            slug = nameToSlug("Théméricourt"),
            displayName = "Théméricourt",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-09-Avernes-Themericourt-et-Wy.pdf"
        ),
        VexinCommune(
            slug = nameToSlug("Cormeilles-en-Vexin"),
            displayName = "Cormeilles-en-Vexin",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-13-Cormeilles-Epiais.pdf"
        ),
        VexinCommune(
            slug = nameToSlug("Sannois"),
            displayName = "Sannois",
            officialCalendarUrl =
                "https://www.ville-sannois.fr/sites/sannois/files/document/2026-01/calendrier-2026-sannois.pdf",
            infoPageUrl = "https://www.ville-sannois.fr/media/10163"
        ),
        VexinCommune(
            slug = nameToSlug("Ermont-Eaubonne"),
            displayName = "Ermont-Eaubonne",
            officialCalendarUrl =
                "https://www.ermont.fr/Statics/Actualites/2026/EMERAUDE/Calendrier_collecte_2026.pdf",
            infoPageUrl = "https://www.ermont.fr/195/dechets-menagers.htm"
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
