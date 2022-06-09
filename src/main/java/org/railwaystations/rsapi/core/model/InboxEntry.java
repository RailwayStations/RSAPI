package org.railwaystations.rsapi.core.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
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

    Boolean ignoreConflict;

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

    public static String createFilename(final Long id, final String extension) {
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

}
