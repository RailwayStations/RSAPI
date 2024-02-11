package org.railwaystations.rsapi.core.model

data class InboxCommand(
    var id: Long = 0,
    var countryCode: String? = null,
    var stationId: String? = null,
    var title: String? = null,
    var coordinates: Coordinates? = null,
    var rejectReason: String? = null,
    var ds100: String? = null,
    var active: Boolean? = null,
    var conflictResolution: ConflictResolution? = null,
) {
    fun hasCoords(): Boolean {
        return coordinates != null && !coordinates!!.hasZeroCoords()
    }

    val lat: Double?
        get() = coordinates?.lat

    val lon: Double?
        get() = coordinates?.lon

    enum class ConflictResolution(
        private val solvesPhotoConflict: Boolean,
        private val solvesStationConflict: Boolean
    ) {
        DO_NOTHING(false, false),
        OVERWRITE_EXISTING_PHOTO(true, false),
        IMPORT_AS_NEW_PRIMARY_PHOTO(true, false),
        IMPORT_AS_NEW_SECONDARY_PHOTO(true, false),
        IGNORE_NEARBY_STATION(false, true);

        fun solvesPhotoConflict(): Boolean {
            return solvesPhotoConflict
        }

        fun solvesStationConflict(): Boolean {
            return solvesStationConflict
        }
    }
}
