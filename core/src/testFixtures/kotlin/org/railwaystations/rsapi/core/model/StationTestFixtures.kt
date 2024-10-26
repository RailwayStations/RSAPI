package org.railwaystations.rsapi.core.model

import org.railwaystations.rsapi.core.model.PhotoTestFixtures.createPhoto

object StationTestFixtures {
    val keyDe4711 = Station.Key("de", "4711")
    val keyDe0815 = Station.Key("de", "0815")
    val keyDe5 = Station.Key("de", "5")

    val stationDe4711 = createStationWithKey(keyDe4711)
    val stationDe0815 = createStationWithKey(keyDe0815)

    private fun createStationWithKey(key: Station.Key): Station = Station(
        key = key,
        title = "${key.country} ${key.id}",
    )

    val stationDe5WithPhoto = createStationWithKey(keyDe5).copy(
        title = "Lummerland",
        coordinates = Coordinates(50.0, 9.0),
        ds100 = "XYZ",
        photos = listOf(createPhoto(keyDe5, UserTestFixtures.createUserJimKnopf()))
    )

    fun createStation(key: Station.Key, coordinates: Coordinates, photo: Photo?): Station =
        createStationWithKey(key).copy(
            coordinates = coordinates,
            ds100 = "LAL",
            photos = if (photo != null) listOf(photo) else listOf()
        )
}