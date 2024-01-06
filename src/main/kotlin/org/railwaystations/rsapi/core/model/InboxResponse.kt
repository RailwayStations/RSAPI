package org.railwaystations.rsapi.core.model

class InboxResponse(
    var id: Long? = null,
    var state: InboxResponseState,
    var message: String? = null,
    var filename: String? = null,
    var inboxUrl: String? = null,
    var crc32: Long? = null,
) {
    enum class InboxResponseState {
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
