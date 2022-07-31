package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class InboxEntry {

    long id;

    String countryCode;

    String stationId;

    String title;

    Coordinates coordinates;

    int photographerId;

    String photographerNickname;

    String photographerEmail;

    String extension;

    String comment;

    String rejectReason;

    Instant createdAt;

    boolean done;

    Command command;

    boolean hasPhoto;

    Long crc32;

    boolean conflict;

    ProblemReportType problemReportType;

    boolean processed;

    String inboxUrl;

    String ds100;

    Boolean active;

    ConflictResolution conflictResolution;

    Boolean createStation;

    boolean notified;

    private Long getCreatedAtEpochMilli() {
        return createdAt != null ? createdAt.toEpochMilli() : null;
    }

    public boolean hasCoords() {
        return coordinates != null && !coordinates.hasZeroCoords();
    }

    public Double getLat() {
        return coordinates != null ? coordinates.getLat() : null;
    }

    public Double getLon() {
        return coordinates != null ? coordinates.getLon() : null;
    }

    public boolean isPhotoUpload() { return problemReportType == null; }

    public boolean isProblemReport() {
        return problemReportType != null;
    }

    public String getFilename() {
        return createFilename(getId(), getExtension());
    }

    public static String createFilename(Long id, String extension) {
        if (id == null || extension == null) {
            return null;
        }
        return String.format("%d.%s", id, extension);
    }

    public enum Command {
        IMPORT,
        ACTIVATE_STATION,
        DEACTIVATE_STATION,
        DELETE_STATION,
        DELETE_PHOTO,
        MARK_SOLVED,
        REJECT,
        CHANGE_NAME,
        UPDATE_LOCATION,
        PHOTO_OUTDATED
    }

    public enum ConflictResolution {
        DO_NOTHING(false, false),
        OVERWRITE_EXISTING_PHOTO(true, false),
        IMPORT_AS_NEW_PRIMARY_PHOTO(true, false),
        IMPORT_AS_NEW_SECONDARY_PHOTO(true, false),
        CREATE_NEW_STATION(false, true);

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
