package org.railwaystations.rsapi.core.model

import org.railwaystations.rsapi.core.model.PhotoTestFixtures.createPhoto

object StationTestFixtures {
    val keyDe8000 = Station.Key("de", "8000")
    val keyDe8001 = Station.Key("de", "8001")
    val keyDe5 = Station.Key("de", "5")

    val stationDe8000 =
        createStationWithKey(keyDe8000, "Offenburg Kreisschulzentrum", Coordinates(48.459376429721, 7.95547485351562))
    val stationDe8001 =
        createStationWithKey(keyDe8001, "Bretten-Rechberg", Coordinates(49.0330697651661, 8.70151340961456))

    private fun createStationWithKey(
        key: Station.Key,
        title: String = "${key.country} ${key.id}",
        coordinates: Coordinates = Coordinates(50.0, 9.0)
    ): Station = Station(
        key = key,
        title = title,
        coordinates = coordinates
    )

    val stationDe5WithPhoto = createStationWithKey(keyDe5, "Lummerland", Coordinates(50.0, 9.0)).copy(
        ds100 = "XYZ",
        photos = listOf(createPhoto(keyDe5, UserTestFixtures.userJimKnopf))
    )

    fun createStation(key: Station.Key, coordinates: Coordinates, photo: Photo?): Station =
        createStationWithKey(key = key, coordinates = coordinates).copy(
            ds100 = "LAL",
            photos = if (photo != null) listOf(photo) else listOf()
        )
}