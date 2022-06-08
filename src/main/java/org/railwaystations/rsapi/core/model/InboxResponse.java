package org.railwaystations.rsapi.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InboxResponse {
    Long id;
    InboxResponseState state;
    String message;
    String filename;
    String inboxUrl;
    Long crc32;

    public static InboxResponse of(final InboxResponseState state, final String message) {
        return InboxResponse.builder().state(state).message(message).build();
    }

    public static InboxResponse of(final InboxResponseState state, final Long id) {
        return InboxResponse.builder().state(state).id(id).build();
    }

    public enum InboxResponseState {
        REVIEW,
        LAT_LON_OUT_OF_RANGE,
        NOT_ENOUGH_DATA,
        UNSUPPORTED_CONTENT_TYPE,
        PHOTO_TOO_LARGE,
        CONFLICT,
        UNAUTHORIZED,
        ERROR
    }

}
