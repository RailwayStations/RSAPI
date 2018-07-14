package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.Objects;

public class Station {

    private static final int EARTH_RADIUS = 6371;

    @JsonUnwrapped
    private final Key key;

    @JsonProperty
    private final String title;

    @JsonUnwrapped
    private final Coordinates coordinates;

    @JsonProperty
    private final String photographer;

    @JsonProperty
    private final String photographerUrl;

    @JsonProperty("DS100")
    private final String ds100;

    @JsonProperty
    private final String photoUrl;

    @JsonProperty
    private final String license;

    @JsonProperty
    private final Long createdAt;

    @JsonIgnore
    private final String statUser;

    public Station() {
        this(new Key("", "0"), null, new Coordinates(0.0, 0.0), null);
    }

    public Station(final Key key, final String title, final Coordinates coordinates, final Photo photo) {
        this(key, title, coordinates, null, photo);
    }

    public Station(final Key key, final String title, final Coordinates coordinates, final String ds100, final Photo photo) {
        super();
        this.key = key;
        this.title = title;
        this.coordinates = coordinates;
        this.ds100 = ds100;
        if (photo != null) {
            this.photographer = photo.getPhotographer();
            this.photoUrl = photo.getUrl();
            this.license = photo.getLicense();
            this.photographerUrl = photo.getPhotographerUrl();
            this.statUser = photo.getStatUser();
            this.createdAt = photo.getCreatedAt();
        } else {
            this.photographer = null;
            this.photoUrl = null;
            this.license = null;
            this.photographerUrl = null;
            this.statUser = null;
            this.createdAt = null;
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
        final Double latDistance = Math.toRadians(latitude - this.coordinates.getLat());
        final Double lonDistance = Math.toRadians(longitude - this.coordinates.getLon());
        final Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.coordinates.getLat())) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        final Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Station.EARTH_RADIUS * c;
    }

    public boolean appliesTo(final Boolean hasPhoto, final String photographer, final Integer maxDistance, final Double lat, final Double lon) {
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

    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public String getStatUser() {
        return statUser;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    @SuppressWarnings("PMD.ShortClassName")
    public static final class Key {
        @JsonProperty
        private final String country;

        @JsonProperty("idStr")
        private final String id;

        public Key() {
            this("","");
        }

        public Key(final String country, final String id) {
            this.country = country;
            this.id = id;
        }

        public String getCountry() {
            return country;
        }

        public String getId() {
            return id;
        }

        @JsonGetter("id")
        public int getIdLegacy() {
            try {
                return Integer.parseInt(id);
            } catch (final NumberFormatException ignored) {
                return -1;
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof  Key)) {
                return false;
            }
            final Key key = (Key) o;
            return Objects.equals(country, key.country) &&
                    Objects.equals(id, key.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(country, id);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "country='" + country + '\'' +
                    ", id='" + id + '\'' +
                    '}';
        }
    }
}