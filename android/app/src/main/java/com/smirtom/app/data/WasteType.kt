package com.smirtom.app.data

enum class WasteType(val label: String, val colorName: String) {
    ORDURES("Ordures ménagères", "gris"),
    EMBALLAGES("Emballages / papiers", "jaune"),
    VEGETAUX("Végétaux", "marron"),
    VERRE("Verre", "vert"),
    ENCOMBRANTS("Encombrants", "orange");

    /** Libellé pour les notifications (sans couleur de bac pour les encombrants). */
    val notificationLabel: String
        get() = when (this) {
            ENCOMBRANTS -> label
            else -> "$label ($colorName)"
        }

    /** Sous-titre dans le guide du tri (bac ou consigne de sortie). */
    val guideSubtitle: String
        get() = when (this) {
            ENCOMBRANTS -> "À sortir devant le logement"
            else -> "Bac $colorName"
        }

    /** Libellé dans les listes de collecte (écran d'accueil, notifications visuelles). */
    val collectionLineLabel: String
        get() = when (this) {
            ENCOMBRANTS -> "$label (devant le logement)"
            else -> "$label ($colorName)"
        }

    companion object {
        fun fromStorage(value: String): WasteType? = entries.find { it.name == value }
    }
}

data class CollectionDay(
    val date: java.time.LocalDate,
    val wasteTypes: List<WasteType>
)

data class CollectionRules(
    val orduresDay: java.time.DayOfWeek,
    val emballagesDay: java.time.DayOfWeek,
    val emballagesAnchor: java.time.LocalDate,
    val verreDay: java.time.DayOfWeek,
    val verreAnchor: java.time.LocalDate,
    val orduresRecurrence: CollectionRecurrence = CollectionRecurrence.WEEKLY,
    val emballagesRecurrence: CollectionRecurrence = CollectionRecurrence.BIWEEKLY,
    val verreRecurrence: CollectionRecurrence = CollectionRecurrence.EVERY_FOUR_WEEKS,
    val verreMonthOrdinal: Int? = null,
    val encombrantsDay: java.time.DayOfWeek? = null,
    val encombrantsMonthOrdinal: Int? = null,
    val vegetauxSchedule: VegetauxSchedule? = null
) {
    companion object {
        /** Rythme officiel 2026 de la commune si le site est injoignable. */
        fun fallback(year: Int, communeSlug: String = "magny-en-vexin"): CollectionRules {
            return OfficialCommuneSchedules.rules(year, communeSlug)
        }
    }
}
