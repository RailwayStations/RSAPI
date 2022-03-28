package org.railwaystations.rsapi.adapter.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.core.model.*;
import org.railwaystations.rsapi.core.services.InboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
public class InboxController {

    private static final Logger LOG = LoggerFactory.getLogger(InboxController.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
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

    private final InboxService inboxService;
    private final RSAuthenticationProvider authenticator;
    private final RSUserDetailsService userDetailsService;

    public InboxController(final InboxService inboxService, final RSAuthenticationProvider authenticator,
                           final RSUserDetailsService userDetailsService) {
        this.inboxService = inboxService;
        this.authenticator = authenticator;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Not part of the "official" API.
     * Supports upload of photos via the website.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/photoUpload", produces = MediaType.TEXT_HTML_VALUE)
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
        LOG.info("MultipartFormData: email={}, station={}, country={}, file={}", email, stationId, countryCode, file.getName());
        final URI refererUri = URI.create(referer);

        try {
            final Authentication authentication = authenticator.authenticate(new UsernamePasswordAuthenticationToken(email, uploadToken));
            if (authentication == null || !authentication.isAuthenticated()) {
                return createIFrameAnswer(consumeBodyAndReturn(file.getInputStream(), new InboxResponse(InboxResponse.InboxResponseState.UNAUTHORIZED)), refererUri);
            }

            final InboxResponse response = uploadPhoto(userAgent, file.getInputStream(), StringUtils.trimToNull(stationId),
                    StringUtils.trimToNull(countryCode), file.getContentType(), stationTitle, latitude, longitude, comment, active, userDetailsService.loadUserByUsername(email));
            return createIFrameAnswer(response, refererUri);
        } catch (final Exception e) {
            LOG.error("FormUpload error", e);
            return createIFrameAnswer(new InboxResponse(InboxResponse.InboxResponseState.ERROR), refererUri);
        }
    }

    @PostMapping(consumes = MediaType.ALL_VALUE ,produces = MediaType.APPLICATION_JSON_VALUE, value = "/photoUpload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InboxResponse> photoUpload(final HttpServletRequest request,
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
        final String stationTitle = encStationTitle != null ? URLDecoder.decode(encStationTitle, StandardCharsets.UTF_8) : null;
        final String comment = encComment != null ? URLDecoder.decode(encComment, StandardCharsets.UTF_8) : null;
        final InboxResponse inboxResponse = uploadPhoto(userAgent, request.getInputStream(), StringUtils.trimToNull(stationId),
                StringUtils.trimToNull(country), contentType, stationTitle, latitude, longitude, comment, active, user);
        return new ResponseEntity<>(inboxResponse, map(inboxResponse.getState()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/reportProblem")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InboxResponse> reportProblem(@RequestHeader(HttpHeaders.USER_AGENT) final String userAgent,
                                       @RequestBody @NotNull() final ProblemReport problemReport,
                                       @AuthenticationPrincipal final AuthUser user) {
        final InboxResponse inboxResponse = inboxService.reportProblem(problemReport, user.getUser(), userAgent);
        return new ResponseEntity<>(inboxResponse, map(inboxResponse.getState()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/publicInbox")
    public List<PublicInboxEntry> publicInbox() {
        return inboxService.publicInbox();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, value = "/userInbox")
    @PreAuthorize("isAuthenticated()")
    public List<InboxStateQuery> userInbox(@AuthenticationPrincipal final AuthUser user, @RequestBody @NotNull final List<InboxStateQuery> queries) {
        return inboxService.userInbox(user.getUser(), queries);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/adminInbox")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InboxEntry> adminInbox(@AuthenticationPrincipal final AuthUser user) {
        return inboxService.listAdminInbox(user.getUser());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, value = "/adminInbox", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminInboxCommandResponse> adminInbox(@AuthenticationPrincipal final AuthUser user, @RequestBody final InboxEntry command) {
        try {
            inboxService.processAdminInboxCommand(user.getUser(), command);
        } catch (final IllegalArgumentException e) {
            LOG.warn("adminInbox command {} failed", command, e);
            return new ResponseEntity<>(new AdminInboxCommandResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(new AdminInboxCommandResponse(HttpStatus.OK.value(), "ok"), HttpStatus.OK);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/adminInboxCount")
    @PreAuthorize("hasRole('ADMIN')")
    public InboxCountResponse adminInboxCount(@AuthenticationPrincipal final AuthUser user) {
        return new InboxCountResponse(inboxService.countPendingInboxEntries());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/nextZ")
    public NextZResponse nextZ() {
        return new NextZResponse(inboxService.getNextZ());
    }

    private InboxResponse uploadPhoto(final String userAgent, final InputStream body, final String stationId,
                                      final String country, final String contentType, final String stationTitle,
                                      final Double latitude, final Double longitude, final String comment,
                                      final Boolean active, final AuthUser user) {
        final InboxResponse inboxResponse = inboxService.uploadPhoto(userAgent, body, stationId, country, contentType, stationTitle,
                latitude, longitude, comment, active, user.getUser());
        return consumeBodyAndReturn(body, inboxResponse);
    }

    private HttpStatus map(final InboxResponse.InboxResponseState state) {
        return switch (state) {
            case REVIEW -> HttpStatus.ACCEPTED;
            case LAT_LON_OUT_OF_RANGE, NOT_ENOUGH_DATA, UNSUPPORTED_CONTENT_TYPE -> HttpStatus.BAD_REQUEST;
            case PHOTO_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ModelAndView createIFrameAnswer(final InboxResponse response, final URI referer) throws JsonProcessingException {
        final ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("iframe");
        modelAndView.getModel().put("response", MAPPER.writeValueAsString(response));
        modelAndView.getModel().put("referer", referer);
        return modelAndView;
    }

    private InboxResponse consumeBodyAndReturn(final InputStream body, final InboxResponse response) {
        if (body != null) {
            try {
                IOUtils.copy(body, NullOutputStream.NULL_OUTPUT_STREAM);
            } catch (final IOException e) {
                LOG.warn("Unable to consume body", e);
            }
        }
        return response;
    }

    private record InboxCountResponse(int pendingInboxEntries) {

        public int getPendingInboxEntries() {
            return pendingInboxEntries;
        }
    }

    private record NextZResponse(String nextZ) {

        public String getNextZ() {
            return nextZ;
        }

    }

    private record AdminInboxCommandResponse(int status, String message) {

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

}
