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
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.app.auth.WebSecurityConfig;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.services.InboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InboxController.class, properties = {"inboxBaseUrl=http://inbox.railway-stations.org"})
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, MockMvcTestConfiguration.class, WebSecurityConfig.class})
@Import({InboxService.class, PhotoFileStorage.class, RSUserDetailsService.class})
@ActiveProfiles("mockMvcTest")
class InboxControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MockMonitor monitor;

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

    @BeforeEach
    void setUp() {
        var key0815 = new Station.Key("ch", "0815");
        var station0815 = createStation(key0815, new Coordinates(40.1, 7.0), "LAL", createPhoto(key0815, createUser("Jim Knopf", 18)));
        var key4711 = new Station.Key("de", "4711");
        var station4711 = createStation(key4711, new Coordinates(50.0, 9.0), "XYZ", null);
        var key1234 = new Station.Key("de", "1234");
        var station1234 = createStation(key1234, new Coordinates(40.1, 7.0), "LAL", createPhoto(key1234, createUser("Jim Knopf")));
        var key5678 = new Station.Key("de", "5678");
        var station5678 = createStation(key5678, new Coordinates(51.0, 10.0), "DEF", createPhoto(key5678, createUser("nickname")));
        var key9876 = new Station.Key("de", "9876");
        var station9876 = createStation(key9876, new Coordinates(52.0, 8.0), "EFF", createPhoto(key9876, createUser("nickname", 42)));

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

        when(stationDao.findByKey(key4711.getCountry(), key4711.getId())).thenReturn(Set.of(station4711));
        when(stationDao.findByKey(key1234.getCountry(), key1234.getId())).thenReturn(Set.of(station1234));
        when(stationDao.findByKey(key5678.getCountry(), key5678.getId())).thenReturn(Set.of(station5678));
        when(stationDao.findByKey(key0815.getCountry(), key0815.getId())).thenReturn(Set.of(station0815));
        when(stationDao.findByKey(key9876.getCountry(), key9876.getId())).thenReturn(Set.of(station9876));

        monitor.getMessages().clear();
    }

    private Station createStation(Station.Key key, Coordinates coordinates, String ds100, Photo photo) {
        var station = Station.builder()
                .key(key)
                .title("Station" + key.getId())
                .coordinates(coordinates)
                .ds100(ds100)
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
    void testUserInbox() throws Exception {
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
                .andExpect(jsonPath("$.[3].state").value("CONFLICT"));
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
    void testPostProblemReport() throws Exception {
        when(inboxDao.insert(any())).thenReturn(6L);
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
    void testPostProblemReportEmailNotVerified() throws Exception {
        var problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "OTHER", "comment": "something is wrong" }
                """;

        whenPostProblemReport("blah", problemReportJson)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.state").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    private User createUser(String name) {
        return createUser(name, 0);
    }

    private User createUser(String name, int id) {
        return User.builder().name(name).url("photographerUrl").license(License.CC0_10).id(id).ownPhotos(true).anonymous(false).admin(false).emailVerification(User.EMAIL_VERIFIED).sendNotifications(true).build();
    }

}
