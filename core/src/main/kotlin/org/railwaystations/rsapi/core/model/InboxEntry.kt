package org.railwaystations.rsapi.core.model

import java.time.Instant

data class InboxEntry(
    val id: Long = 0,
    val countryCode: String? = null,
    val stationId: String? = null,
    val photoId: Long? = null,
    val title: String? = null,
    val newTitle: String? = null,
    val coordinates: Coordinates? = null,
    val newCoordinates: Coordinates? = null,
    val photographerId: Int = 0,
    val photographerNickname: String? = null,
    val photographerEmail: String? = null,
    val extension: String? = null,
    val comment: String? = null,
    val rejectReason: String? = null,
    val createdAt: Instant? = null,
    val done: Boolean = false,
    val existingPhotoUrlPath: String? = null,
    val crc32: Long? = null,
    val conflict: Boolean = false,
    val problemReportType: ProblemReportType? = null,
    val processed: Boolean = false,
    val inboxUrl: String? = null,
    val ds100: String? = null,
    val active: Boolean? = null,
    val createStation: Boolean? = null,
    val notified: Boolean = false,
    val posted: Boolean = false,
) {

    val lat: Double?
        get() = coordinates?.lat

    val lon: Double?
        get() = coordinates?.lon

    val newLat: Double?
        get() = newCoordinates?.lat

    val newLon: Double?
        get() = newCoordinates?.lon

    val isPhotoUpload: Boolean
        get() = problemReportType == null && extension != null

    val isProblemReport: Boolean
        get() = problemReportType != null

    val filename: String?
        get() = extension?.let { createFilename(id, it) }

    val hasPhoto: Boolean
        get() = existingPhotoUrlPath != null

    companion object {

        fun createFilename(id: Long, extension: String): String {
            return "$id.$extension"
        }
    }
}
