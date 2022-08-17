package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InboxCommand {

    long id;

    String countryCode;

    String stationId;

    String title;

    Coordinates coordinates;

    String rejectReason;

    String ds100;

    Boolean active;

    ConflictResolution conflictResolution;

    public boolean hasCoords() {
        return coordinates != null && !coordinates.hasZeroCoords();
    }

    public Double getLat() {
        return coordinates != null ? coordinates.getLat() : null;
    }

    public Double getLon() {
        return coordinates != null ? coordinates.getLon() : null;
    }

    public enum ConflictResolution {
        DO_NOTHING(false, false),
        OVERWRITE_EXISTING_PHOTO(true, false),
        IMPORT_AS_NEW_PRIMARY_PHOTO(true, false),
        IMPORT_AS_NEW_SECONDARY_PHOTO(true, false),
        IGNORE_NEARBY_STATION(false, true);

        private final boolean solvesPhotoConflict;
        private final boolean solvesStationConflict;

        ConflictResolution(boolean solvesPhotoConflict, boolean solvesStationConflict) {
            this.solvesPhotoConflict = solvesPhotoConflict;
            this.solvesStationConflict = solvesStationConflict;
        }

        public boolean solvesPhotoConflict() {
            return solvesPhotoConflict;
        }

        public boolean solvesStationConflict() {
            return solvesStationConflict;
        }
    }

}
