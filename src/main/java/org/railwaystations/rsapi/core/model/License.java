package org.railwaystations.rsapi.core.model;

import java.util.Arrays;

public enum License {

    CC_BY_30("CC BY 3.0", "https://creativecommons.org/licenses/by/3.0/"),
    CC_BY_NC_40_INT("CC BY-NC 4.0 International", "https://creativecommons.org/licenses/by-nc/4.0/"),
    CC_BY_NC_SA_30_DE("CC BY-NC-SA 3.0 DE", "https://creativecommons.org/licenses/by-nc-sa/3.0/de/"),
    CC_BY_SA_40("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/"),
    CC0_10("CC0 1.0 Universell (CC0 1.0)", "https://creativecommons.org/publicdomain/zero/1.0/");

    private final String displayName;
    private final String url;

    License(String displayName, String url) {
        this.displayName = displayName;
        this.url = url;
    }

    public static License of(String licenseName) {
        return licenseName != null ? valueOf(licenseName) : null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrl() {
        return url;
    }

    public static License ofDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(l -> l.displayName.equals(displayName))
                .findFirst()
                .orElse(null);
    }

}
