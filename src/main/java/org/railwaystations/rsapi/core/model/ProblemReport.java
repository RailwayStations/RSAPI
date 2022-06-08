package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProblemReport {

    String countryCode;

    String stationId;

    ProblemReportType type;

    String comment;

    Coordinates coordinates;

}
