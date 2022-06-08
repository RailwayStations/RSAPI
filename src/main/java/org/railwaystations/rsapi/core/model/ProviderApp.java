package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderApp {

    String type;
    String name;
    String url;

}
