package org.railwaystations.rsapi.core.model;

public class PublicInboxEntry {

    protected String countryCode;

    protected String stationId;

    protected String title;

    protected Coordinates coordinates;

    public PublicInboxEntry() {
    }

    public PublicInboxEntry(final String countryCode, final String stationId, final String title, final Coordinates coordinates) {
        this.countryCode = countryCode;
        this.stationId = stationId;
        this.title = title;
        this.coordinates = coordinates;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getStationId() {
        return stationId;
    }

    public String getTitle() {
        return title;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public void setStationId(final String stationId) {
        this.stationId = stationId;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setCoordinates(final Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public Double getLat() {
        return coordinates != null ? coordinates.getLat() : null;
    }

    public Double getLon() {
        return coordinates != null ? coordinates.getLon() : null;
    }
}
