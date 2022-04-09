package org.railwaystations.rsapi.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Coordinates (double lat, double lon) {
    public static final double ZERO = 0.0;

    public Coordinates() {
        this(ZERO,ZERO);
    }

    @JsonIgnore
    public boolean hasZeroCoords() {
        return lat == ZERO && lon == ZERO;
    }

    @JsonIgnore
    public boolean isValid() {
        return Math.abs(lat) < 90 && Math.abs(lon) < 180 && !hasZeroCoords();
    }

}
