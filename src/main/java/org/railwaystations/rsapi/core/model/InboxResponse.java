package org.railwaystations.rsapi.core.model;

public class InboxResponse {
    private final InboxResponseState state;
    private final String message;
    private final Long id;
    private final String filename;
    private final String inboxUrl;
    private final Long crc32;

    public InboxResponse(final InboxResponseState state, final String message, final Long id, final String filename,
                         final String inboxUrl, final Long crc32) {
        this.state = state;
        this.message = message;
        this.id = id;
        this.filename = filename;
        this.inboxUrl = inboxUrl;
        this.crc32 = crc32;
    }

    public InboxResponse(final InboxResponseState state, final Long id, final String filename, final String inboxUrl, final Long crc32) {
        this(state, state.name(), id, filename, inboxUrl, crc32);
    }

    public InboxResponse(final InboxResponseState state, final Long id) {
        this(state, state.name(), id, null, null, null);
    }

    public InboxResponse(final InboxResponseState state, final String message) {
        this(state, message, null, null, null, null);
    }

    public InboxResponseState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public Long getCrc32() {
        return crc32;
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
