package org.railwaystations.rsapi.adapter.in.web.controller;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.InboxDao;
import org.railwaystations.rsapi.adapter.out.db.PhotoDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.adapter.out.monitoring.MockMonitor;
import org.railwaystations.rsapi.adapter.out.photostorage.PhotoFileStorage;
import org.railwaystations.rsapi.adapter.out.photostorage.WorkDir;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider;
import org.railwaystations.rsapi.app.auth.RSUserDetailsService;
import org.railwaystations.rsapi.app.auth.WebSecurityConfig;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.InboxEntry;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.services.InboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InboxController.class, properties = {"inboxBaseUrl=http://inbox.railway-stations.org"})
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, MockMvcTestConfiguration.class, WebSecurityConfig.class})
@Import({InboxService.class, PhotoFileStorage.class, RSUserDetailsService.class})
@ActiveProfiles("mockMvcTest")
public class PhotoInboxEntryControllerTest {

    public static final String IMAGE_CONTENT = "image-content";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MockMonitor monitor;

    @Autowired
    private WorkDir workDir;

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
    public void setUp() {
        final Station.Key key0815 = new Station.Key("ch", "0815");
        final Station station0815 = new Station(key0815, "Station 0815", new Coordinates(40.1, 7.0), "LAL", new Photo(key0815, "URL", createUser("Jim Knopf", 18), null, "CC0"), true);
        final Station.Key key4711 = new Station.Key("de", "4711");
        final Station station4711 = new Station(key4711, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", null, true);
        final Station.Key key1234 = new Station.Key("de", "1234");
        final Station station1234 = new Station(key1234, "Neverland", new Coordinates(51.0, 10.0), "ABC", new Photo(key1234, "URL", createUser("Jim Knopf"), null, "CC0"), true);
        final Station.Key key5678 = new Station.Key("de", "5678");
        final Station station5678 = new Station(key5678, "Phantasia", new Coordinates(51.0, 10.0), "DEF", new Photo(key5678, "URL", createUser("nickname"), null, "CC0"), true);
        final Station.Key key9876 = new Station.Key("de", "9876");
        final Station station9876 = new Station(key9876, "Station 9876", new Coordinates(52.0, 8.0), "EFF", new Photo(key9876, "URL", createUser("nickname", 42), null, "CC0"), true);

        final User userNickname = new User("nickname", null, "CC0", 42, "nickname@example.com", true, true, null, false, User.EMAIL_VERIFIED, false);
        when(userDao.findByEmail("nickname@example.com")).thenReturn(Optional.of(userNickname));
        final User userSomeuser = new User("someuser", "someuser@example.com", "CC0", true, null, true, null, true);
        when(userDao.findByEmail("someuser@example.com")).thenReturn(Optional.of(userSomeuser));

        when(stationDao.findByKey(key4711.getCountry(), key4711.getId())).thenReturn(Set.of(station4711));
        when(stationDao.findByKey(key1234.getCountry(), key1234.getId())).thenReturn(Set.of(station1234));
        when(stationDao.findByKey(key5678.getCountry(), key5678.getId())).thenReturn(Set.of(station5678));
        when(stationDao.findByKey(key0815.getCountry(), key0815.getId())).thenReturn(Set.of(station0815));
        when(stationDao.findByKey(key9876.getCountry(), key9876.getId())).thenReturn(Set.of(station9876));

        monitor.getMessages().clear();
    }

    private ResultActions whenPostImage(final String nickname, final int userId, final String email, final String stationId, final String country,
                                        final String stationTitle, final Double latitude, final Double longitude, final String comment) throws Exception {
        return whenPostImage(nickname, userId, email, stationId, country, stationTitle, latitude, longitude, comment, User.EMAIL_VERIFIED);
    }

    private ResultActions whenPostImage(final String nickname, final int userId, final String email, final String stationId, final String country,
                                        final String stationTitle, final Double latitude, final Double longitude, final String comment, final String emailVerification) throws Exception {
        final byte[] inputBytes = "image-content".getBytes(Charset.defaultCharset());

        final HttpHeaders headers = new HttpHeaders();
        if (country != null) {
            headers.add("Country", country);
        }
        if (stationId != null) {
            headers.add("Station-Id", stationId);
        }
        if (stationTitle != null) {
            headers.add("Station-Title", stationTitle);
        }
        if (latitude != null) {
            headers.add("Latitude", String.valueOf(latitude));
        }
        if (longitude != null) {
            headers.add("Longitude", String.valueOf(longitude));
        }
        if (comment != null) {
            headers.add("Comment", comment);
        }
        headers.add("Content-Type", "image/jpeg");
        headers.add("User-Agent", "UserAgent");

        return mvc.perform(post("/photoUpload")
                        .headers(headers)
                        .content(inputBytes)
                        .with(user(new AuthUser(new User(nickname, null, "CC0", userId, email, true, false, null, false, emailVerification, true), Collections.emptyList())))
                        .with(csrf()))
                .andExpect(validOpenApi());
    }

    private ResultMatcher validOpenApi() {
        return openApi().isValid("static/openapi.yaml");
    }

    @Test
    public void testPostIframeUnauthorized() throws Exception {
        when(authenticator.authenticate(new UsernamePasswordAuthenticationToken("unknown@example.com", "secretUploadToken"))).thenReturn(null);
        when(inboxDao.insert(any())).thenReturn(1);
        final String response = whenPostImageIframe("unknown@example.com", "http://localhost/uploadPage.php");

        assertThat(response, containsString("UNAUTHORIZED"));
        verify(inboxDao, never()).insert(any());
        assertThat(monitor.getMessages().size(), is(0));
    }

    @Test
    public void testPostIframeEmailNotVerified() throws Exception {
        when(authenticator.authenticate(new UsernamePasswordAuthenticationToken("someuser@example.com", "secretUploadToken"))).thenReturn(new UsernamePasswordAuthenticationToken("","", Collections.emptyList()));
        when(inboxDao.insert(any())).thenReturn(1);
        final String response = whenPostImageIframe("someuser@example.com", "http://localhost/uploadPage.php");

        assertThat(response, containsString("UNAUTHORIZED"));
        assertThat(response, containsString("Email not verified"));
        verify(inboxDao, never()).insert(any());
        assertThat(monitor.getMessages().size(), is(0));
    }

    @Test
    public void testPostIframeMaliciousReferer() {
        when(authenticator.authenticate(new UsernamePasswordAuthenticationToken("nickname@example.com", "secretUploadToken"))).thenReturn(new UsernamePasswordAuthenticationToken("","", Collections.emptyList()));
        when(inboxDao.insert(any())).thenReturn(1);
        try {
            final String response = whenPostImageIframe("nickname@example.com", "http://localhost/uploadPage.php<script>alert('FooBar!');</script>");
            fail("IllegalArgumentException expected, but got: " + response);
        } catch (final Exception e) {
            assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
        }
        verify(inboxDao, never()).insert(any());
        assertThat(monitor.getMessages().size(), is(0));
    }

    @Test
    public void testPostIframe() throws Exception {
        final ArgumentCaptor<InboxEntry> uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);
        when(authenticator.authenticate(new UsernamePasswordAuthenticationToken("nickname@example.com", "secretUploadToken"))).thenReturn(new UsernamePasswordAuthenticationToken("","", Collections.emptyList()));
        when(inboxDao.insert(any())).thenReturn(1);
        final String response = whenPostImageIframe("nickname@example.com", "http://localhost/uploadPage.php");

        assertThat(response, containsString("REVIEW"));
        assertFileWithContentExistsInInbox("image-content", "1.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), "de","4711", null, null);

        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland - de:4711\nSome Comment\nhttp://inbox.railway-stations.org/1.jpg\nby nickname\nvia UserAgent"));
    }

    private String whenPostImageIframe(final String email,
                                       final String referer) throws Exception {
        return mvc.perform(multipart("/photoUpload")
                        .file(new MockMultipartFile("file", "1.jpg", "image/jpeg", "image-content".getBytes(Charset.defaultCharset())))
                        .param("email", email)
                        .param("uploadToken", "secretUploadToken")
                        .param("stationId", "4711")
                        .param("countryCode", "de")
                        .param("comment", "Some Comment")
                        .header("User-Agent", "UserAgent")
                        .header("Referer", referer)
                        .header("Accept", "text/html")
                        .with(csrf()))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void testUploadPhoto() throws Exception {
        final var uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);
        when(inboxDao.insert(any())).thenReturn(1);

        whenPostImage("@nick name", 42, "nickname@example.com","4711", "de", null, null, null, "Some Comment")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("1.jpg"));


        assertFileWithContentExistsInInbox("image-content", "1.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), "de","4711", null, null);
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland - de:4711\nSome Comment\nhttp://inbox.railway-stations.org/1.jpg\nby @nick name\nvia UserAgent"));
    }

    private void assertUpload(final InboxEntry inboxEntry, final String countryCode, final String stationId, final String title, final Coordinates coordinates) {
        assertThat(inboxEntry.getCountryCode(), equalTo(countryCode));
        assertThat(inboxEntry.getStationId(), equalTo(stationId));
        assertThat(inboxEntry.getTitle(), equalTo(title));
        assertThat(inboxEntry.getPhotographerId(), equalTo(42));
        assertThat(inboxEntry.getComment(), equalTo("Some Comment"));
        assertThat(Duration.between(inboxEntry.getCreatedAt(), Instant.now()).getSeconds() < 5, equalTo(true));
        if (coordinates != null) {
            assertThat(inboxEntry.getCoordinates(), equalTo(coordinates));
        } else {
            assertThat(inboxEntry.getCoordinates(), nullValue());
        }
        assertThat(inboxEntry.isDone(), equalTo(false));
    }

    @Test
    public void testPostMissingStation() throws Exception {
        when(inboxDao.insert(any())).thenReturn(4);
        final var uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);

        whenPostImage("@nick name", 42, "nickname@example.com",null, null, "Missing Station", 50.9876d, 9.1234d, "Some Comment")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.filename").value("4.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "4.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), null,null, "Missing Station", new Coordinates(50.9876, 9.1234));

        assertThat(monitor.getMessages().get(0), equalTo("Photo upload for missing station Missing Station at https://map.railway-stations.org/index.php?mlat=50.9876&mlon=9.1234&zoom=18&layers=M\nSome Comment\nhttp://inbox.railway-stations.org/4.jpg\nby @nick name\nvia UserAgent"));
    }

