package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Value
@Builder
public class Photo implements Comparable<Photo> {

    long id;
    Station.Key stationKey;
    boolean primary;
    String urlPath;
    User photographer;
    Instant createdAt;
    License license;
    boolean outdated;

    private Long getCreatedAtEpochMilli() {
        return createdAt != null ? createdAt.toEpochMilli() : null;
    }
    
    @Override
    public int compareTo(@NotNull Photo o) {
        if (primary && !o.isPrimary()) {
            return -1;
        }
        if (!primary && o.isPrimary()) {
            return 1;
        }
        return Long.compare(id, o.getId());
    }

}
