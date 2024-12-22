package org.railwaystations.rsapi.core.model

import org.railwaystations.rsapi.core.model.PhotoTestFixtures.createPhoto
import org.railwaystations.rsapi.core.model.UserTestFixtures.user2
import java.time.Instant

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

    val keyCh8503001 = Station.Key("ch", "8503001")

    val stationCh8503001 = createStationWithKey(
        key = keyCh8503001,
        title = "Zürich Altstetten",
        coordinates = Coordinates(lat = 47.3914808361, lon = 8.4889402654),
    ).copy(
        ds100 = "ZAS",
        photos = listOf(
            createPhoto(keyCh8503001, user2).copy(
                id = 1,
                urlPath = "/ch/8503001%20-%20Z%C3%BCrich%20Altstetten.jpg",
                createdAt = Instant.parse("2018-04-06T17:52:47Z"),
                license = License.CC0_10,
            )
        )
    )

    fun createStation(key: Station.Key, coordinates: Coordinates, photo: Photo? = null): Station =
        createStationWithKey(key = key, coordinates = coordinates).copy(
            ds100 = "LAL",
            photos = if (photo != null) listOf(photo) else listOf()
        )
}