package com.collectes.app.data

import java.text.Collator
import java.util.Locale

data class VexinCommune(
    val slug: String,
    val displayName: String,
    val officialCalendarUrl: String = "",
    /** Page d'info collecte ; par défaut la fiche SMIRTOM du Vexin. */
    val infoPageUrl: String? = null,
    /** Centre approximatif de la commune (WGS84), pour la suggestion géolocalisée. */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    val pageUrl: String
        get() = infoPageUrl ?: "https://smirtomduvexin.net/informations_utiles/$slug/"

    /** Commune couverte par le SMIRTOM du Vexin (sinon source externe, ex. mairie). */
    val usesSmirtomNetwork: Boolean
        get() = infoPageUrl == null

    val guideTerritory: WasteGuideTerritory
        get() = when {
            usesSmirtomNetwork -> WasteGuideTerritory.SMIRTOM_VEXIN
            usesNcpaCalendarSource -> WasteGuideTerritory.NCPA
            usesThelloiseCalendarSource -> WasteGuideTerritory.THELLOISE
            else -> WasteGuideTerritory.SYNDICAT_EMERAUDE
        }

    val usesEmeraudeCalendarSource: Boolean
        get() = !usesSmirtomNetwork && (
            officialCalendarUrl.contains("/EMERAUDE/", ignoreCase = true) ||
                officialCalendarUrl.contains("syndicat-emeraude", ignoreCase = true)
            )

    val usesSannoisMunicipalSource: Boolean
        get() = officialCalendarUrl.contains("ville-sannois.fr", ignoreCase = true)

    val usesNcpaCalendarSource: Boolean
        get() = officialCalendarUrl.contains("normandiecabourgpaysdauge.fr", ignoreCase = true)

    val usesThelloiseCalendarSource: Boolean
        get() = officialCalendarUrl.contains("thelloise.fr", ignoreCase = true)

    val usesCcvtCalendarSource: Boolean
        get() = officialCalendarUrl.contains("vexinthelle.fr", ignoreCase = true)

    /** Sous-titre du lien « Calendrier officiel » dans les réglages. */
    fun officialCalendarSubtitle(): String = when {
        usesEmeraudeCalendarSource -> "PDF Syndicat Emeraude — $displayName"
        usesSannoisMunicipalSource -> "PDF ville de Sannois — $displayName"
        usesNcpaCalendarSource -> "PDF Normandie Cabourg Pays d’Auge — $displayName"
        usesThelloiseCalendarSource -> "PDF Communauté de communes Thelloise — $displayName"
        else -> "PDF calendrier officiel — $displayName"
    }

    /** Titre du bandeau source dans le guide du tri. */
    fun guideSourceTitle(): String = when {
        usesEmeraudeCalendarSource -> WasteGuideTerritory.SYNDICAT_EMERAUDE.displayName
        usesSannoisMunicipalSource -> "Ville de Sannois"
        usesNcpaCalendarSource -> WasteGuideTerritory.NCPA.displayName
        usesThelloiseCalendarSource -> WasteGuideTerritory.THELLOISE.displayName
        usesSmirtomNetwork -> "Communes du Vexin"
        else -> displayName
    }

    fun guideSourceSubtitle(): String? = when {
        usesSmirtomNetwork ->
            "Règles indicatives — consultez smirtomduvexin.net pour la source officielle"
        usesEmeraudeCalendarSource ->
            "Règles indicatives — consultez syndicat-emeraude.fr pour la source officielle"
        usesSannoisMunicipalSource ->
            "Règles indicatives — consultez ville-sannois.fr pour la source officielle"
        usesNcpaCalendarSource ->
            "Règles indicatives — consultez normandiecabourgpaysdauge.fr pour la source officielle"
        usesThelloiseCalendarSource ->
            "Règles indicatives — consultez thelloise.fr pour la source officielle"
        infoPageUrl != null ->
            "Règles indicatives — consultez le site de $displayName pour la source officielle"
        else -> null
    }

    fun guideInfoUrl(): String = when {
        usesSmirtomNetwork -> pageUrl
        usesEmeraudeCalendarSource -> WasteGuideTerritory.SYNDICAT_EMERAUDE.infoUrl
        usesNcpaCalendarSource -> WasteGuideTerritory.NCPA.infoUrl
        usesThelloiseCalendarSource -> WasteGuideTerritory.THELLOISE.infoUrl
        else -> infoPageUrl ?: pageUrl
    }

    fun guideInfoLinkLabel(): String = when {
        usesSmirtomNetwork -> "En savoir plus sur le site officiel"
        usesEmeraudeCalendarSource -> "En savoir plus sur syndicat-emeraude.fr"
        usesSannoisMunicipalSource -> "En savoir plus sur ville-sannois.fr"
        usesNcpaCalendarSource -> "En savoir plus sur normandiecabourgpaysdauge.fr"
        usesThelloiseCalendarSource -> "En savoir plus sur thelloise.fr"
        else -> "Page déchets de $displayName"
    }

    fun guideSecondaryInfoUrl(): String? = when {
        usesEmeraudeCalendarSource || usesNcpaCalendarSource || usesThelloiseCalendarSource ->
            infoPageUrl
        else -> null
    }

    fun guideSecondaryInfoLinkLabel(): String? =
        guideSecondaryInfoUrl()?.let { "Page déchets de $displayName" }

}

