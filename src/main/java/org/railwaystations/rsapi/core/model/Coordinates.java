package org.railwaystations.rsapi.core.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Coordinates {

    public static final double ZERO = 0.0;

    double lat;
    double lon;

    public Coordinates() {
        this(ZERO,ZERO);
    }

    public boolean hasZeroCoords() {
        return lat == ZERO && lon == ZERO;
    }

    public boolean isValid() {
        return Math.abs(lat) < 90 && Math.abs(lon) < 180 && !hasZeroCoords();
    }

}
