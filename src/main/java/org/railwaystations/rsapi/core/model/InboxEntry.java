package org.railwaystations.rsapi.core.model;

import java.time.Instant;

public class InboxEntry extends PublicInboxEntry {

    private long id;

    private int photographerId;

    private String photographerNickname;

    private String photographerEmail;

    private String extension;

    private String comment;

    private String rejectReason;

    private Instant createdAt;

    private boolean done;

    private Command command;

    private boolean hasPhoto;

    private Long crc32;

    private boolean conflict;

    private ProblemReportType problemReportType;

    private boolean processed;

    private String inboxUrl;

    private String ds100;

    private Boolean active;

    private Boolean ignoreConflict;

    private Boolean createStation;

    private boolean notified;

    /**
     * Constructor with all values from database
     */
    public InboxEntry(final long id, final String countryCode, final String stationId, final String title,
                      final Coordinates coordinates, final int photographerId, final String photographerNickname, final String photographerEmail,
                      final String extension, final String comment, final String rejectReason,
                      final Instant createdAt, final boolean done, final Command command, final boolean hasPhoto,
                      final boolean conflict, final ProblemReportType problemReportType, final Boolean active,
                      final Long crc32, final boolean notified) {
        super(countryCode, stationId, title, coordinates);
        this.id = id;
        this.photographerId = photographerId;
        this.photographerNickname = photographerNickname;
        this.photographerEmail = photographerEmail;
        this.extension = extension;
        this.comment = comment;
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
        this.done = done;
        this.command = command;
        this.hasPhoto = hasPhoto;
        this.conflict = conflict;
        this.problemReportType = problemReportType;
        this.active = active;
        this.crc32 = crc32;
        this.notified = notified;
    }

    /**
     * Constructor to insert new record from photoUpload
     */
    public InboxEntry(final String countryCode, final String stationId, final String title,
                      final Coordinates coordinates, final int photographerId,
                      final String extension, final String comment, final ProblemReportType problemReportType,
                      final Boolean active) {
        this(0L, countryCode, stationId, title, coordinates, photographerId, null, null, extension,
                comment, null, Instant.now(), false, null, false,
                false, problemReportType, active, null, false);
    }

    /**
     * Constructor to deserialize json for updating the records
     */
    public InboxEntry(final long id, final String countryCode, final String stationId, final String rejectReason,
                      final Command command, final String ds100, final Boolean active, final Boolean ignoreConflict,
                      final Boolean createStation) {
        this(id, countryCode, stationId, null, null, 0, null, null,
                null, null, rejectReason, null, false, command, false,
                false, null, active != null ? active : true, null, false);
        this.ds100 = ds100;
        this.ignoreConflict = ignoreConflict;
        this.createStation = createStation;
    }

    public InboxEntry() {
        super();
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setPhotographerId(final int photographerId) {
        this.photographerId = photographerId;
    }

    public void setPhotographerNickname(final String photographerNickname) {
        this.photographerNickname = photographerNickname;
    }

    public void setPhotographerEmail(final String photographerEmail) {
        this.photographerEmail = photographerEmail;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public void setRejectReason(final String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setDone(final boolean done) {
        this.done = done;
    }

    public void setCommand(final Command command) {
        this.command = command;
    }

    public boolean isHasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(final boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }

    public void setCrc32(final Long crc32) {
        this.crc32 = crc32;
    }

    public boolean isConflict() {
        return conflict;
    }

    public void setProblemReportType(final ProblemReportType problemReportType) {
        this.problemReportType = problemReportType;
    }

    public void setProcessed(final boolean processed) {
        this.processed = processed;
    }

    public String getDs100() {
        return ds100;
    }

    public void setDs100(final String ds100) {
        this.ds100 = ds100;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public Boolean getIgnoreConflict() {
        return ignoreConflict;
    }

    public void setIgnoreConflict(final Boolean ignoreConflict) {
        this.ignoreConflict = ignoreConflict;
    }

    public Boolean getCreateStation() {
        return createStation;
    }

    public void setCreateStation(final Boolean createStation) {
        this.createStation = createStation;
    }

    public void setNotified(final boolean notified) {
        this.notified = notified;
    }

    public long getId() {
        return id;
    }

    public int getPhotographerId() {
        return photographerId;
    }

    public String getPhotographerNickname() {
        return photographerNickname;
    }

    public String getPhotographerEmail() {
        return photographerEmail;
    }

    public String getComment() {
        return comment;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private Long getCreatedAtEpochMilli() {
        return createdAt != null ? createdAt.toEpochMilli() : null;
    }

    public boolean isDone() {
        return done;
    }

    public Command getCommand() {
        return command;
    }

    public String getExtension() {
        return extension;
    }

    public boolean hasPhoto() {
        return hasPhoto;
    }

    public boolean hasConflict() {
        return conflict;
    }

    public ProblemReportType getProblemReportType() {
        return problemReportType;
    }

    public String getFilename() {
        return getFilename(id, extension);
    }

    public static String getFilename(final Long id, final String extension) {
        if (id == null || extension == null) {
            return null;
        }
        return String.format("%d.%s", id, extension);
    }

    public void isProcessed(final boolean processed) {
        this.processed = processed;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setInboxUrl(final String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public boolean isPhotoUpload() { return problemReportType == null; }

    public boolean isProblemReport() {
        return problemReportType != null;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean ignoreConflict() {
        return ignoreConflict;
    }

    public Boolean createStation() {
        return createStation;
    }

    public void setConflict(final boolean conflict) {
        this.conflict = conflict;
    }

    public boolean hasCoords() {
        return coordinates != null && !coordinates.hasZeroCoords();
    }

    public Long getCrc32() {
        return crc32;
    }

    public boolean isNotified() {
        return notified;
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
