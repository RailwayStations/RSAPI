package org.railwaystations.rsapi.core.model

import java.time.Instant

data class InboxStateQuery(
    val id: Long,
    val countryCode: String? = null,
    val stationId: String? = null,
    val title: String? = null,
    val coordinates: Coordinates? = null,
    val newTitle: String? = null,
    val newCoordinates: Coordinates? = null,
    val state: InboxState = InboxState.UNKNOWN,
    val comment: String? = null,
    val problemReportType: ProblemReportType? = null,
    val rejectedReason: String? = null,
    val filename: String? = null,
    val inboxUrl: String? = null,
    val crc32: Long? = null,
    val createdAt: Instant? = null,
) {

    enum class InboxState {
        UNKNOWN,
        REVIEW,
        CONFLICT,
        ACCEPTED,
        REJECTED
    }
}
