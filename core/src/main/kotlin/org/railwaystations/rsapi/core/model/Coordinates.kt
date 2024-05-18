package org.railwaystations.rsapi.core.model

import kotlin.math.abs

const val ZERO: Double = 0.0

data class Coordinates(
    val lat: Double = ZERO,
    val lon: Double = ZERO,
) {

    val hasZeroCoords: Boolean
        get() = lat == ZERO && lon == ZERO

    val isValid: Boolean
        get() = abs(lat) < 90 && abs(lon) < 180 && !hasZeroCoords

}