    @ParameterizedTest
    @CsvSource({"-91d, 9.1234d",
                "91d, 9.1234d",
                "50.9876d, -181d",
                "50.9876d, 181d",
    })
    public void testPostMissingStationLatLonOutOfRange(final Double latitude, final Double longitude) throws Exception {
        whenPostImage("@nick name", 42, "nickname@example.com",null, null, "Missing Station", latitude, longitude, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.state").value("LAT_LON_OUT_OF_RANGE"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    @Test
    public void testPostSomeUserWithTokenSalt() throws Exception {
        when(inboxDao.insert(any())).thenReturn(3);
        whenPostImage("@someuser", 11, "someuser@example.com","4711", "de", null, null, null, null)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.filename").value("3.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "3.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland - de:4711\n\nhttp://inbox.railway-stations.org/3.jpg\nby @someuser\nvia UserAgent"));
    }

    @Test
    public void testPostDuplicateInbox() throws Exception {
        when(inboxDao.insert(any())).thenReturn(2);
        when(inboxDao.countPendingInboxEntriesForStation(null, "de", "4711")).thenReturn(1);

        whenPostImage("@nick name", 42, "nickname@example.com","4711", "de", null, null, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.state").value("CONFLICT"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.filename").value("2.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "2.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Lummerland - de:4711\n\nhttp://inbox.railway-stations.org/2.jpg (possible duplicate!)\nby @nick name\nvia UserAgent"));
    }

    @Test
    public void testUserInbox() throws Exception {
        final User user = new User("nickname", null, "CC0", 42, "nickname@example.com", true, false, null, false, null, true);

        when(inboxDao.findById(1)).thenReturn(new InboxEntry(1, "de", "4711", "Station 4711", new Coordinates(50.1,9.2), user.getId(), user.getName(), null, "jpg", null, null, Instant.now(), false, null, false, false, null, null, null, false));
        when(inboxDao.findById(2)).thenReturn(new InboxEntry(2, "de", "1234", "Station 1234", new Coordinates(50.1,9.2), user.getId(), user.getName(), null, "jpg", null, null, Instant.now(), true, null, false, false, null, null, null, false));
        when(inboxDao.findById(3)).thenReturn(new InboxEntry(3, "de", "5678", "Station 5678", new Coordinates(50.1,9.2), user.getId(), user.getName(), null, "jpg", null, "rejected", Instant.now(), true, null, false, false, null, null, null, false));
        when(inboxDao.findById(4)).thenReturn(new InboxEntry(4, "ch", "0815", "Station 0815", new Coordinates(50.1,9.2), user.getId(), user.getName(), null, "jpg", null, null, Instant.now(), false, null, false, false, null, null, null, false));

        final var inboxStateQueries = """
                [
                    {"id": 1, "countryCode": "de", "stationId": "4711"},
                    {"id": 2, "countryCode": "de", "stationId": "1234"},
                    {"id": 3, "countryCode": "de", "stationId": "5678"},
                    {"id": 4, "countryCode": "ch", "stationId": "0815"}
                ]
                """;

        mvc.perform(post("/userInbox")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(inboxStateQueries)
                        .with(user(new AuthUser(new User("nickname", null, "CC0", 42, "nickname@example.com", true, false, null, false, User.EMAIL_VERIFIED, true), Collections.emptyList())))
                        .with(csrf()))
                .andExpect(validOpenApi())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].state").value("REVIEW"))
                .andExpect(jsonPath("$.[0].filename").value("1.jpg"))
                .andExpect(jsonPath("$.[1].state").value("ACCEPTED"))
                .andExpect(jsonPath("$.[2].state").value("REJECTED"))
                .andExpect(jsonPath("$.[3].state").value("CONFLICT"));
    }

