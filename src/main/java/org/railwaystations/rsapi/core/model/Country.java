package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class Country {

    @EqualsAndHashCode.Include
    String code;

    String name;

    @Builder.Default
    String email = "bahnhofsfotos@deutschlands-bahnhoefe.de";

    @Builder.Default
    String twitterTags = "@Bahnhofsoma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn";

    String timetableUrlTemplate;

    String overrideLicense;

    boolean active;

    @Singular
    List<ProviderApp> providerApps = new ArrayList<>();

}
