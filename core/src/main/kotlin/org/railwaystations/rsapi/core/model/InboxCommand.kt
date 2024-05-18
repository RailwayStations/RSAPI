package org.railwaystations.rsapi.core.model

data class InboxCommand(
    val id: Long = 0,
    val countryCode: String? = null,
    val stationId: String? = null,
    val title: String? = null,
    val coordinates: Coordinates? = null,
    val rejectReason: String? = null,
    val ds100: String? = null,
    val active: Boolean? = null,
    val conflictResolution: ConflictResolution? = null,
) {
    val hasCoords: Boolean
        get() = !(coordinates?.hasZeroCoords ?: true)

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
