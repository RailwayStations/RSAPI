package org.railwaystations.rsapi.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Objects;

@Value
@Builder
public class Station {

    private static final int EARTH_RADIUS = 6371;

    @Builder.Default
    Key key = new Key("", "0");

    String title;

    @Builder.Default
    Coordinates coordinates = new Coordinates(0.0, 0.0);

    String ds100;

    Photo photo;

    @Builder.Default
    boolean active = true;

    public boolean hasPhoto() {
        return this.photo != null;
    }

    /*
     * Calculate distance in km between this objects position and the given latitude and longitude.
     * Uses Haversine method as its base.
     *
     * @returns Distance in km
     */
    public double distanceTo(double latitude, double longitude) {
        double latDistance = Math.toRadians(latitude - this.coordinates.getLat());
        double lonDistance = Math.toRadians(longitude - this.coordinates.getLon());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.coordinates.getLat())) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Station.EARTH_RADIUS * c;
    }

    public boolean appliesTo(Boolean hasPhoto, String photographer, Boolean active) {
        boolean result = true;
        if (hasPhoto != null) {
            result = this.hasPhoto() == hasPhoto;
        }
        if (photographer != null) {
            result &= hasPhoto() && photographer.equals(photo.getPhotographer().getDisplayName());
        }
        if (active != null) {
            result &= active == this.active;
        }
        return result;
    }

    @Value
    @AllArgsConstructor
    public static class Key {
        String country;
        String id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Station other)) {
            return false;
        }
        return Objects.equals(key, other.getKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

}
