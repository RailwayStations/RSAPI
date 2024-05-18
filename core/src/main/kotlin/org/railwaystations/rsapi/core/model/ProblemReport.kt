package org.railwaystations.rsapi.core.model

data class ProblemReport(
    val countryCode: String,
    val stationId: String,
    val title: String? = null,
    val photoId: Long? = null,
    val type: ProblemReportType,
    val comment: String? = null,
    val coordinates: Coordinates? = null,
)
