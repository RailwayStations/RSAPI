package org.railwaystations.rsapi.adapter.in.web;

import org.railwaystations.rsapi.adapter.in.web.model.InboxResponseDto;
import org.railwaystations.rsapi.core.model.InboxResponse;
import org.springframework.http.HttpStatus;

public class InboxResponseMapper {

    public static HttpStatus toHttpStatus(InboxResponseDto.StateEnum state) {
        return switch (state) {
            case REVIEW -> HttpStatus.ACCEPTED;
            case LAT_LON_OUT_OF_RANGE, NOT_ENOUGH_DATA, UNSUPPORTED_CONTENT_TYPE -> HttpStatus.BAD_REQUEST;
            case PHOTO_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    public static InboxResponseDto toDto(InboxResponse inboxResponse) {
        return new InboxResponseDto(toDto(inboxResponse.getState()))
                .id(inboxResponse.getId())
                .crc32(inboxResponse.getCrc32())
                .filename(inboxResponse.getFilename())
                .inboxUrl(inboxResponse.getInboxUrl())
                .message(inboxResponse.getMessage());
    }

    public static InboxResponseDto.StateEnum toDto(InboxResponse.InboxResponseState inboxResponseState) {
        return switch (inboxResponseState) {
            case ERROR -> InboxResponseDto.StateEnum.ERROR;
            case CONFLICT -> InboxResponseDto.StateEnum.CONFLICT;
            case REVIEW -> InboxResponseDto.StateEnum.REVIEW;
            case UNAUTHORIZED -> InboxResponseDto.StateEnum.UNAUTHORIZED;
            case LAT_LON_OUT_OF_RANGE -> InboxResponseDto.StateEnum.LAT_LON_OUT_OF_RANGE;
            case NOT_ENOUGH_DATA -> InboxResponseDto.StateEnum.NOT_ENOUGH_DATA;
            case PHOTO_TOO_LARGE -> InboxResponseDto.StateEnum.PHOTO_TOO_LARGE;
            case UNSUPPORTED_CONTENT_TYPE -> InboxResponseDto.StateEnum.UNSUPPORTED_CONTENT_TYPE;
        };
    }

}
