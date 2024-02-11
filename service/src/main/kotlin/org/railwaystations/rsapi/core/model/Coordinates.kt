package org.railwaystations.rsapi.core.model

import kotlin.math.abs

data class Coordinates(
    var lat: Double = 0.0,
    var lon: Double = 0.0,
) {

    constructor() : this(ZERO, ZERO)

    fun hasZeroCoords(): Boolean {
        return lat == ZERO && lon == ZERO
    }

    val isValid: Boolean
        get() = abs(lat) < 90 && abs(lon) < 180 && !hasZeroCoords()

    companion object {
        const val ZERO: Double = 0.0
    }
}
