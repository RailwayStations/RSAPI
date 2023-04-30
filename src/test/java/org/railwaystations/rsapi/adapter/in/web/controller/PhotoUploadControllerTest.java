package org.railwaystations.rsapi.adapter.in.web.controller;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
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
import org.railwaystations.rsapi.core.model.License;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotoUploadController.class, properties = {"inboxBaseUrl=http://inbox.railway-stations.org"})
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, MockMvcTestConfiguration.class, WebSecurityConfig.class})
@Import({InboxService.class, PhotoFileStorage.class, RSUserDetailsService.class})
@ActiveProfiles("mockMvcTest")
class PhotoUploadControllerTest {

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


        var key4711 = new Station.Key("de", "4711");
        var station4711 = createStation(key4711, new Coordinates(50.0, 9.0), "XYZ", null);
        var key1234 = new Station.Key("de", "1234");
        var station1234 = createStation(key1234, new Coordinates(40.1, 7.0), "LAL", createPhoto(key1234, createUserJimKnopf()));

        when(stationDao.findByKey(key4711.getCountry(), key4711.getId())).thenReturn(Set.of(station4711));
        when(stationDao.findByKey(key1234.getCountry(), key1234.getId())).thenReturn(Set.of(station1234));

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

    private ResultActions whenPostImage(String nickname, int userId, String email, String stationId, String country,
                                        String stationTitle, Double latitude, Double longitude, String comment) throws Exception {
        return whenPostImage(nickname, userId, email, stationId, country, stationTitle, latitude, longitude, comment, User.EMAIL_VERIFIED);
    }

    private ResultActions whenPostImage(String nickname, int userId, String email, String stationId, String country,
                                        String stationTitle, Double latitude, Double longitude, String comment, String emailVerification) throws Exception {
        return whenPostPhotoUpload(nickname, userId, email, stationId, country, stationTitle, latitude, longitude, comment, emailVerification, "image-content".getBytes(Charset.defaultCharset()), "image/jpeg");
    }

    @NotNull
    private ResultActions whenPostPhotoUpload(String nickname, int userId, String email, String stationId, String country, String stationTitle, Double latitude, Double longitude, String comment, String emailVerification, byte[] inputBytes, String contentType) throws Exception {
        var headers = new HttpHeaders();
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
        headers.add("Content-Type", contentType);
        headers.add("User-Agent", "UserAgent");

        return mvc.perform(post("/photoUpload")
                        .headers(headers)
                        .content(inputBytes)
                        .with(user(new AuthUser(User.builder().name(nickname).license(License.CC0_10).id(userId).email(email).ownPhotos(true).emailVerification(emailVerification).build(), Collections.emptyList())))
                        .with(csrf()))
                .andExpect(validOpenApiResponse());
    }

    @Test
    void postMultipartFormdataEmailNotVerified() throws Exception {
        when(authenticator.authenticate(new UsernamePasswordAuthenticationToken("someuser@example.com", "secretUploadToken"))).thenReturn(new UsernamePasswordAuthenticationToken("", "", Collections.emptyList()));
        when(inboxDao.insert(any())).thenReturn(1L);
        var response = whenPostImageMultipartFormdata("someuser@example.com", "some_verification_token");

        assertThat(response).contains("UNAUTHORIZED");
        assertThat(response).contains("Profile incomplete, not allowed to upload photos");
        verify(inboxDao, never()).insert(any());
        assertThat(monitor.getMessages().size()).isEqualTo(0);
    }

    @Test
    void postPhotoForExistingStationViaMultipartFormdata() throws Exception {
        var uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);
        when(inboxDao.insert(any())).thenReturn(1L);
        var response = whenPostImageMultipartFormdata("nickname@example.com", User.EMAIL_VERIFIED);

