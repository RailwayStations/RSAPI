package org.railwaystations.rsapi.core.model

data class PublicInboxEntry(
    var countryCode: String,
    var stationId: String,
    var title: String,
    var coordinates: Coordinates,
) {

    val lat: Double
        get() = coordinates.lat

    val lon: Double
        get() = coordinates.lon
}
