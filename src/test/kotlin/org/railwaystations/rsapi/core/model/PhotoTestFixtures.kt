package org.railwaystations.rsapi.core.model

import java.time.Instant

class PhotoTestFixtures {
    companion object {
        fun createPhoto(key: Station.Key, user: User): Photo {
            return Photo(
                stationKey = key,
                primary = true,
                urlPath = "/${key.country}/${key.id}.jpg",
                photographer = user,
                createdAt = Instant.now(),
                license = License.CC0_10,
                outdated = false
            )
        }
    }
}