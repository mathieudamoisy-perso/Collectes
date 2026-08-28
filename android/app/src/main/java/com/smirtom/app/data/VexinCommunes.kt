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
    val default: VexinCommune = bySlug("magny-en-vexin")!!

    private val allCommunes = listOf(
        "Ableiges",
        "Aincourt",
        "Ambleville",
        "Amenucourt",
        "Arronville",
        "Arthies",
        "Avernes",
        "Banthelu",
        "Berville",
        "Boissy l'Aillerie",
        "Bray-et-Lû",
        "Bréançon",
        "Brignancourt",
        "Brueil-en-Vexin",
        "Buhy",
        "Butry-sur-Oise",
        "Charmont",
        "Chars",
        "Chaussy",
        "Chérence",
        "Cléry-en-Vexin",
        "Commeny",
        "Condécourt",
        "Cormeilles-en-Vexin",
        "Courcelles-sur-Viosne",
        "Ennery",
        "Épiais-Rhus",
        "Frémainville",
        "Frémécourt",
        "Gaillon-sur-Montcient",
        "Genainville",
        "Génicourt",
        "Gouzangrez",
        "Grisy-les-Plâtres",
        "Guiry-en-Vexin",
        "Guitrancourt",
        "Haravilliers",
        "Hardricourt",
        "Haute-Isle",
        "Hérouville-en-Vexin",
        "Hodent",
        "Jambville",
        "Juziers",
        "La Chapelle-en-Vexin",
        "La Roche-Guyon",
        "Labbeville",
        "Lainville-en-Vexin",
        "Le Bellay-en-Vexin",
        "Le Heaulme",
        "Le Perchay",
        "Livilliers",
        "Longuesse",
        "Magny-en-Vexin",
        "Marines",
        "Maudétour-en-Vexin",
        "Menouville",
        "Mézy-sur-Seine",
        "Montalet-le-Bois",
        "Montgeroult",
        "Montreuil-sur-Epte",
        "Moussy",
        "Nesles-la-Vallée",
        "Neuilly-en-Vexin",
        "Nucourt",
        "Oinville-sur-Montcient",
        "Omerville",
        "Sagy",
        "Saint-Clair-sur-Epte",
        "Saint-Cyr-en-Arthies",
        "Saint-Gervais",
        "Santeuil",
        "Seraincourt",
        "Tessancourt-sur-Aubette",
        "Théméricourt",
        "Theuville",
        "Us",
        "Vallangoujard",
        "Valmondois",
        "Vétheuil",
        "Vienne-en-Arthies",
        "Vigny",
        "Villers-en-Arthies",
        "Wy-dit-Joli-Village"
    ).map { name ->
        VexinCommune(slug = nameToSlug(name), displayName = name)
    }.sortedBy { it.displayName }

    val all: List<VexinCommune> = allCommunes

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
