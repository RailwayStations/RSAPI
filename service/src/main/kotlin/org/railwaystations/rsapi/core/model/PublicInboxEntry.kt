package org.railwaystations.rsapi.core.model

data class PublicInboxEntry(
    val countryCode: String?,
    val stationId: String?,
    val title: String,
    val coordinates: Coordinates,
) {

    val lat: Double
        get() = coordinates.lat

    val lon: Double
        get() = coordinates.lon
}
