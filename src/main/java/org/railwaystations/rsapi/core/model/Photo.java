package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class Photo {

    Station.Key stationKey;
    String urlPath;
    User photographer;
    Instant createdAt;
    License license;
    boolean outdated;

}
