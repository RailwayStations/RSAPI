package org.railwaystations.rsapi.adapter.in.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.PhotoDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.adapter.out.monitoring.MockMonitor;
import org.railwaystations.rsapi.adapter.out.photostorage.PhotoFileStorage;
import org.railwaystations.rsapi.app.ClockTestConfiguration;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.app.auth.WebSecurityConfig;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.ProblemReportType;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.railwaystations.rsapi.core.services.InboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InboxController.class, properties = {"inboxBaseUrl=http://inbox.railway-stations.org"})
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, MockMvcTestConfiguration.class, WebSecurityConfig.class})
@Import({InboxService.class, PhotoFileStorage.class, RSUserDetailsService.class, ClockTestConfiguration.class})
@ActiveProfiles("mockMvcTest")
class InboxControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MockMonitor monitor;

    @Autowired
    private Clock clock;

    @MockBean
    private InboxDao inboxDao;

    @MockBean
    private StationDao stationDao;

    @MockBean
    private RSAuthenticationProvider authenticator;

    @MockBean
    private UserDao userDao;

    @MockBean
    private CountryDao countryDao;

    @MockBean
    private PhotoDao photoDao;

    @MockBean
    private ManageProfileUseCase manageProfileUseCase;

    @BeforeEach
    void setUp() {
        var userNickname = User.builder()
                .name("nickname")
                .license(License.CC0_10)
                .id(42)
                .email("nickname@example.com")
                .ownPhotos(true)
                .anonymous(true)
                .admin(false)
                .emailVerification(User.EMAIL_VERIFIED)
                .sendNotifications(false)
                .build();
        when(userDao.findByEmail("nickname@example.com")).thenReturn(Optional.of(userNickname));
        var userSomeuser = User.builder()
                .name("someuser")
                .license(License.CC0_10)
                .email("someuser@example.com")
                .build();
        when(userDao.findByEmail("someuser@example.com")).thenReturn(Optional.of(userSomeuser));

        var key0815 = new Station.Key("ch", "0815");
        var station0815 = createStation(key0815, new Coordinates(40.1, 7.0), createPhoto(key0815, User.builder().name("Jim Knopf").url("photographerUrl").license(License.CC0_10).id(18).ownPhotos(true).anonymous(false).admin(false).emailVerification(User.EMAIL_VERIFIED).sendNotifications(true).build()));
        when(stationDao.findByKey(key0815.getCountry(), key0815.getId())).thenReturn(Optional.of(station0815));

        var key1234 = new Station.Key("de", "1234");
        var station1234 = createStation(key1234, new Coordinates(40.1, 7.0), createPhoto(key1234, createUser()));
        when(stationDao.findByKey(key1234.getCountry(), key1234.getId())).thenReturn(Optional.of(station1234));

        monitor.getMessages().clear();
    }

    private Station createStation(Station.Key key, Coordinates coordinates, Photo photo) {
        var station = Station.builder()
                .key(key)
                .title("Station" + key.getId())
                .coordinates(coordinates)
                .ds100("LAL")
                .build();
        if (photo != null) {
            station.getPhotos().add(photo);
        }
        return station;
    }

    private Photo createPhoto(Station.Key key0815, User Jim_Knopf) {
        return Photo.builder()
                .stationKey(key0815)
                .urlPath("URL")
                .photographer(Jim_Knopf)
                .license(License.CC0_10)
                .build();
    }

    @Test
    void userInbox() throws Exception {
        var user = User.builder().name("nickname").license(License.CC0_10).id(42).email("nickname@example.com").build();

        when(inboxDao.findById(1)).thenReturn(createInboxEntry(user, 1, "de", "4711", null, false));
        when(inboxDao.findById(2)).thenReturn(createInboxEntry(user, 2, "de", "1234", null, true));
        when(inboxDao.findById(3)).thenReturn(createInboxEntry(user, 3, "de", "5678", "rejected", true));
        when(inboxDao.findById(4)).thenReturn(createInboxEntry(user, 4, "ch", "0815", null, false));

        var inboxStateQueries = """
                [
                    {"id": 1},
                    {"id": 2},
                    {"id": 3},
                    {"id": 4}
                ]
                """;

        mvc.perform(post("/userInbox")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(inboxStateQueries)
                        .with(user(new AuthUser(User.builder().name("nickname").license(License.CC0_10).id(42).email("nickname@example.com").emailVerification(User.EMAIL_VERIFIED).build(), Collections.emptyList())))
                        .with(csrf()))
                .andExpect(validOpenApiResponse())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].state").value("REVIEW"))
                .andExpect(jsonPath("$.[0].filename").value("1.jpg"))
                .andExpect(jsonPath("$.[1].state").value("ACCEPTED"))
                .andExpect(jsonPath("$.[2].state").value("REJECTED"))
                .andExpect(jsonPath("$.[3].state").value("REVIEW"));
    }

    private InboxEntry createInboxEntry(User user, int id, String countryCode, String stationId, String rejectReason, boolean done) {
        return InboxEntry.builder()
                .id(id)
                .countryCode(countryCode)
                .stationId(stationId)
                .title("Station " + stationId)
                .coordinates(new Coordinates(50.1, 9.2))
                .photographerId(user.getId())
                .photographerNickname(user.getName())
                .extension("jpg")
                .done(done)
                .rejectReason(rejectReason)
                .createdAt(Instant.now())
                .build();
    }

    private ResultActions whenPostProblemReport(String emailVerification, String problemReportJson) throws Exception {
        return mvc.perform(post("/reportProblem")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(problemReportJson)
                        .with(user(new AuthUser(User.builder().name("@nick name").license(License.CC0_10).id(42).email("nickname@example.com").ownPhotos(true).anonymous(false).emailVerification(emailVerification).build(), Collections.emptyList())))
                        .with(csrf()))
                .andExpect(validOpenApiResponse());
    }

    @Test
    void postProblemReportOther() throws Exception {
        when(inboxDao.insert(InboxEntry.builder()
                .photographerId(42)
                .countryCode("de")
                .stationId("1234")
                .problemReportType(ProblemReportType.OTHER)
                .comment("something is wrong")
                .createdAt(clock.instant())
                .build())).thenReturn(6L);
        var problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "OTHER", "comment": "something is wrong" }
                """;

        whenPostProblemReport(User.EMAIL_VERIFIED, problemReportJson)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.filename").doesNotExist());

        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New problem report for Station1234 - de:1234
                OTHER: something is wrong
                by @nick name
                via UserAgent""");
    }

    @Test
    void postProblemReportWrongLocation() throws Exception {
        when(inboxDao.insert(InboxEntry.builder()
                .photographerId(42)
                .countryCode("de")
                .stationId("1234")
                .problemReportType(ProblemReportType.WRONG_LOCATION)
                .coordinates(new Coordinates(50.0, 9.1))
                .comment("coordinates are slightly off")
                .createdAt(clock.instant())
                .build())).thenReturn(6L);
        var problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "WRONG_LOCATION", "lat": 50.0, "lon": 9.1, "comment": "coordinates are slightly off" }
                """;

        whenPostProblemReport(User.EMAIL_VERIFIED, problemReportJson)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.filename").doesNotExist());

        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New problem report for Station1234 - de:1234
                WRONG_LOCATION: coordinates are slightly off
                by @nick name
                via UserAgent""");
    }

    @Test
    void postProblemReportWrongName() throws Exception {
        when(inboxDao.insert(InboxEntry.builder()
                .photographerId(42)
                .countryCode("de")
                .stationId("1234")
                .title("New Name")
                .problemReportType(ProblemReportType.WRONG_NAME)
                .comment("name is wrong")
                .createdAt(clock.instant())
                .build())).thenReturn(6L);
        var problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "WRONG_NAME", "title": "New Name", "comment": "name is wrong" }
                """;

        whenPostProblemReport(User.EMAIL_VERIFIED, problemReportJson)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.filename").doesNotExist());

        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New problem report for Station1234 - de:1234
                WRONG_NAME: name is wrong
                by @nick name
                via UserAgent""");
    }

    @Test
    void postProblemReportEmailNotVerified() throws Exception {
        var problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "OTHER", "comment": "something is wrong" }
                """;

        whenPostProblemReport("blah", problemReportJson)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.state").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    private User createUser() {
        return User.builder().name("Jim Knopf").url("photographerUrl").license(License.CC0_10).id(18).ownPhotos(true).anonymous(false).admin(false).emailVerification(User.EMAIL_VERIFIED).sendNotifications(true).build();
    }

}
