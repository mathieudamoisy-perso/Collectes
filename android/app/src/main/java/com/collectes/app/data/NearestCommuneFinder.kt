package com.collectes.app.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NearestCommuneMatch(
    val commune: VexinCommune,
    val distanceKm: Double
)

object NearestCommuneFinder {
    /** Au-delà, on propose quand même la plus proche mais l'UI peut avertir. */
    const val FAR_AWAY_THRESHOLD_KM = 80.0

    fun findNearest(
        latitude: Double,
        longitude: Double,
        communes: List<VexinCommune> = VexinCommunes.all
    ): NearestCommuneMatch? {
        if (communes.isEmpty()) return null
        return communes
            .map { commune ->
                NearestCommuneMatch(
                    commune = commune,
                    distanceKm = haversineKm(
                        latitude,
                        longitude,
                        commune.latitude,
                        commune.longitude
                    )
                )
            }
            .minByOrNull { it.distanceKm }
    }

    fun haversineKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
