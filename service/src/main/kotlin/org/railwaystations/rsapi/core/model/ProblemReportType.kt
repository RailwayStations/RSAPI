package org.railwaystations.rsapi.core.model

enum class ProblemReportType(private val needsPhoto: Boolean) {
    WRONG_LOCATION(false),
    STATION_ACTIVE(false),
    STATION_INACTIVE(false),
    STATION_NONEXISTENT(false),
    WRONG_PHOTO(true),
    PHOTO_OUTDATED(true),
    OTHER(false),
    WRONG_NAME(false),
    DUPLICATE(false);

    fun needsPhoto(): Boolean {
        return needsPhoto
    }
}
