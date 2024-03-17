package org.railwaystations.rsapi.core.model

data class InboxResponse(
    val id: Long? = null,
    val state: InboxResponseState,
    val message: String? = null,
    val filename: String? = null,
    val inboxUrl: String? = null,
    val crc32: Long? = null,
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
