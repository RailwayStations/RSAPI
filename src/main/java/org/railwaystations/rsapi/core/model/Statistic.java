package org.railwaystations.rsapi.core.model;

public record Statistic(String countryCode, long total, long withPhoto, long photographers) {

    public long withoutPhoto() {
        return total - withPhoto;
    }

}
