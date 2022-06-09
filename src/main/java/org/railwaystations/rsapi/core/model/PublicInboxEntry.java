package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicInboxEntry {

    String countryCode;

    String stationId;

    String title;

    Coordinates coordinates;

    public Double getLat() {
        return coordinates != null ? coordinates.getLat() : null;
    }

    public Double getLon() {
        return coordinates != null ? coordinates.getLon() : null;
    }

}
