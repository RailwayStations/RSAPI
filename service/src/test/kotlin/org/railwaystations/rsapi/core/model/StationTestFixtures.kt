package org.railwaystations.rsapi.core.model

import org.railwaystations.rsapi.core.model.PhotoTestFixtures.Companion.createPhoto

class StationTestFixtures {
    companion object {
        fun createStationDe4711(): Station = createStationWithKey(Station.Key("de", "4711"))

        fun createStationDe0815(): Station = createStationWithKey(Station.Key("de", "0815"))

        private fun createStationWithKey(key: Station.Key): Station = Station(
            key = key,
            title = "${key.country} ${key.id}",
        )

        fun createStationDE5(): Station {
            val keyDE5 = Station.Key("de", "5")
            val stationDE5 = createStationWithKey(keyDE5).copy(
                title = "Lummerland",
                coordinates = Coordinates(50.0, 9.0),
                ds100 = "XYZ",
                photos = listOf(createPhoto(keyDE5, UserTestFixtures.createUserJimKnopf()))
            )
            return stationDE5
        }

        fun createStation(key: Station.Key, coordinates: Coordinates, photo: Photo?): Station =
            createStationWithKey(key).copy(
                coordinates = coordinates,
                ds100 = "LAL",
                photos = if (photo != null) listOf(photo) else listOf()
            )
    }
}