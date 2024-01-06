package org.railwaystations.rsapi.core.model

import java.time.Instant

data class Photo(
    var id: Long = 0,
    var stationKey: Station.Key,
    var primary: Boolean = false,
    var urlPath: String,
    var photographer: User,
    var createdAt: Instant,
    var license: License,
    var outdated: Boolean = false,
) : Comparable<Photo> {

    override fun compareTo(other: Photo): Int {
        if (primary && other.primary) {
            return -1
        }
        if (!primary && other.primary) {
            return 1
        }
        return id.compareTo(other.id)
    }
}
