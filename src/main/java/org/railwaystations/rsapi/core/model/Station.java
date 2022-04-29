package org.railwaystations.rsapi.core.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.Objects;

public class Station {

    private static final int EARTH_RADIUS = 6371;

    private final Key key;

    private String title;

    private Coordinates coordinates;

    private int photographerId;

    private String photographer;

    private String photographerUrl;

    private final String ds100;

    private String photoUrl;

    private String license;

    private String licenseUrl;

    private Instant createdAt;

    private boolean active;

    private Boolean outdated;

    public Station() {
        this(new Key("", "0"), null, new Coordinates(0.0, 0.0), null, true);
    }

    public Station(final Key key, final String title, final Coordinates coordinates, final Photo photo, final boolean active) {
        this(key, title, coordinates, null, photo, active);
    }

    public Station(final Key key, final String title, final Coordinates coordinates, final String ds100, final Photo photo, final boolean active) {
        super();
        this.key = key;
        this.title = title;
        this.coordinates = coordinates;
        this.ds100 = ds100;
        this.active = active;
        setPhoto(photo);
    }

    public void setPhoto(final Photo photo) {
        if (photo != null) {
            final User user = photo.getPhotographer();
            if (user != null) {
                this.photographerId = user.getId();
                this.photographer = user.getDisplayName();
                this.photographerUrl = user.getDisplayUrl();
            } else {
                this.photographerId = 0;
                this.photographer = "-";
                this.photographerUrl = "";
            }

            this.photoUrl = photo.getUrlPath();
            this.license = photo.getLicense();
            this.licenseUrl = photo.getLicenseUrl();
            this.photographerUrl = photo.getPhotographer().getDisplayUrl();
            this.createdAt = photo.getCreatedAt();
            this.outdated = photo.isOutdated();
        } else {
            this.photographerId = 0;
            this.photographer = null;
            this.photoUrl = null;
            this.license = null;
            this.licenseUrl = null;
            this.photographerUrl = null;
            this.createdAt = null;
            this.outdated = null;
        }
    }

    public Key getKey() {
        return this.key;
    }

    public String getTitle() {
        return this.title;
    }

    public Coordinates getCoordinates() {
        return this.coordinates;
    }

    public boolean hasPhoto() {
        return this.photographer != null;
    }

    public String getPhotographer() { return this.photographer; }

    /*
     * Calculate distance in km between this objects position and the given latitude and longitude.
     * Uses Haversine method as its base.
     *
     * @returns Distance in km
     */
    public double distanceTo(final double latitude, final double longitude) {
        final double latDistance = Math.toRadians(latitude - this.coordinates.getLat());
        final double lonDistance = Math.toRadians(longitude - this.coordinates.getLon());
        final double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.coordinates.getLat())) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Station.EARTH_RADIUS * c;
    }

    public boolean appliesTo(final Boolean hasPhoto, final String photographer, final Integer maxDistance, final Double lat, final Double lon, final Boolean active) {
        boolean result = true;
        if (hasPhoto != null) {
            result = this.hasPhoto() == hasPhoto;
        }
        if (photographer != null) {
            result &= photographer.equals(this.getPhotographer());
        }
        if (maxDistance != null && lat != null && lon != null) {
            result &= this.distanceTo(lat, lon) < maxDistance;
        }
        if (active != null) {
            result &= active == this.active;
        }
        return result;
    }

    public String getDS100() {
        return ds100;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getLicense() {
        return license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private Long getCreatedAtEpochMilli() {
        return createdAt != null ? createdAt.toEpochMilli() : null;
    }

    private void setCreatedAtEpochMilli(final Long time) {
        this.createdAt = time != null ? Instant.ofEpochMilli(time) : null;
    }

    public boolean isActive() {
        return active;
    }

    public int getPhotographerId() {
        return photographerId;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public void setCoordinates(final Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public void prependPhotoBaseUrl(final String photoBaseUrl) {
        if (photoUrl != null) {
            photoUrl = photoBaseUrl + photoUrl;
        }
    }

    public Boolean getOutdated() {
        return outdated;
    }

    public void setOutdated(final Boolean outdated) {
        this.outdated = outdated;
    }


    @Value
    @AllArgsConstructor
    public static class Key {
        String country;
        String id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final Station other)) {
            return false;
        }
        return Objects.equals(key, other.getKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

}
