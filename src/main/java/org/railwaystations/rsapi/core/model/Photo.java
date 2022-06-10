package org.railwaystations.rsapi.core.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Photo {

    private static final Map<String, String> LICENSES = new HashMap<>();

    static {
        LICENSES.put("CC BY 3.0", "https://creativecommons.org/licenses/by/3.0/");
        LICENSES.put("CC BY-NC 4.0 International", "https://creativecommons.org/licenses/by-nc/4.0/");
        LICENSES.put("CC BY-NC-SA 3.0 DE", "https://creativecommons.org/licenses/by-nc-sa/3.0/de/");
        LICENSES.put("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/");
        LICENSES.put("CC0 1.0 Universell (CC0 1.0)", "https://creativecommons.org/publicdomain/zero/1.0/");
    }

    private final Station.Key stationKey;
    private final String urlPath;
    private final User photographer;
    private final Instant createdAt;
    private final String license;
    private final String licenseUrl;
    private final boolean outdated;

    public Photo(Station.Key stationKey, String urlPath, User photographer, Instant createdAt, String license, boolean outdated) {
        this.stationKey = stationKey;
        this.urlPath = urlPath;
        this.photographer = photographer;
        this.createdAt = createdAt;
        this.license = license;
        this.licenseUrl = LICENSES.get(license);
        this.outdated = outdated;
    }

    public Photo(Station.Key stationKey, String urlPath, User photographer, Instant createdAt, String license) {
        this(stationKey, urlPath, photographer, createdAt, license, false);
    }

    public String getUrlPath() {
        return urlPath;
    }

    public User getPhotographer() {
        return photographer;
    }

    public Station.Key getStationKey() {
        return stationKey;
    }

    public String getLicense() {
        return license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isOutdated() {
        return outdated;
    }

}
