package org.railwaystations.rsapi.core.model

import java.time.Instant

data class Photo(
    val id: Long = 0,
    val stationKey: Station.Key,
    val primary: Boolean = false,
    val urlPath: String,
    val photographer: User,
    val createdAt: Instant,
    val license: License,
    val outdated: Boolean = false,
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
