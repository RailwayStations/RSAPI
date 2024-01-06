package org.railwaystations.rsapi.core.model

import java.time.Instant

data class InboxEntry(
    var id: Long = 0,
    var countryCode: String? = null,
    var stationId: String? = null,
    var photoId: Long? = null,
    var title: String? = null,
    var newTitle: String? = null,
    var coordinates: Coordinates? = null,
    var newCoordinates: Coordinates? = null,
    var photographerId: Int = 0,
    var photographerNickname: String? = null,
    var photographerEmail: String? = null,
    var extension: String? = null,
    var comment: String? = null,
    var rejectReason: String? = null,
    var createdAt: Instant? = null,
    var done: Boolean = false,
    var existingPhotoUrlPath: String? = null,
    var crc32: Long? = null,
    var conflict: Boolean = false,
    var problemReportType: ProblemReportType? = null,
    var processed: Boolean = false,
    var inboxUrl: String? = null,
    var ds100: String? = null,
    var active: Boolean? = null,
    var createStation: Boolean? = null,
    var notified: Boolean = false,
    var posted: Boolean = false,
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

    fun hasPhoto(): Boolean {
        return existingPhotoUrlPath != null
    }

    companion object {

        fun createFilename(id: Long, extension: String): String {
            return "$id.$extension"
        }
    }
}
