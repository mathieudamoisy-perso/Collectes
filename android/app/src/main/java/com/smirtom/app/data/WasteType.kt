package com.smirtom.app.data

enum class WasteType(val label: String, val colorName: String) {
    ORDURES("Ordures ménagères", "gris"),
    EMBALLAGES("Emballages / papiers", "jaune"),
    VERRE("Verre", "vert");

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
    val verreAnchor: java.time.LocalDate
)
