package com.smirtom.app.data

data class VexinCommune(
    val slug: String,
    val displayName: String
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
        "Ambleville",
        "Arthies",
        "Banthelu",
        "Charmont",
        "Cléry-en-Vexin",
        "Genainville",
        "Hodent",
        "La Chapelle-en-Vexin",
        "Magny-en-Vexin",
        "Maudétour-en-Vexin",
        "Nucourt",
        "Omerville",
        "Saint-Gervais",
        "Wy-dit-Joli-Village"
    ).map { name ->
        VexinCommune(slug = nameToSlug(name), displayName = name)
    }.sortedBy { it.displayName }

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
