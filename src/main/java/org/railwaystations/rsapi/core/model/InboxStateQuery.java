package org.railwaystations.rsapi.core.model;

public class InboxStateQuery {
    private Long id;
    private String countryCode;
    private String stationId;
    private Coordinates coordinates;
    private InboxState state = InboxState.UNKNOWN;
    private String rejectedReason;
    private String filename;
    private String inboxUrl;
    private Long crc32;

    public InboxStateQuery() {
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getStationId() {
        return stationId;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public InboxState getState() {
        return state;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setState(final InboxState state) {
        this.state = state;
    }

    public void setRejectedReason(final String rejectReason) {
        this.rejectedReason = rejectReason;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public void setStationId(final String stationId) {
        this.stationId = stationId;
    }

    public void setCoordinates(final Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public void setInboxUrl(final String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setCrc32(final Long crc32) {
        this.crc32 = crc32;
    }

    public Long getCrc32() {
        return crc32;
    }

    public enum InboxState {
        UNKNOWN,
        REVIEW,
        CONFLICT,
        ACCEPTED,
        REJECTED
    }

}
