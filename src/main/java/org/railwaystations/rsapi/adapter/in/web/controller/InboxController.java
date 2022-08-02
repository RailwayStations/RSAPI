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
    public ModelAndView photoUploadIframe(@RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                          @RequestParam(EMAIL) String email,
                                          @RequestParam(UPLOAD_TOKEN) String uploadToken,
                                          @RequestParam(value = STATION_ID, required = false) String stationId,
                                          @RequestParam(value = COUNTRY_CODE, required = false) String countryCode,
                                          @RequestParam(value = STATION_TITLE, required = false) String stationTitle,
                                          @RequestParam(value = LATITUDE, required = false) Double latitude,
                                          @RequestParam(value = LONGITUDE, required = false) Double longitude,
                                          @RequestParam(value = COMMENT, required = false) String comment,
                                          @RequestParam(value = ACTIVE, required = false) Boolean active,
                                          @RequestParam(value = FILE) MultipartFile file,
                                          @RequestHeader(value = HttpHeaders.REFERER) String referer) throws JsonProcessingException {
        log.info("MultipartFormData: email={}, station={}, country={}, file={}", email, stationId, countryCode, file.getName());
        var refererUri = URI.create(referer);

        try {
            var authentication = authenticator.authenticate(new UsernamePasswordAuthenticationToken(email, uploadToken));
            if (authentication == null || !authentication.isAuthenticated()) {
                return createIFrameAnswer(consumeBodyAndReturn(file.getInputStream(),
                                new InboxResponseDto().state(InboxResponseDto.StateEnum.UNAUTHORIZED)),
                                refererUri);
            }

            var response = uploadPhoto(userAgent, file.getInputStream(), StringUtils.trimToNull(stationId),
                    countryCode, file.getContentType(), stationTitle, latitude, longitude, comment, active, userDetailsService.loadUserByUsername(email));
            return createIFrameAnswer(response, refererUri);
        } catch (Exception e) {
            log.error("FormUpload error", e);
            return createIFrameAnswer(new InboxResponseDto().state(InboxResponseDto.StateEnum.ERROR), refererUri);
        }
    }

    @PostMapping(consumes = MediaType.ALL_VALUE ,produces = MediaType.APPLICATION_JSON_VALUE, value = "/photoUpload")
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

    private InboxResponseDto toDto(InboxResponse inboxResponse) {
        return new InboxResponseDto()
                .id(inboxResponse.getId())
                .crc32(inboxResponse.getCrc32())
                .state(toDto(inboxResponse.getState()))
                .filename(inboxResponse.getFilename())
                .inboxUrl(inboxResponse.getInboxUrl())
                .message(inboxResponse.getMessage());
    }

    private InboxResponseDto.StateEnum toDto(InboxResponse.InboxResponseState inboxResponseState) {
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
    public ResponseEntity<InboxResponseDto> reportProblem(@RequestHeader(HttpHeaders.USER_AGENT) String userAgent,
                                       @RequestBody @NotNull() ProblemReportDto problemReport,
                                       @AuthenticationPrincipal AuthUser user) {
        InboxResponseDto inboxResponse = toDto(manageInboxUseCase.reportProblem(toDomain(problemReport), user.getUser(), userAgent));
        return new ResponseEntity<>(inboxResponse, toHttpStatus(inboxResponse.getState()));
    }

    private ProblemReport toDomain(ProblemReportDto problemReport) {
        return ProblemReport.builder()
                .countryCode(problemReport.getCountryCode())
                .stationId(problemReport.getStationId())
                .type(toDomain(problemReport.getType()))
                .comment(problemReport.getComment())
                .coordinates(mapCoordinates(problemReport.getLat(), problemReport.getLon()))
                .build();
    }

    private ProblemReportType toDomain(ProblemReportDto.TypeEnum dtoType) {
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

    private Coordinates mapCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return null;
        }
        return new Coordinates(lat, lon);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/publicInbox")
    public List<PublicInboxEntryDto> publicInbox() {
        return toPublicInboxEntryDto(manageInboxUseCase.publicInbox());
    }

    private List<PublicInboxEntryDto> toPublicInboxEntryDto(List<PublicInboxEntry> publicInboxEntries) {
        return publicInboxEntries.stream()
                .map(this::toDto)
                .toList();
    }

    private PublicInboxEntryDto toDto(PublicInboxEntry publicInboxEntry) {
        return new PublicInboxEntryDto()
                .countryCode(publicInboxEntry.getCountryCode())
                .stationId(publicInboxEntry.getStationId())
                .title(publicInboxEntry.getTitle())
                .lat(publicInboxEntry.getLat())
                .lon(publicInboxEntry.getLon());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/userInbox")
    @PreAuthorize("isAuthenticated()")
    public List<InboxStateQueryResponseDto> userInbox(@AuthenticationPrincipal AuthUser user, @RequestBody @NotNull List<InboxStateQueryRequestDto> inboxStateQueryRequestDtos) {
        return toInboxStateQueryDto(manageInboxUseCase.userInbox(user.getUser(), toIdList(inboxStateQueryRequestDtos)));
    }

    private List<Long> toIdList(List<InboxStateQueryRequestDto> inboxStateQueryRequestDtos) {
        return inboxStateQueryRequestDtos.stream()
                .map(InboxStateQueryRequestDto::getId)
                .toList();
    }

    private List<InboxStateQueryResponseDto> toInboxStateQueryDto(List<InboxStateQuery> inboxStateQueries) {
        return inboxStateQueries.stream()
                .map(this::toDto)
                .toList();
    }

    private InboxStateQueryResponseDto toDto(InboxStateQuery inboxStateQuery) {
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

    public InboxStateQueryResponseDto.StateEnum toDto(InboxStateQuery.InboxState inboxState) {
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
    public List<InboxEntryDto> adminInbox(@AuthenticationPrincipal AuthUser user) {
        return toInboxEntryDto(manageInboxUseCase.listAdminInbox(user.getUser()));
    }

    private List<InboxEntryDto> toInboxEntryDto(List<InboxEntry> inboxEntries) {
        return inboxEntries.stream()
                .map(this::toDto)
                .toList();
    }

    private InboxEntryDto toDto(InboxEntry inboxEntry) {
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
                .hasConflict(inboxEntry.isConflict())
                .hasPhoto(inboxEntry.isHasPhoto())
                .isProcessed(inboxEntry.isProcessed())
                .photographerEmail(inboxEntry.getPhotographerEmail())
                .photographerNickname(inboxEntry.getPhotographerNickname())
                .problemReportType(toDto(inboxEntry.getProblemReportType()));
    }

    private InboxEntryDto.ProblemReportTypeEnum toDto(ProblemReportType problemReportType) {
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
    public ResponseEntity<AdminInboxCommandResponseDto> adminInbox(@AuthenticationPrincipal AuthUser user, @RequestBody InboxCommandDto commandDto) {
        log.info("Executing adminInbox commandDto {} for Nickname: {}", commandDto.getCommand(), user.getUsername());
        try {
            var command = toDomain(commandDto);
            switch (commandDto.getCommand()) {
                case REJECT -> manageInboxUseCase.rejectInboxEntry(command);
                case IMPORT -> manageInboxUseCase.importUpload(command);
                case ACTIVATE_STATION -> manageInboxUseCase.updateStationActiveState(command, true);
                case DEACTIVATE_STATION -> manageInboxUseCase.updateStationActiveState(command, false);
                case DELETE_STATION -> manageInboxUseCase.deleteStation(command);
                case DELETE_PHOTO -> manageInboxUseCase.deletePrimaryPhoto(command);
                case MARK_SOLVED -> manageInboxUseCase.markProblemReportSolved(command);
                case CHANGE_NAME -> manageInboxUseCase.changeStationTitle(command);
                case UPDATE_LOCATION -> manageInboxUseCase.updateLocation(command);
                case PHOTO_OUTDATED -> manageInboxUseCase.markPrimaryPhotoOutdated(command);
                default -> throw new IllegalArgumentException("Unexpected commandDto value: " + commandDto.getCommand());
            }
        } catch (IllegalArgumentException e) {
            log.warn("adminInbox commandDto {} failed", commandDto, e);
            return new ResponseEntity<>(new AdminInboxCommandResponseDto().status(HttpStatus.BAD_REQUEST.value()).message(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(new AdminInboxCommandResponseDto().status(HttpStatus.OK.value()).message("ok"), HttpStatus.OK);
    }

    private InboxEntry toDomain(InboxCommandDto command) {
        return InboxEntry.builder()
                .id(command.getId())
                .countryCode(command.getCountryCode())
                .stationId(command.getStationId())
                .title(command.getTitle())
                .rejectReason(command.getRejectReason())
                .ds100(command.getDS100())
                .active(command.getActive() != null ? command.getActive() : true)
                .conflictResolution(toDomain(command.getConflictResolution()))
                .createStation(command.getCreateStation())
                .build();
    }

    private InboxEntry.ConflictResolution toDomain(InboxCommandDto.ConflictResolutionEnum conflictResolution) {
        if (conflictResolution == null) {
            return InboxEntry.ConflictResolution.DO_NOTHING;
        }
        return switch (conflictResolution) {
            case DO_NOTHING -> InboxEntry.ConflictResolution.DO_NOTHING;
            case OVERWRITE_EXISTING_PHOTO -> InboxEntry.ConflictResolution.OVERWRITE_EXISTING_PHOTO;
            case IMPORT_AS_NEW_PRIMARY_PHOTO -> InboxEntry.ConflictResolution.IMPORT_AS_NEW_PRIMARY_PHOTO;
            case IMPORT_AS_NEW_SECONDARY_PHOTO -> InboxEntry.ConflictResolution.IMPORT_AS_NEW_SECONDARY_PHOTO;
            case CREATE_NEW_STATION -> InboxEntry.ConflictResolution.CREATE_NEW_STATION;
        };
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/adminInboxCount")
    @PreAuthorize("hasRole('ADMIN')")
    public InboxCountResponseDto adminInboxCount(@AuthenticationPrincipal AuthUser user) {
        return new InboxCountResponseDto().pendingInboxEntries(manageInboxUseCase.countPendingInboxEntries());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/nextZ")
    public NextZResponseDto nextZ() {
        return new NextZResponseDto().nextZ(manageInboxUseCase.getNextZ());
    }

    private InboxResponseDto uploadPhoto(String userAgent, InputStream body, String stationId,
                                      String countryCode, String contentType, String stationTitle,
                                      Double latitude, Double longitude, String comment,
                                      Boolean active, AuthUser user) {
        InboxResponse inboxResponse = manageInboxUseCase.uploadPhoto(userAgent, body, StringUtils.trimToNull(stationId), StringUtils.trimToNull(countryCode),
                StringUtils.trimToEmpty(contentType).split(";")[0], stationTitle,
                latitude, longitude, comment, active != null ? active : true, user.getUser());
        return consumeBodyAndReturn(body, toDto(inboxResponse));
    }

    private HttpStatus toHttpStatus(InboxResponseDto.StateEnum state) {
        return switch (state) {
            case REVIEW -> HttpStatus.ACCEPTED;
            case LAT_LON_OUT_OF_RANGE, NOT_ENOUGH_DATA, UNSUPPORTED_CONTENT_TYPE -> HttpStatus.BAD_REQUEST;
            case PHOTO_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ModelAndView createIFrameAnswer(InboxResponseDto response, URI referer) throws JsonProcessingException {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("iframe");
        modelAndView.getModel().put("response", mapper.writeValueAsString(response));
        modelAndView.getModel().put("referer", referer);
        return modelAndView;
    }

    private InboxResponseDto consumeBodyAndReturn(InputStream body, InboxResponseDto response) {
        if (body != null) {
            try {
                IOUtils.copy(body, NullOutputStream.NULL_OUTPUT_STREAM);
            } catch (IOException e) {
                log.warn("Unable to consume body", e);
            }
        }
        return response;
    }

}
