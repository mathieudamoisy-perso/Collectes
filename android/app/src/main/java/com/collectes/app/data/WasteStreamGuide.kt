package com.collectes.app.data

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
            WasteGuideTerritory.NCPA -> ncpaGuides
            WasteGuideTerritory.THELLOISE -> thelloiseGuides
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

    private val ncpaGuides: Map<WasteType, WasteStreamGuide> = mapOf(
        WasteType.EMBALLAGES to WasteStreamGuide(
            type = WasteType.EMBALLAGES,
            acceptedItems = listOf(
                "Bouteilles et flacons en plastique",
                "Boîtes métalliques",
                "Briques alimentaires",
                "Petits cartons",
                "Journaux et magazines"
            ),
            rejectedItems = listOf(
                "Verre",
                "Déchets organiques",
                "Emballages souillés non rinçables"
            ),
            tips = listOf(
                "Sacs jaunes disponibles en mairie et à la déchetterie de Périers-en-Auge",
                "Emballages vides ; sortis la veille dès 19h"
            )
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
            ),
            tips = listOf(
                "Apport volontaire uniquement (pas de collecte en porte-à-porte)",
                "Dépôt interdit de 21h à 8h à Cabourg"
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
            ),
            tips = listOf(
                "Sacs verts payants (1 €) ; max 8 sacs par collecte",
                "Disponibles à la déchetterie de Périers-en-Auge"
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
                "1 enlèvement gratuit par an sur rendez-vous (Ressourcerie l’Auguste, Dives-sur-Mer)"
            )
        )
    )

    private val thelloiseGuides: Map<WasteType, WasteStreamGuide> = mapOf(
        WasteType.EMBALLAGES to WasteStreamGuide(
            type = WasteType.EMBALLAGES,
            acceptedItems = listOf(
                "Tous les emballages en plastique",
                "Emballages métalliques",
                "Briques alimentaires",
                "Cartons et papiers",
                "Journaux, revues et magazines"
            ),
            rejectedItems = listOf(
                "Verre",
                "Déchets végétaux",
                "Gravats, peinture, pneus, produits chimiques"
            ),
            tips = listOf(
                "Emballages vidés ; inutile de les laver",
                "En vrac dans le bac jaune, sans les imbriquer"
            )
        ),
        WasteType.ORDURES to WasteStreamGuide(
            type = WasteType.ORDURES,
            acceptedItems = listOf(
                "Déchets ménagers non recyclables",
                "Textiles sanitaires",
                "Objets en plastique non recyclables"
            ),
            rejectedItems = listOf(
                "Emballages recyclables",
                "Verre",
                "Déchets végétaux"
            ),
            tips = listOf(
                "Sortir la veille : 18h (collectif) / 19h (pavillonnaire)",
                "Sacs fermés ; poids inférieur à 25 kg"
            )
        ),
        WasteType.VERRE to WasteStreamGuide(
            type = WasteType.VERRE,
            acceptedItems = listOf(
                "Bouteilles",
                "Pots et bocaux",
                "Flacons"
            ),
            rejectedItems = listOf(
                "Vaisselle et plats en verre",
                "Céramique",
                "Miroirs et bris de glace"
            ),
            tips = listOf(
                "Apport volontaire uniquement (borne à verre sur la commune)",
                "Retirer couvercles, capsules et bouchons"
            )
        ),
        WasteType.VEGETAUX to WasteStreamGuide(
            type = WasteType.VEGETAUX,
            acceptedItems = listOf(
                "Tontes de gazon",
                "Feuilles mortes",
                "Branches (diamètre < 4 cm, longueur max 1,20 m)"
            ),
            rejectedItems = listOf(
                "Fruits, troncs, souches, terre",
                "Branches de plus de 4 cm",
                "Planches de bois"
            ),
            tips = listOf(
                "Volume max 1 m³ par collecte ; poids < 25 kg",
                "Pas de lien plastique ou métal ; ne pas tasser"
            )
        ),
        WasteType.ENCOMBRANTS to WasteStreamGuide(
            type = WasteType.ENCOMBRANTS,
            acceptedItems = listOf(
                "Mobilier d’ameublement démonté",
                "Gros objets non réutilisables"
            ),
            rejectedItems = listOf(
                "Extincteurs, bouteilles de gaz",
                "Amiante, produits explosifs ou radioactifs",
                "Médicaments, DASRI, carcasses de voiture"
            ),
            tips = listOf(
                "Enlèvement sur rendez-vous uniquement (thelloise-encombrants.fr)",
                "Volume limité à 1 m³ ; max 2 m × 1,50 m / 75 kg"
            )
        )
    )
}
