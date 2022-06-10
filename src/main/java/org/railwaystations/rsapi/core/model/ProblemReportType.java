package org.railwaystations.rsapi.core.model;

public enum ProblemReportType {
    WRONG_LOCATION(false),
    STATION_ACTIVE(false),
    STATION_INACTIVE(false),
    STATION_NONEXISTENT(false),
    WRONG_PHOTO(true),
    PHOTO_OUTDATED(true),
    OTHER(false),
    WRONG_NAME(false);

    private final boolean needsPhoto;

    ProblemReportType(boolean needsPhoto) {
        this.needsPhoto = needsPhoto;
    }

    public boolean needsPhoto() {
        return needsPhoto;
    }
}
