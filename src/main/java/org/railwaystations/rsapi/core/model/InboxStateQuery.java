package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InboxStateQuery {

    Long id;

    String countryCode;

    String stationId;

    Coordinates coordinates;

    @Builder.Default
    InboxState state = InboxState.UNKNOWN;

    String rejectedReason;

    String filename;

    String inboxUrl;

    Long crc32;

    public enum InboxState {
        UNKNOWN,
        REVIEW,
        CONFLICT,
        ACCEPTED,
        REJECTED
    }

}
