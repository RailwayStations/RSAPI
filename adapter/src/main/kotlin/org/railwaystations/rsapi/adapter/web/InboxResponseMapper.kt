package org.railwaystations.rsapi.adapter.web

import org.railwaystations.openapi.model.InboxResponseDto
import org.railwaystations.rsapi.core.model.InboxResponse
import org.springframework.http.HttpStatus

object InboxResponseMapper {
    fun toHttpStatus(state: InboxResponseDto.State): HttpStatus {
        return when (state) {
            InboxResponseDto.State.REVIEW -> HttpStatus.ACCEPTED
            InboxResponseDto.State.LAT_LON_OUT_OF_RANGE, InboxResponseDto.State.NOT_ENOUGH_DATA, InboxResponseDto.State.UNSUPPORTED_CONTENT_TYPE -> HttpStatus.BAD_REQUEST
            InboxResponseDto.State.PHOTO_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE
            InboxResponseDto.State.CONFLICT -> HttpStatus.CONFLICT
            InboxResponseDto.State.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
            InboxResponseDto.State.ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    fun toDto(inboxResponse: InboxResponse): InboxResponseDto {
        return InboxResponseDto(
            state = toDto(inboxResponse.state),
            message = inboxResponse.message,
            id = inboxResponse.id,
            filename = inboxResponse.filename,
            inboxUrl = inboxResponse.inboxUrl,
            crc32 = inboxResponse.crc32,
        )
    }

    fun toDto(inboxResponseState: InboxResponse.InboxResponseState): InboxResponseDto.State {
        return when (inboxResponseState) {
            InboxResponse.InboxResponseState.ERROR -> InboxResponseDto.State.ERROR
            InboxResponse.InboxResponseState.CONFLICT, InboxResponse.InboxResponseState.REVIEW -> InboxResponseDto.State.REVIEW
            InboxResponse.InboxResponseState.UNAUTHORIZED -> InboxResponseDto.State.UNAUTHORIZED
            InboxResponse.InboxResponseState.LAT_LON_OUT_OF_RANGE -> InboxResponseDto.State.LAT_LON_OUT_OF_RANGE
            InboxResponse.InboxResponseState.NOT_ENOUGH_DATA -> InboxResponseDto.State.NOT_ENOUGH_DATA
            InboxResponse.InboxResponseState.PHOTO_TOO_LARGE -> InboxResponseDto.State.PHOTO_TOO_LARGE
            InboxResponse.InboxResponseState.UNSUPPORTED_CONTENT_TYPE -> InboxResponseDto.State.UNSUPPORTED_CONTENT_TYPE
        }
    }
}
