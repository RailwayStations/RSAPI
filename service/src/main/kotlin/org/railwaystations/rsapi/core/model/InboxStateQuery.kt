package org.railwaystations.rsapi.core.model

import java.time.Instant

class InboxStateQuery(
    var id: Long,
    var countryCode: String? = null,
    var stationId: String? = null,
    var title: String? = null,
    var coordinates: Coordinates? = null,
    var newTitle: String? = null,
    var newCoordinates: Coordinates? = null,
    var state: InboxState = InboxState.UNKNOWN,
    var comment: String? = null,
    var problemReportType: ProblemReportType? = null,
    var rejectedReason: String? = null,
    var filename: String? = null,
    var inboxUrl: String? = null,
    var crc32: Long? = null,
    var createdAt: Instant? = null,
) {

    enum class InboxState {
        UNKNOWN,
        REVIEW,
        CONFLICT,
        ACCEPTED,
        REJECTED
    }
}
