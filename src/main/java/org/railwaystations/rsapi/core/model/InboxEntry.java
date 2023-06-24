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

    Long photoId;

    String title;

    String newTitle;

    Coordinates coordinates;

    Coordinates newCoordinates;

    int photographerId;

    String photographerNickname;

    String photographerEmail;

    String extension;

    String comment;

    String rejectReason;

    Instant createdAt;

    boolean done;

    String existingPhotoUrlPath;

    Long crc32;

    boolean conflict;

    ProblemReportType problemReportType;

    boolean processed;

    String inboxUrl;

    String ds100;

    Boolean active;

    Boolean createStation;

    boolean notified;

    boolean posted;

    public Double getLat() {
        return coordinates != null ? coordinates.getLat() : null;
    }

    public Double getLon() {
        return coordinates != null ? coordinates.getLon() : null;
    }

    public Double getNewLat() {
        return newCoordinates != null ? newCoordinates.getLat() : null;
    }

    public Double getNewLon() {
        return newCoordinates != null ? newCoordinates.getLon() : null;
    }

    public boolean isPhotoUpload() {
        return problemReportType == null && extension != null;
    }

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

    public boolean hasPhoto() {
        return existingPhotoUrlPath != null;
    }

}
