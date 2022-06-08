package org.railwaystations.rsapi.adapter.in.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.in.web.model.AdminInboxCommandResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxCommandDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxCountResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxEntryDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxStateQueryRequestDto;
import org.railwaystations.rsapi.adapter.in.web.model.InboxStateQueryResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.NextZResponseDto;
import org.railwaystations.rsapi.adapter.in.web.model.ProblemReportDto;
import org.railwaystations.rsapi.adapter.in.web.model.PublicInboxEntryDto;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.InboxResponse;
import org.railwaystations.rsapi.core.model.InboxStateQuery;
import org.railwaystations.rsapi.core.model.ProblemReport;
import org.railwaystations.rsapi.core.model.ProblemReportType;
import org.railwaystations.rsapi.core.model.PublicInboxEntry;
import org.railwaystations.rsapi.core.ports.in.ManageInboxUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Slf4j
public class InboxController {

    public static final String EMAIL = "email";
    public static final String UPLOAD_TOKEN = "uploadToken";
    public static final String STATION_ID = "stationId";
    public static final String COUNTRY_CODE = "countryCode";
    public static final String STATION_TITLE = "stationTitle";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String COMMENT = "comment";
    public static final String ACTIVE = "active";
    public static final String FILE = "file";

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ManageInboxUseCase manageInboxUseCase;

    @Autowired
    private RSAuthenticationProvider authenticator;