    private void assertFileWithContentExistsInInbox(final String content, final String filename) throws IOException {
        final var image = workDir.getInboxDir().resolve(filename);
        assertThat(Files.exists(image), equalTo(true));

        final var inputBytes = content.getBytes(Charset.defaultCharset());
        final var outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(Files.newInputStream(image), outputBytes);
        assertThat(outputBytes, equalTo(inputBytes));
    }

    @Test
    public void testPostDuplicate() throws Exception {
        when(inboxDao.insert(any())).thenReturn(5);
        whenPostImage("@nick name", 42, "nickname@example.com","1234", "de", null, null, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.state").value("CONFLICT"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.filename").value("5.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "5.jpg");
        assertThat(monitor.getMessages().get(0), equalTo("New photo upload for Neverland - de:1234\n\nhttp://inbox.railway-stations.org/5.jpg (possible duplicate!)\nby @nick name\nvia UserAgent"));
    }

    @Test
    public void testPostEmailNotVerified() throws Exception {
        whenPostImage("@nick name", 42, "nickname@example.com","1234", "de", null, null, null, null, User.EMAIL_VERIFICATION_TOKEN + "blahblah")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.state").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    @Test
    public void testPostInvalidCountry() throws Exception {
        whenPostImage("nickname", 42, "nickname@example.com", "4711", "xy", null, null, null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.state").value("NOT_ENOUGH_DATA"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    private ResultActions whenPostProblemReport(final String emailVerification) throws Exception {
        final var problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "OTHER", "comment": "something is wrong" }
                """;
        return mvc.perform(post("/reportProblem")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(problemReportJson)
                        .with(user(new AuthUser(new User("@nick name", null, "CC0", 42, "nickname@example.com", true, false, null, false, emailVerification, true), Collections.emptyList())))
                        .with(csrf()))
                .andExpect(validOpenApi());
    }

    @Test
    public void testPostProblemReport() throws Exception {
        when(inboxDao.insert(any())).thenReturn(6);

        whenPostProblemReport(User.EMAIL_VERIFIED)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.filename").doesNotExist());

        assertThat(monitor.getMessages().get(0), equalTo("New problem report for Neverland - de:1234\nOTHER: something is wrong\nby @nick name\nvia UserAgent"));
    }

    @Test
    public void testPostProblemReportEmailNotVerified() throws Exception {
        whenPostProblemReport(User.EMAIL_VERIFICATION_TOKEN + "blah")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.state").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    private User createUser(final String name) {
        return createUser(name, 0);
    }

    private User createUser(final String name, final int id) {
        return new User(name, "photographerUrl", "CC0", id, null, true, false, null, false, User.EMAIL_VERIFIED, true);
    }


}