        assertThat(response).contains("REVIEW");
        assertFileWithContentExistsInInbox("image-content", "1.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), "de", "4711", null, null);

        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New photo upload for Station4711 - de:4711
                Some Comment
                http://inbox.railway-stations.org/1.jpg
                by nickname
                via UserAgent""");
    }

    private String whenPostImageMultipartFormdata(String email,
                                                  String emailVerified) throws Exception {
        return mvc.perform(multipart("/photoUploadMultipartFormdata")
                        .file(new MockMultipartFile("file", "1.jpg", "image/jpeg", "image-content".getBytes(Charset.defaultCharset())))
                        .with(user(new AuthUser(User.builder().name("nickname").license(License.CC0_10).id(42).email(email).ownPhotos(true).emailVerification(emailVerified).build(), Collections.emptyList())))
                        .param("stationId", "4711")
                        .param("countryCode", "de")
                        .param("comment", "Some Comment")
                        .header("User-Agent", "UserAgent")
                        .header("Referer", "http://localhost/uploadPage.php")
                        .header("Accept", "application/json")
                        .with(csrf()))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void repostMissingStationWithoutPhotoViaMultipartFormdata() throws Exception {
        var uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);
        when(inboxDao.insert(any())).thenReturn(1L);
        var response = mvc.perform(multipart("/photoUploadMultipartFormdata")
                        .file(new MockMultipartFile("file", null, "application/octet-stream", (byte[]) null))
                        .with(user(new AuthUser(User.builder().name("nickname").license(License.CC0_10).id(42).email("nickname@example.com").ownPhotos(true).emailVerification(User.EMAIL_VERIFIED).build(), Collections.emptyList())))
                        .param("stationTitle", "Missing Station")
                        .param("latitude", "10")
                        .param("longitude", "20")
                        .param("active", "true")
                        .param("countryCode", "de")
                        .param("comment", "Some Comment")
                        .header("User-Agent", "UserAgent")
                        .header("Referer", "http://localhost/uploadPage.php")
                        .header("Accept", "application/json")
                        .with(csrf()))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("REVIEW");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), "de", null, "Missing Station", new Coordinates(10, 20));

        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                Report missing station Missing Station at https://map.railway-stations.org/index.php?mlat=10.0&mlon=20.0&zoom=18&layers=M
                Some Comment
                by nickname
                via UserAgent""");
    }

    @Test
    void uploadPhoto() throws Exception {
        var uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);
        when(inboxDao.insert(any())).thenReturn(1L);

        whenPostImage("@nick name", 42, "nickname@example.com", "4711", "de", null, null, null, "Some Comment")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("1.jpg"));


        assertFileWithContentExistsInInbox("image-content", "1.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), "de", "4711", null, null);
        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New photo upload for Station4711 - de:4711
                Some Comment
                http://inbox.railway-stations.org/1.jpg
                by @nick name
                via UserAgent""");
    }

    private void assertUpload(InboxEntry inboxEntry, String countryCode, String stationId, String title, Coordinates coordinates) {
        assertThat(inboxEntry.getCountryCode()).isEqualTo(countryCode);
        assertThat(inboxEntry.getStationId()).isEqualTo(stationId);
        assertThat(inboxEntry.getTitle()).isEqualTo(title);
        assertThat(inboxEntry.getPhotographerId()).isEqualTo(42);
        assertThat(inboxEntry.getComment()).isEqualTo("Some Comment");
        assertThat(Duration.between(inboxEntry.getCreatedAt(), Instant.now()).getSeconds() < 5).isTrue();
        if (coordinates != null) {
            assertThat(inboxEntry.getCoordinates()).isEqualTo(coordinates);
        } else {
            assertThat(inboxEntry.getCoordinates()).isNull();
        }
        assertThat(inboxEntry.isDone()).isFalse();
    }

    @Test
    void postMissingStation() throws Exception {
        when(inboxDao.insert(any())).thenReturn(4L);
        var uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);

        whenPostImage("@nick name", 42, "nickname@example.com", null, null, "Missing Station", 50.9876d, 9.1234d, "Some Comment")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.filename").value("4.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "4.jpg");
        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), null, null, "Missing Station", new Coordinates(50.9876, 9.1234));

        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                Photo upload for missing station Missing Station at https://map.railway-stations.org/index.php?mlat=50.9876&mlon=9.1234&zoom=18&layers=M
                Some Comment
                http://inbox.railway-stations.org/4.jpg
                by @nick name
                via UserAgent""");
    }

    @Test
    void postMissingStationWithoutPhoto() throws Exception {
        when(inboxDao.insert(any())).thenReturn(4L);
        var uploadCaptor = ArgumentCaptor.forClass(InboxEntry.class);

        whenPostPhotoUpload("@nick name", 42, "nickname@example.com", null, null, "Missing Station", 50.9876d, 9.1234d, "Some Comment", User.EMAIL_VERIFIED, null, "application/octet-stream")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.filename").doesNotExist());

        verify(inboxDao).insert(uploadCaptor.capture());
        assertUpload(uploadCaptor.getValue(), null, null, "Missing Station", new Coordinates(50.9876, 9.1234));

        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                Report missing station Missing Station at https://map.railway-stations.org/index.php?mlat=50.9876&mlon=9.1234&zoom=18&layers=M
                Some Comment
                by @nick name
                via UserAgent""");
    }

    @ParameterizedTest
    @CsvSource({"-91d, 9.1234d",
            "91d, 9.1234d",
            "50.9876d, -181d",
            "50.9876d, 181d",
    })
    void postMissingStationLatLonOutOfRange(Double latitude, Double longitude) throws Exception {
        whenPostImage("@nick name", 42, "nickname@example.com", null, null, "Missing Station", latitude, longitude, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.state").value("LAT_LON_OUT_OF_RANGE"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    @Test
    void postSomeUserWithTokenSalt() throws Exception {
        when(inboxDao.insert(any())).thenReturn(3L);
        whenPostImage("@someuser", 11, "someuser@example.com", "4711", "de", null, null, null, null)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.state").value("REVIEW"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.filename").value("3.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "3.jpg");
        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New photo upload for Station4711 - de:4711
                                
                http://inbox.railway-stations.org/3.jpg
                by @someuser
                via UserAgent""");
    }

    @Test
    void postDuplicateInbox() throws Exception {
        when(inboxDao.insert(any())).thenReturn(2L);
        when(inboxDao.countPendingInboxEntriesForStation(null, "de", "4711")).thenReturn(1);

        whenPostImage("@nick name", 42, "nickname@example.com", "4711", "de", null, null, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.state").value("CONFLICT"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.filename").value("2.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "2.jpg");
        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New photo upload for Station4711 - de:4711
                                
                http://inbox.railway-stations.org/2.jpg (possible duplicate!)
                by @nick name
                via UserAgent""");
    }

    private void assertFileWithContentExistsInInbox(String content, String filename) throws IOException {
        var image = workDir.getInboxDir().resolve(filename);
        assertThat(Files.exists(image)).isTrue();

        var inputBytes = content.getBytes(Charset.defaultCharset());
        var outputBytes = new byte[inputBytes.length];
        IOUtils.readFully(Files.newInputStream(image), outputBytes);
        assertThat(outputBytes).isEqualTo(inputBytes);
    }

    @Test
    void postDuplicate() throws Exception {
        when(inboxDao.insert(any())).thenReturn(5L);
        whenPostImage("@nick name", 42, "nickname@example.com", "1234", "de", null, null, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.state").value("CONFLICT"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.filename").value("5.jpg"));

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "5.jpg");
        assertThat(monitor.getMessages().get(0)).isEqualTo("""
                New photo upload for Station1234 - de:1234
                                
                http://inbox.railway-stations.org/5.jpg (possible duplicate!)
                by @nick name
                via UserAgent""");
    }

    @Test
    void postEmailNotVerified() throws Exception {
        whenPostImage("@nick name", 42, "nickname@example.com", "4711", "de", null, null, null, null, "blahblah")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.state").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    @Test
    void postInvalidCountry() throws Exception {
        whenPostImage("nickname", 42, "nickname@example.com", "4711", "xy", null, null, null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.state").value("NOT_ENOUGH_DATA"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.filename").doesNotExist());
    }

    private User createUserJimKnopf() {
        return User.builder().name("Jim Knopf").url("photographerUrl").license(License.CC0_10).id(0).ownPhotos(true).anonymous(false).admin(false).emailVerification(User.EMAIL_VERIFIED).sendNotifications(true).build();
    }

}
