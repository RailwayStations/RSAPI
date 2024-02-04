package org.railwaystations.rsapi.core.model

class StationTestFixtures {
    companion object {
        fun createStationDe4711(): Station {
            val key = Station.Key("de", "4711")
            return createStationWithKey(key)
        }

        fun createStationDe0815(): Station {
            val key = Station.Key("de", "0815")
            return createStationWithKey(key)
        }

        private fun createStationWithKey(key: Station.Key): Station {
            return Station(
                key = key,
                title = "${key.country} ${key.id}",
            )
        }

        fun createStationDE5(): Station {
            val keyDE5 = Station.Key("de", "5")
            val stationDE5 = createStationWithKey(keyDE5).copy(
                title = "Lummerland",
                coordinates = Coordinates(50.0, 9.0),
                ds100 = "XYZ",
            )
            stationDE5.photos.add(
                PhotoTestFixtures.createPhoto(keyDE5, UserTestFixtures.createUserJimKnopf())
            )
            return stationDE5
        }

        fun createStation(key: Station.Key, coordinates: Coordinates, photo: Photo?): Station {
            val station = createStationWithKey(key).copy(
                coordinates = coordinates,
                ds100 = "LAL",
            )
            if (photo != null) {
                station.photos.add(photo)
            }
            return station
        }
    }
}