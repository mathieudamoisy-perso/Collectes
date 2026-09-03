package com.smirtom.app.data

import java.text.Collator
import java.util.Locale

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

    val guideTerritory: WasteGuideTerritory
        get() = if (usesSmirtomNetwork) {
            WasteGuideTerritory.SMIRTOM_VEXIN
        } else {
            WasteGuideTerritory.SYNDICAT_EMERAUDE
        }

    val usesEmeraudeCalendarSource: Boolean
        get() = !usesSmirtomNetwork && (
            officialCalendarUrl.contains("/EMERAUDE/", ignoreCase = true) ||
                officialCalendarUrl.contains("syndicat-emeraude", ignoreCase = true)
            )

    val usesSannoisMunicipalSource: Boolean
        get() = officialCalendarUrl.contains("ville-sannois.fr", ignoreCase = true)

    val usesCcvtCalendarSource: Boolean
        get() = officialCalendarUrl.contains("vexinthelle.fr", ignoreCase = true)

    /** Sous-titre du lien « Calendrier officiel » dans les réglages. */
    fun officialCalendarSubtitle(): String = when {
        usesEmeraudeCalendarSource -> "PDF Syndicat Emeraude — $displayName"
        usesSannoisMunicipalSource -> "PDF ville de Sannois — $displayName"
        else -> "PDF calendrier officiel — $displayName"
    }

    /** Titre du bandeau source dans le guide du tri. */
    fun guideSourceTitle(): String = when {
        usesEmeraudeCalendarSource -> WasteGuideTerritory.SYNDICAT_EMERAUDE.displayName
        usesSannoisMunicipalSource -> "Ville de Sannois"
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
        infoPageUrl != null ->
            "Règles indicatives — consultez le site de $displayName pour la source officielle"
        else -> null
    }

    fun guideInfoUrl(): String = when {
        usesSmirtomNetwork -> pageUrl
        usesEmeraudeCalendarSource -> WasteGuideTerritory.SYNDICAT_EMERAUDE.infoUrl
        else -> infoPageUrl ?: pageUrl
    }

    fun guideInfoLinkLabel(): String = when {
        usesSmirtomNetwork -> "En savoir plus sur le site officiel"
        usesEmeraudeCalendarSource -> "En savoir plus sur syndicat-emeraude.fr"
        usesSannoisMunicipalSource -> "En savoir plus sur ville-sannois.fr"
        else -> "Page déchets de $displayName"
    }

    fun guideSecondaryInfoUrl(): String? =
        if (usesEmeraudeCalendarSource) infoPageUrl else null

    fun guideSecondaryInfoLinkLabel(): String? =
        guideSecondaryInfoUrl()?.let { "Page déchets de $displayName" }

    /** Termes pour retrouver le PDF calendrier sur le site SMIRTOM. */
    fun pdfSearchTerms(): List<String> = listOf(
        displayName,
        displayName.replace("-", " "),
        slug.replace("-", " ")
    )
}

object VexinCommunes {
    private val displayNameOrder = Collator.getInstance(Locale.FRENCH).apply {
        strength = Collator.PRIMARY
    }

    private val allCommunes = listOf(
        VexinCommune(
            slug = nameToSlug("Bouconvillers"),
            displayName = "Bouconvillers",
            officialCalendarUrl =
                "https://vexinthelle.fr/wp-content/uploads/2026/01/BOUCONVILLERS-2026.pdf",
            infoPageUrl = "https://bouconvillers.fr/vie-pratique/environnement/le-tri-selectif-2/"
        ),
        VexinCommune(
            slug = nameToSlug("Cormeilles-en-Vexin"),
            displayName = "Cormeilles-en-Vexin",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-13-Cormeilles-Epiais.pdf"
        ),
        VexinCommune(
            slug = nameToSlug("Épiais-Rhus"),
            displayName = "Épiais-Rhus",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-13-Cormeilles-Epiais.pdf"
        ),
        VexinCommune(
            slug = nameToSlug("Ermont"),
            displayName = "Ermont",
            officialCalendarUrl =
                "https://www.ermont.fr/Statics/Actualites/2026/EMERAUDE/Calendrier_collecte_2026.pdf",
            infoPageUrl = "https://www.ermont.fr/195/dechets-menagers.htm"
        ),
        VexinCommune(
            slug = nameToSlug("Magny-en-Vexin"),
            displayName = "Magny-en-Vexin",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-01-Magny-en-vexin-Charmont.pdf"
        ),
        VexinCommune(
            slug = nameToSlug("Sannois"),
            displayName = "Sannois",
            officialCalendarUrl =
                "https://www.ville-sannois.fr/sites/sannois/files/document/2026-01/calendrier-2026-sannois.pdf",
            infoPageUrl = "https://www.ville-sannois.fr/media/10163"
        ),
        VexinCommune(
            slug = nameToSlug("Théméricourt"),
            displayName = "Théméricourt",
            officialCalendarUrl =
                "https://smirtomduvexin.net/wp-content/uploads/2026/02/Calendrier-09-Avernes-Themericourt-et-Wy.pdf"
        )
    ).sortedWith(compareBy(displayNameOrder) { it.displayName })

    val all: List<VexinCommune> = allCommunes

    /** Première commune de la liste triée (ordre alphabétique français). */
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
