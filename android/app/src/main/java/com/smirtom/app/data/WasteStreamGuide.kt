package com.smirtom.app.data

data class WasteStreamGuide(
    val type: WasteType,
    val acceptedItems: List<String>,
    val rejectedItems: List<String>,
    val tips: List<String> = emptyList()
)

object WasteStreamGuides {
    private val displayOrder = listOf(
        WasteType.EMBALLAGES,
        WasteType.ORDURES,
        WasteType.VERRE,
        WasteType.VEGETAUX,
        WasteType.ENCOMBRANTS
    )

    fun forTerritory(territory: WasteGuideTerritory): List<WasteStreamGuide> {
        val guides = when (territory) {
            WasteGuideTerritory.SMIRTOM_VEXIN -> smirtomGuides
            WasteGuideTerritory.SYNDICAT_EMERAUDE -> emeraudeGuides
        }
        return displayOrder.mapNotNull { type -> guides[type] }
    }

    fun forCommune(commune: VexinCommune): List<WasteStreamGuide> =
        forTerritory(commune.guideTerritory)

    fun guideFor(territory: WasteGuideTerritory, type: WasteType): WasteStreamGuide? =
        forTerritory(territory).find { it.type == type }

    private val smirtomGuides: Map<WasteType, WasteStreamGuide> = mapOf(
        WasteType.EMBALLAGES to WasteStreamGuide(
            type = WasteType.EMBALLAGES,
            acceptedItems = listOf(
                "Emballages en plastique",
                "Emballages métalliques",
                "Briques alimentaires",
                "Cartonnettes et cartons",
                "Papiers, journaux, revues et magazines"
            ),
            rejectedItems = listOf(
                "Verre",
                "Déchets organiques",
                "Emballages souillés non rinçables"
            ),
            tips = listOf("Les emballages doivent être vides et séparés les uns des autres")
        ),
        WasteType.ORDURES to WasteStreamGuide(
            type = WasteType.ORDURES,
            acceptedItems = listOf(
                "Textiles sanitaires (mouchoirs, couches, lingettes, masques…)",
                "Objets en plastique non recyclables",
                "Vaisselle cassée"
            ),
            rejectedItems = listOf(
                "Emballages recyclables",
                "Verre",
                "Végétaux"
            )
        ),
        WasteType.VERRE to WasteStreamGuide(
            type = WasteType.VERRE,
            acceptedItems = listOf(
                "Bouteilles",
                "Pots et bocaux",
                "Flacons de parfum"
            ),
            rejectedItems = listOf(
                "Bouchons et couvercles métalliques",
                "Porcelaine et céramique",
                "Miroirs et vitres"
            )
        ),
        WasteType.VEGETAUX to WasteStreamGuide(
            type = WasteType.VEGETAUX,
            acceptedItems = listOf(
                "Tontes de gazon",
                "Feuilles mortes",
                "Petites branches"
            ),
            rejectedItems = listOf(
                "Pierres et graviers",
                "Terre",
                "Pots en plastique"
            )
        ),
        WasteType.ENCOMBRANTS to WasteStreamGuide(
            type = WasteType.ENCOMBRANTS,
            acceptedItems = listOf(
                "Meubles",
                "Gros objets non réutilisables"
            ),
            rejectedItems = listOf(
                "Déchets dangereux",
                "Gravats",
                "Électroménager (→ déchèterie)"
            ),
            tips = listOf(
                "À sortir devant votre logement la veille au soir — pas dans un bac",
                "En immeuble, les consignes peuvent différer : consultez votre syndic"
            )
        )
    )

    private val emeraudeGuides: Map<WasteType, WasteStreamGuide> = mapOf(
        WasteType.EMBALLAGES to WasteStreamGuide(
            type = WasteType.EMBALLAGES,
            acceptedItems = listOf(
                "Emballages en plastique",
                "Emballages métalliques",
                "Briques alimentaires",
                "Cartons et papiers",
                "Journaux, revues et magazines"
            ),
            rejectedItems = listOf(
                "Verre",
                "Déchets organiques",
                "Emballages souillés non rinçables"
            ),
            tips = listOf("Les emballages doivent être vides et séparés les uns des autres")
        ),
        WasteType.ORDURES to WasteStreamGuide(
            type = WasteType.ORDURES,
            acceptedItems = listOf(
                "Textiles sanitaires (mouchoirs, couches, lingettes…)",
                "Objets en plastique non recyclables",
                "Vaisselle cassée"
            ),
            rejectedItems = listOf(
                "Emballages recyclables",
                "Verre",
                "Végétaux"
            )
        ),
        WasteType.VERRE to WasteStreamGuide(
            type = WasteType.VERRE,
            acceptedItems = listOf(
                "Bouteilles",
                "Pots et bocaux",
                "Flacons de parfum"
            ),
            rejectedItems = listOf(
                "Bouchons et couvercles métalliques",
                "Porcelaine et céramique",
                "Miroirs et vitres"
            )
        ),
        WasteType.VEGETAUX to WasteStreamGuide(
            type = WasteType.VEGETAUX,
            acceptedItems = listOf(
                "Tontes de gazon",
                "Feuilles mortes",
                "Petites branches"
            ),
            rejectedItems = listOf(
                "Pierres et graviers",
                "Terre",
                "Pots en plastique"
            )
        ),
        WasteType.ENCOMBRANTS to WasteStreamGuide(
            type = WasteType.ENCOMBRANTS,
            acceptedItems = listOf(
                "Meubles",
                "Gros objets non réutilisables"
            ),
            rejectedItems = listOf(
                "Déchets dangereux",
                "Gravats",
                "Électroménager (→ déchèterie du Plessis-Bouchard)"
            ),
            tips = listOf(
                "À sortir devant votre logement la veille au soir — pas dans un bac",
                "Pour tout renseignement : prevention@syndicat-emeraude.fr"
            )
        )
    )
}