    @Autowired
    private RSUserDetailsService userDetailsService;

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE + ";charset=UTF-8"}, value = "/photoUpload", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ModelAndView photoUploadIframe(@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) final String userAgent,
                                          @RequestParam(EMAIL) final String email,
                                          @RequestParam(UPLOAD_TOKEN) final String uploadToken,
                                          @RequestParam(value = STATION_ID, required = false) final String stationId,
                                          @RequestParam(value = COUNTRY_CODE, required = false) final String countryCode,
                                          @RequestParam(value = STATION_TITLE, required = false) final String stationTitle,
                                          @RequestParam(value = LATITUDE, required = false) final Double latitude,
                                          @RequestParam(value = LONGITUDE, required = false) final Double longitude,
                                          @RequestParam(value = COMMENT, required = false) final String comment,
                                          @RequestParam(value = ACTIVE, required = false) final Boolean active,
                                          @RequestParam(value = FILE) final MultipartFile file,
                                          @RequestHeader(value = HttpHeaders.REFERER) final String referer) throws JsonProcessingException {
        log.info("MultipartFormData: email={}, station={}, country={}, file={}", email, stationId, countryCode, file.getName());
        final var refererUri = URI.create(referer);

        try {
            final var authentication = authenticator.authenticate(new UsernamePasswordAuthenticationToken(email, uploadToken));
            if (authentication == null || !authentication.isAuthenticated()) {
                return createIFrameAnswer(consumeBodyAndReturn(file.getInputStream(),
                                new InboxResponseDto().state(InboxResponseDto.StateEnum.UNAUTHORIZED)),
                                refererUri);
            }

            final var response = uploadPhoto(userAgent, file.getInputStream(), StringUtils.trimToNull(stationId),
                    countryCode, file.getContentType(), stationTitle, latitude, longitude, comment, active, userDetailsService.loadUserByUsername(email));
            return createIFrameAnswer(response, refererUri);
        } catch (final Exception e) {
            log.error("FormUpload error", e);
            return createIFrameAnswer(new InboxResponseDto().state(InboxResponseDto.StateEnum.ERROR), refererUri);
        }
    }

    @PostMapping(consumes = MediaType.ALL_VALUE ,produces = MediaType.APPLICATION_JSON_VALUE, value = "/photoUpload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InboxResponseDto> photoUpload(final HttpServletRequest request,
                                                        @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) final String userAgent,
                                                        @RequestHeader(value = "Station-Id", required = false) final String stationId,
                                                        @RequestHeader(value = "Country", required = false) final String country,
                                                        @RequestHeader(value = HttpHeaders.CONTENT_TYPE) final String contentType,
                                                        @RequestHeader(value = "Station-Title", required = false) final String encStationTitle,
                                                        @RequestHeader(value = "Latitude", required = false) final Double latitude,
                                                        @RequestHeader(value = "Longitude", required = false) final Double longitude,
                                                        @RequestHeader(value = "Comment", required = false) final String encComment,
                                                        @RequestHeader(value = "Active", required = false) final Boolean active,
                                                        @AuthenticationPrincipal final AuthUser user) throws IOException {
        final var stationTitle = encStationTitle != null ? URLDecoder.decode(encStationTitle, StandardCharsets.UTF_8) : null;
        final var comment = encComment != null ? URLDecoder.decode(encComment, StandardCharsets.UTF_8) : null;
        final var inboxResponse = uploadPhoto(userAgent, request.getInputStream(), stationId,
                country, contentType, stationTitle, latitude, longitude, comment, active, user);
        return new ResponseEntity<>(inboxResponse, toHttpStatus(inboxResponse.getState()));
    }

    private InboxResponseDto toDto(final InboxResponse inboxResponse) {
        return new InboxResponseDto()
                .id(inboxResponse.getId())
                .crc32(inboxResponse.getCrc32())
                .state(toDto(inboxResponse.getState()))
                .filename(inboxResponse.getFilename())
                .inboxUrl(inboxResponse.getInboxUrl())
                .message(inboxResponse.getMessage());
    }

    private InboxResponseDto.StateEnum toDto(final InboxResponse.InboxResponseState inboxResponseState) {
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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/reportProblem")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InboxResponseDto> reportProblem(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                       @RequestBody @NotNull() final ProblemReportDto problemReport,
                                       @AuthenticationPrincipal final AuthUser user) {
        final InboxResponseDto inboxResponse = toDto(manageInboxUseCase.reportProblem(toDomain(problemReport), user.getUser(), userAgent));
        return new ResponseEntity<>(inboxResponse, toHttpStatus(inboxResponse.getState()));
    }

    private ProblemReport toDomain(final ProblemReportDto problemReport) {
        return ProblemReport.builder()
                .countryCode(problemReport.getCountryCode())
                .stationId(problemReport.getStationId())
                .type(toDomain(problemReport.getType()))
                .comment(problemReport.getComment())
                .coordinates(mapCoordinates(problemReport.getLat(), problemReport.getLon()))
                .build();
    }

    private ProblemReportType toDomain(final ProblemReportDto.TypeEnum dtoType) {
        return switch (dtoType) {
            case OTHER -> ProblemReportType.OTHER;
            case WRONG_NAME -> ProblemReportType.WRONG_NAME;
            case WRONG_PHOTO -> ProblemReportType.WRONG_PHOTO;
            case PHOTO_OUTDATED -> ProblemReportType.PHOTO_OUTDATED;
            case STATION_ACTIVE -> ProblemReportType.STATION_ACTIVE;
            case STATION_INACTIVE -> ProblemReportType.STATION_INACTIVE;
            case WRONG_LOCATION -> ProblemReportType.WRONG_LOCATION;
            case STATION_NONEXISTENT -> ProblemReportType.STATION_NONEXISTENT;
        };
    }

    private Coordinates mapCoordinates(final Double lat, final Double lon) {
        if (lat == null || lon == null) {
            return null;
        }
        return new Coordinates(lat, lon);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/publicInbox")
    public List<PublicInboxEntryDto> publicInbox() {
        return toPublicInboxEntryDto(manageInboxUseCase.publicInbox());
    }

    private List<PublicInboxEntryDto> toPublicInboxEntryDto(final List<PublicInboxEntry> publicInboxEntries) {
        return publicInboxEntries.stream()
                .map(this::toDto)
                .toList();
    }

    private PublicInboxEntryDto toDto(final PublicInboxEntry publicInboxEntry) {
        return new PublicInboxEntryDto()
                .countryCode(publicInboxEntry.getCountryCode())
                .stationId(publicInboxEntry.getStationId())
                .title(publicInboxEntry.getTitle())
                .lat(publicInboxEntry.getLat())
                .lon(publicInboxEntry.getLon());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/userInbox")
    @PreAuthorize("isAuthenticated()")
    public List<InboxStateQueryResponseDto> userInbox(@AuthenticationPrincipal final AuthUser user, @RequestBody @NotNull final List<InboxStateQueryRequestDto> inboxStateQueryRequestDtos) {
        return toInboxStateQueryDto(manageInboxUseCase.userInbox(user.getUser(), toIdList(inboxStateQueryRequestDtos)));
    }

    private List<Long> toIdList(final List<InboxStateQueryRequestDto> inboxStateQueryRequestDtos) {
        return inboxStateQueryRequestDtos.stream()
                .map(InboxStateQueryRequestDto::getId)
                .toList();
    }

    private List<InboxStateQueryResponseDto> toInboxStateQueryDto(final List<InboxStateQuery> inboxStateQueries) {
        return inboxStateQueries.stream()
                .map(this::toDto)
                .toList();
    }

    private InboxStateQueryResponseDto toDto(final InboxStateQuery inboxStateQuery) {
        return new InboxStateQueryResponseDto()
                .id(inboxStateQuery.getId())
                .countryCode(inboxStateQuery.getCountryCode())
                .stationId(inboxStateQuery.getStationId())
                .inboxUrl(inboxStateQuery.getInboxUrl())
                .lat(inboxStateQuery.getCoordinates() != null ? inboxStateQuery.getCoordinates().getLat() : null)
                .lon(inboxStateQuery.getCoordinates() != null ? inboxStateQuery.getCoordinates().getLon() : null)
                .filename(inboxStateQuery.getFilename())
                .crc32(inboxStateQuery.getCrc32())
                .rejectedReason(inboxStateQuery.getRejectedReason())
                .state(toDto(inboxStateQuery.getState()));
    }

    public InboxStateQueryResponseDto.StateEnum toDto(final InboxStateQuery.InboxState inboxState) {
        return switch (inboxState) {
            case REVIEW -> InboxStateQueryResponseDto.StateEnum.REVIEW;
            case ACCEPTED -> InboxStateQueryResponseDto.StateEnum.ACCEPTED;
            case CONFLICT -> InboxStateQueryResponseDto.StateEnum.CONFLICT;
            case REJECTED -> InboxStateQueryResponseDto.StateEnum.REJECTED;
            case UNKNOWN -> InboxStateQueryResponseDto.StateEnum.UNKNOWN;
        };
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/adminInbox")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InboxEntryDto> adminInbox(@AuthenticationPrincipal final AuthUser user) {
        return toInboxEntryDto(manageInboxUseCase.listAdminInbox(user.getUser()));
    }

    private List<InboxEntryDto> toInboxEntryDto(final List<InboxEntry> inboxEntries) {
        return inboxEntries.stream()
                .map(this::toDto)
                .toList();
    }

    private InboxEntryDto toDto(final InboxEntry inboxEntry) {
        return new InboxEntryDto()
                .id(inboxEntry.getId())
                .countryCode(inboxEntry.getCountryCode())
                .stationId(inboxEntry.getStationId())
                .title(inboxEntry.getTitle())
                .lat(inboxEntry.getLat())
                .lon(inboxEntry.getLon())
                .active(inboxEntry.getActive())
                .comment(inboxEntry.getComment())
                .inboxUrl(inboxEntry.getInboxUrl())
                .createdAt(inboxEntry.getCreatedAt().toEpochMilli())
                .done(inboxEntry.isDone())
                .filename(inboxEntry.getFilename())
                .hasConflict(inboxEntry.hasConflict())
                .hasPhoto(inboxEntry.hasPhoto())
                .isProcessed(inboxEntry.isProcessed())
                .photographerEmail(inboxEntry.getPhotographerEmail())
                .photographerNickname(inboxEntry.getPhotographerNickname())
                .problemReportType(toDto(inboxEntry.getProblemReportType()));
    }

    private InboxEntryDto.ProblemReportTypeEnum toDto(final ProblemReportType problemReportType) {
        if (problemReportType == null) {
            return null;
        }
        return switch (problemReportType) {
            case STATION_NONEXISTENT -> InboxEntryDto.ProblemReportTypeEnum.STATION_NONEXISTENT;
            case WRONG_LOCATION -> InboxEntryDto.ProblemReportTypeEnum.WRONG_LOCATION;
            case STATION_ACTIVE -> InboxEntryDto.ProblemReportTypeEnum.STATION_ACTIVE;
            case STATION_INACTIVE -> InboxEntryDto.ProblemReportTypeEnum.STATION_INACTIVE;
            case PHOTO_OUTDATED -> InboxEntryDto.ProblemReportTypeEnum.PHOTO_OUTDATED;
            case WRONG_PHOTO -> InboxEntryDto.ProblemReportTypeEnum.WRONG_PHOTO;
            case WRONG_NAME -> InboxEntryDto.ProblemReportTypeEnum.WRONG_NAME;
            case OTHER -> InboxEntryDto.ProblemReportTypeEnum.OTHER;
        };
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/adminInbox", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminInboxCommandResponseDto> adminInbox(@AuthenticationPrincipal final AuthUser user, @RequestBody final InboxCommandDto command) {
        try {
            manageInboxUseCase.processAdminInboxCommand(user.getUser(), toDomain(command));
        } catch (final IllegalArgumentException e) {
            log.warn("adminInbox command {} failed", command, e);
            return new ResponseEntity<>(new AdminInboxCommandResponseDto().status(HttpStatus.BAD_REQUEST.value()).message(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(new AdminInboxCommandResponseDto().status(HttpStatus.OK.value()).message("ok"), HttpStatus.OK);
    }

    private InboxEntry toDomain(final InboxCommandDto command) {
        return new InboxEntry(command.getId(), command.getCountryCode(), command.getStationId(), command.getRejectReason(),
                toDomain(command.getCommand()), command.getDS100(), command.getActive(), command.getIgnoreConflict(), command.getCreateStation());
    }

    private InboxEntry.Command toDomain(final InboxCommandDto.CommandEnum command) {
        return switch (command) {
            case PHOTO_OUTDATED -> InboxEntry.Command.PHOTO_OUTDATED;
            case IMPORT -> InboxEntry.Command.IMPORT;
            case REJECT -> InboxEntry.Command.REJECT;
            case CHANGE_NAME -> InboxEntry.Command.CHANGE_NAME;
            case MARK_SOLVED -> InboxEntry.Command.MARK_SOLVED;
            case DELETE_PHOTO -> InboxEntry.Command.DELETE_PHOTO;
            case DELETE_STATION -> InboxEntry.Command.DELETE_STATION;
            case UPDATE_LOCATION -> InboxEntry.Command.UPDATE_LOCATION;
            case ACTIVATE_STATION -> InboxEntry.Command.ACTIVATE_STATION;
            case DEACTIVATE_STATION -> InboxEntry.Command.DEACTIVATE_STATION;
        };
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/adminInboxCount")
    @PreAuthorize("hasRole('ADMIN')")
    public InboxCountResponseDto adminInboxCount(@AuthenticationPrincipal final AuthUser user) {
        return new InboxCountResponseDto().pendingInboxEntries(manageInboxUseCase.countPendingInboxEntries());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/nextZ")
    public NextZResponseDto nextZ() {
        return new NextZResponseDto().nextZ(manageInboxUseCase.getNextZ());
    }

    private InboxResponseDto uploadPhoto(final String userAgent, final InputStream body, final String stationId,
                                      final String countryCode, final String contentType, final String stationTitle,
                                      final Double latitude, final Double longitude, final String comment,
                                      final Boolean active, final AuthUser user) {
        final InboxResponse inboxResponse = manageInboxUseCase.uploadPhoto(userAgent, body, StringUtils.trimToNull(stationId), StringUtils.trimToNull(countryCode),
                StringUtils.trimToEmpty(contentType).split(";")[0], stationTitle,
                latitude, longitude, comment, active, user.getUser());
        return consumeBodyAndReturn(body, toDto(inboxResponse));
    }

    private HttpStatus toHttpStatus(final InboxResponseDto.StateEnum state) {
        return switch (state) {
            case REVIEW -> HttpStatus.ACCEPTED;
            case LAT_LON_OUT_OF_RANGE, NOT_ENOUGH_DATA, UNSUPPORTED_CONTENT_TYPE -> HttpStatus.BAD_REQUEST;
            case PHOTO_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ModelAndView createIFrameAnswer(final InboxResponseDto response, final URI referer) throws JsonProcessingException {
        final ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("iframe");
        modelAndView.getModel().put("response", mapper.writeValueAsString(response));
        modelAndView.getModel().put("referer", referer);
        return modelAndView;
    }

    private InboxResponseDto consumeBodyAndReturn(final InputStream body, final InboxResponseDto response) {
        if (body != null) {
            try {
                IOUtils.copy(body, NullOutputStream.NULL_OUTPUT_STREAM);
            } catch (final IOException e) {
                log.warn("Unable to consume body", e);
            }
        }
        return response;
    }

}
