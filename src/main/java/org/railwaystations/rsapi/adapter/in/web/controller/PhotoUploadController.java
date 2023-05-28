package org.railwaystations.rsapi.adapter.in.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.in.web.model.InboxResponseDto;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.core.ports.in.ManageInboxUseCase;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.railwaystations.rsapi.adapter.in.web.InboxResponseMapper.toDto;
import static org.railwaystations.rsapi.adapter.in.web.InboxResponseMapper.toHttpStatus;
import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getRequest;

@RestController
@Slf4j
@Validated
@RequiredArgsConstructor
public class PhotoUploadController {

    private final ManageInboxUseCase manageInboxUseCase;

    private final ManageProfileUseCase manageProfileUseCase;

    private final LocaleResolver localeResolver;

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE + ";charset=UTF-8"}, value = "/photoUploadMultipartFormdata", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InboxResponseDto> photoUploadMultipartFormdata(@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                                                         @AuthenticationPrincipal AuthUser user,
                                                                         @RequestParam(value = "stationId", required = false) String stationId,
                                                                         @RequestParam(value = "countryCode", required = false) String countryCode,
                                                                         @RequestParam(value = "stationTitle", required = false) String stationTitle,
                                                                         @RequestParam(value = "latitude", required = false) Double latitude,
                                                                         @RequestParam(value = "longitude", required = false) Double longitude,
                                                                         @RequestParam(value = "comment", required = false) String comment,
                                                                         @RequestParam(value = "active", required = false) Boolean active,
                                                                         @RequestParam(value = "file") MultipartFile file) {
        log.info("MultipartFormData2: user={}, station={}, country={}, file={}", user.getUsername(), stationId, countryCode, file.getName());

        try {
            InboxResponseDto response;
            if (file.isEmpty()) {
                response = uploadPhoto(userAgent, null, StringUtils.trimToNull(stationId),
                        countryCode, null, stationTitle, latitude, longitude, comment, active, user);
            } else {
                response = uploadPhoto(userAgent, file.getInputStream(), StringUtils.trimToNull(stationId),
                        countryCode, file.getContentType(), stationTitle, latitude, longitude, comment, active, user);
            }
            return new ResponseEntity<>(response, toHttpStatusMultipartFormdata(response.getState()));
        } catch (Exception e) {
            log.error("FormUpload error", e);
            return new ResponseEntity<>(new InboxResponseDto(InboxResponseDto.StateEnum.ERROR), toHttpStatus(InboxResponseDto.StateEnum.ERROR));
        }
    }

    /**
     * jQuery.ajax treats anything different to 2xx as error, so we have to simplify the response codes
     */
    private HttpStatus toHttpStatusMultipartFormdata(InboxResponseDto.StateEnum state) {
        return switch (state) {
            case REVIEW, CONFLICT -> HttpStatus.ACCEPTED;
            case PHOTO_TOO_LARGE, LAT_LON_OUT_OF_RANGE, NOT_ENOUGH_DATA, UNSUPPORTED_CONTENT_TYPE -> HttpStatus.OK;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }


    @PostMapping(consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/photoUpload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InboxResponseDto> photoUpload(HttpServletRequest request,
                                                        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                                        @RequestHeader(value = "Station-Id", required = false) String stationId,
                                                        @RequestHeader(value = "Country", required = false) String country,
                                                        @RequestHeader(value = HttpHeaders.CONTENT_TYPE) String contentType,
                                                        @RequestHeader(value = "Station-Title", required = false) String encStationTitle,
                                                        @RequestHeader(value = "Latitude", required = false) Double latitude,
                                                        @RequestHeader(value = "Longitude", required = false) Double longitude,
                                                        @RequestHeader(value = "Comment", required = false) String encComment,
                                                        @RequestHeader(value = "Active", required = false) Boolean active,
                                                        @AuthenticationPrincipal AuthUser user) throws IOException {
        var stationTitle = encStationTitle != null ? URLDecoder.decode(encStationTitle, StandardCharsets.UTF_8) : null;
        var comment = encComment != null ? URLDecoder.decode(encComment, StandardCharsets.UTF_8) : null;
        var inboxResponse = uploadPhoto(userAgent, request.getInputStream(), stationId,
                country, contentType, stationTitle, latitude, longitude, comment, active, user);
        return new ResponseEntity<>(inboxResponse, toHttpStatus(inboxResponse.getState()));
    }

    private InboxResponseDto uploadPhoto(String userAgent, InputStream body, String stationId,
                                         String countryCode, String contentType, String stationTitle,
                                         Double latitude, Double longitude, String comment,
                                         Boolean active, AuthUser authUser) {
        var locale = localeResolver.resolveLocale(getRequest());
        var user = authUser.getUser();
        if (!user.getLocale().equals(locale)) {
            manageProfileUseCase.updateLocale(user, locale);
        }

        var inboxResponse = manageInboxUseCase.uploadPhoto(userAgent, body, StringUtils.trimToNull(stationId), StringUtils.trimToNull(countryCode),
                StringUtils.trimToEmpty(contentType).split(";")[0], stationTitle,
                latitude, longitude, comment, active != null ? active : true, user);
        return consumeBodyAndReturn(body, toDto(inboxResponse));
    }

    private InboxResponseDto consumeBodyAndReturn(InputStream body, InboxResponseDto response) {
        if (body != null) {
            try {
                IOUtils.copy(body, NullOutputStream.INSTANCE);
            } catch (IOException e) {
                log.warn("Unable to consume body", e);
            }
        }
        return response;
    }


}