object VexinCommunes {
    private val displayNameOrder = Collator.getInstance(Locale.FRENCH).apply {
        strength = Collator.PRIMARY
    }

    private val allCommunes = listOf(
        VexinCommune(
            slug = nameToSlug("Blaincourt-lès-Précy"),
            displayName = "Blaincourt-lès-Précy",
            officialCalendarUrl =
                "https://www.thelloise.fr/images/documents/fichiers/calendrier-collecte-2026cctcompressed.pdf",
            infoPageUrl = "https://www.blaincourtlesprecy.fr/informations-pratiques-2/",
            latitude = 49.2306,
            longitude = 2.3553
        ),
        VexinCommune(
            slug = nameToSlug("Bouconvillers"),
            displayName = "Bouconvillers",
            officialCalendarUrl =
                "https://vexinthelle.fr/wp-content/uploads/2026/01/BOUCONVILLERS-2026.pdf",
            infoPageUrl = "https://bouconvillers.fr/vie-pratique/environnement/le-tri-selectif-2/",
            latitude = 49.1850,
            longitude = 1.9150
        ),
        VexinCommune(
            slug = nameToSlug("Cabourg"),
            displayName = "Cabourg",
            officialCalendarUrl =
                "https://www.normandiecabourgpaysdauge.fr/app/uploads/2026/03/Cabourg-Calendrier-de-collecte-2026.pdf",
            infoPageUrl = "https://www.cabourg.fr/habiter/votre-quotidien/environnement/dechets/",
            latitude = 49.2883,
            longitude = -0.1164
        ),
        VexinCommune(
            slug = nameToSlug("Cormeilles-en-Vexin"),
            displayName = "Cormeilles-en-Vexin",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-13-Cormeilles-Epiais.pdf",
            latitude = 49.0942,
            longitude = 1.9925
        ),
        VexinCommune(
            slug = nameToSlug("Épiais-Rhus"),
            displayName = "Épiais-Rhus",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-13-Cormeilles-Epiais.pdf",
            latitude = 49.1225,
            longitude = 2.0200
        ),
        VexinCommune(
            slug = nameToSlug("Ermont"),
            displayName = "Ermont",
            officialCalendarUrl =
                "https://www.ermont.fr/Statics/Actualites/2026/EMERAUDE/Calendrier_collecte_2026.pdf",
            infoPageUrl = "https://www.ermont.fr/195/dechets-menagers.htm",
            latitude = 48.9892,
            longitude = 2.2581
        ),
        VexinCommune(
            slug = nameToSlug("Magny-en-Vexin"),
            displayName = "Magny-en-Vexin",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-01-Magny-en-vexin-Charmont.pdf",
            latitude = 49.1547,
            longitude = 1.7872
        ),
        VexinCommune(
            slug = nameToSlug("Sannois"),
            displayName = "Sannois",
            officialCalendarUrl =
                "https://www.ville-sannois.fr/sites/sannois/files/document/2026-01/calendrier-2026-sannois.pdf",
            infoPageUrl = "https://www.ville-sannois.fr/media/10163",
            latitude = 48.9719,
            longitude = 2.2569
        ),
        VexinCommune(
            slug = nameToSlug("Théméricourt"),
            displayName = "Théméricourt",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-09-Avernes-Themericourt-et-Wy.pdf",
            latitude = 49.0869,
            longitude = 1.8964
        )
    ).sortedWith(compareBy(displayNameOrder) { it.displayName })

    val all: List<VexinCommune> = allCommunes

    /** Fallback interne uniquement (jamais imposé à l'utilisateur sans choix). */
    val default: VexinCommune = allCommunes.first()

    fun bySlug(slug: String): VexinCommune? {
        return allCommunes.find { it.slug.equals(normalizeSlug(slug), ignoreCase = true) }
    }

    /** Anciens slugs conservés pour les préférences déjà enregistrées. */
    fun normalizeSlug(slug: String): String = when (slug.lowercase(Locale.FRENCH)) {
        "ermont-eaubonne" -> "ermont"
        else -> slug
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
