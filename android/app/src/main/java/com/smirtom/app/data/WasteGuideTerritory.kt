package com.smirtom.app.data

enum class WasteGuideTerritory(
    val displayName: String,
    val infoUrl: String
) {
    SMIRTOM_VEXIN("SMIRTOM du Vexin", "https://smirtomduvexin.net"),
    SYNDICAT_EMERAUDE("Syndicat Emeraude", "https://www.syndicat-emeraude.fr");

    fun detailUrl(commune: VexinCommune): String = when (this) {
        SMIRTOM_VEXIN -> commune.pageUrl
        SYNDICAT_EMERAUDE -> infoUrl
    }
}
