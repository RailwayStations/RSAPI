package org.railwaystations.rsapi.core.model

data class ProblemReport(
    var countryCode: String,
    var stationId: String,
    var title: String? = null,
    var photoId: Long? = null,
    var type: ProblemReportType,
    var comment: String? = null,
    var coordinates: Coordinates? = null,
)
