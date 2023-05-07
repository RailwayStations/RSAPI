package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class InboxStateQuery {

    Long id;

    String countryCode;

    String stationId;

    String title;

    Coordinates coordinates;

    @Builder.Default
    InboxState state = InboxState.UNKNOWN;

    String comment;

    ProblemReportType problemReportType;

    String rejectedReason;

    String filename;

    String inboxUrl;

    Long crc32;

    Instant createdAt;

    public enum InboxState {
        UNKNOWN,
        REVIEW,
        CONFLICT,
        ACCEPTED,
        REJECTED
    }

}
