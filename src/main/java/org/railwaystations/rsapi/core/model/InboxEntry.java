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

    boolean hasPhoto;

    Long crc32;

    boolean conflict;

    ProblemReportType problemReportType;

    boolean processed;

    String inboxUrl;

    String ds100;

    Boolean active;

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

}
