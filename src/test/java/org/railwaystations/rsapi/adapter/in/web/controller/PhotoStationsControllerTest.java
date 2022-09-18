package org.railwaystations.rsapi.adapter.in.web.controller;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.services.PhotoStationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotoStationsController.class)
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class})
@AutoConfigureMockMvc(addFilters = false)
class PhotoStationsControllerTest {

    static final Station.Key KEY_XY_1 = new Station.Key("xy", "1");
    static final Station.Key KEY_XY_5 = new Station.Key("xy", "5");
    static final User PHOTOGRAPHER_JIM_KNOPF = createTestPhotographer("Jim Knopf", "photographerUrlJim", License.CC0_10);
    static final Station.Key KEY_AB_3 = new Station.Key("ab", "3");
    static final Instant CREATED_AT = Instant.now();
    static final User PHOTOGRAPHER_PETER_PAN = createTestPhotographer("Peter Pan", "photographerUrlPeter", License.CC_BY_NC_SA_30_DE);

    @Autowired
    MockMvc mvc;

    @MockBean
    PhotoStationsService photoStationsService;

    @Test
    void get_photoStationsByCountry() throws Exception {
        var stationXY1 = createStationXY1();
        var stationXY5 = createStationXY5();
        stationXY5.getPhotos().add(createPhotoXY5());
        when(photoStationsService.findByCountry(Set.of("xy"), null, null)).thenReturn(Set.of(stationXY1, stationXY5));

        mvc.perform(get("/photoStationsByCountry/xy"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
                .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
                .andExpect(jsonPath("$.licenses[0].url").value("https://creativecommons.org/publicdomain/zero/1.0/"))
                .andExpect(jsonPath("$.licenses[1]").doesNotExist())
                .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
                .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
                .andExpect(jsonPath("$.photographers[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '5')].country").value("xy"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].title").value("Lummerland"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].lat").value(50.0))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].lon").value(9.0))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].shortCode").value("XYZ"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].inactive").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].id").value(0))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].path").value("/xy/5.jpg"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].license").value("CC0_10"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].outdated").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '1')].country").value("xy"))
                .andExpect(jsonPath("$.stations[?(@.id == '1')].title").value("Lummerland Ost"))
                .andExpect(jsonPath("$.stations[?(@.id == '1')].lat").value(50.1))
                .andExpect(jsonPath("$.stations[?(@.id == '1')].lon").value(9.1))
                .andExpect(jsonPath("$.stations[?(@.id == '1')].shortCode").value("XYY"))
                .andExpect(jsonPath("$.stations[?(@.id == '1')].inactive").value(true))
                .andExpect(jsonPath("$.stations[?(@.id == '1')].photos.size()").value(0))
                .andExpect(jsonPath("$.stations[2]").doesNotExist());
    }

    @Test
    void get_photoStationsByCountry_with_isActive_filter() throws Exception {
        when(photoStationsService.findByCountry(Set.of("xy"), null, false)).thenReturn(Set.of(createStationXY1()));

        mvc.perform(get("/photoStationsByCountry/xy?isActive=false"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses").isEmpty())
                .andExpect(jsonPath("$.photographers").isEmpty())
                .andExpect(jsonPath("$.stations[0].country").value("xy"))
                .andExpect(jsonPath("$.stations[0].id").value("1"))
                .andExpect(jsonPath("$.stations[0].title").value("Lummerland Ost"))
                .andExpect(jsonPath("$.stations[0].lat").value(50.1))
                .andExpect(jsonPath("$.stations[0].lon").value(9.1))
                .andExpect(jsonPath("$.stations[0].shortCode").value("XYY"))
                .andExpect(jsonPath("$.stations[0].inactive").value(true))
                .andExpect(jsonPath("$.stations[0].photos").isEmpty())
                .andExpect(jsonPath("$.stations[1]").doesNotExist());
    }

    @Test
    void get_photoStationsByCountry_with_hasPhoto_filter() throws Exception {
        var stationXY5 = createStationXY5();
        stationXY5.getPhotos().add(createPhotoXY5());
        when(photoStationsService.findByCountry(Set.of("xy"), true, null)).thenReturn(Set.of(stationXY5));

        mvc.perform(get("/photoStationsByCountry/xy?hasPhoto=true"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
                .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
                .andExpect(jsonPath("$.licenses[0].url").value("https://creativecommons.org/publicdomain/zero/1.0/"))
                .andExpect(jsonPath("$.licenses[1]").doesNotExist())
                .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
                .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
                .andExpect(jsonPath("$.photographers[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[0].country").value("xy"))
                .andExpect(jsonPath("$.stations[0].id").value("5"))
                .andExpect(jsonPath("$.stations[0].title").value("Lummerland"))
                .andExpect(jsonPath("$.stations[0].lat").value(50))
                .andExpect(jsonPath("$.stations[0].lon").value(9))
                .andExpect(jsonPath("$.stations[0].shortCode").value("XYZ"))
                .andExpect(jsonPath("$.stations[0].inactive").doesNotExist())
                .andExpect(jsonPath("$.stations[0].photos[0].id").value(0L))
                .andExpect(jsonPath("$.stations[0].photos[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.stations[0].photos[0].path").value("/xy/5.jpg"))
                .andExpect(jsonPath("$.stations[0].photos[0].license").value("CC0_10"))
                .andExpect(jsonPath("$.stations[0].photos[0].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[0].photos[0].outdated").doesNotExist())
                .andExpect(jsonPath("$.stations[0].photos[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[1]").doesNotExist());
    }

    @Test
    void get_photoStationsByCountry_with_unknown_country() throws Exception {
        mvc.perform(get("/photoStationsByCountry/00"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses").isEmpty())
                .andExpect(jsonPath("$.photographers").isEmpty())
                .andExpect(jsonPath("$.stations").isEmpty());
    }

    @Test
    void get_photoStationById_inactive_station_without_photos() throws Exception {
        when(photoStationsService.findByCountryAndId("xy", "1")).thenReturn(Optional.of(createStationXY1()));

        mvc.perform(get("/photoStationById/xy/1"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses").isEmpty())
                .andExpect(jsonPath("$.photographers").isEmpty())
                .andExpect(jsonPath("$.stations[0].country").value("xy"))
                .andExpect(jsonPath("$.stations[0].id").value("1"))
                .andExpect(jsonPath("$.stations[0].title").value("Lummerland Ost"))
                .andExpect(jsonPath("$.stations[0].lat").value(50.1))
                .andExpect(jsonPath("$.stations[0].lon").value(9.1))
                .andExpect(jsonPath("$.stations[0].shortCode").value("XYY"))
                .andExpect(jsonPath("$.stations[0].inactive").value(true))
                .andExpect(jsonPath("$.stations[0].photos").isEmpty())
                .andExpect(jsonPath("$.stations[1]").doesNotExist());
    }

    @Test
    void get_photoStationById_active_station_with_two_photos() throws Exception {
        var stationAB3 = createStationAB3();
        stationAB3.getPhotos().add(createPhotoAB3_1());
        stationAB3.getPhotos().add(createPhotoAB3_2());
        when(photoStationsService.findByCountryAndId("ab", "3")).thenReturn(Optional.of(stationAB3));

        mvc.perform(get("/photoStationById/ab/3"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses[0].id").value("CC_BY_NC_40_INT"))
                .andExpect(jsonPath("$.licenses[0].name").value("CC BY-NC 4.0 International"))
                .andExpect(jsonPath("$.licenses[0].url").value("https://creativecommons.org/licenses/by-nc/4.0/"))
                .andExpect(jsonPath("$.licenses[1].id").value("CC0_10"))
                .andExpect(jsonPath("$.licenses[1].name").value("CC0 1.0 Universell (CC0 1.0)"))
                .andExpect(jsonPath("$.licenses[1].url").value("https://creativecommons.org/publicdomain/zero/1.0/"))
                .andExpect(jsonPath("$.photographers[0].name").value("Peter Pan"))
                .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlPeter"))
                .andExpect(jsonPath("$.photographers[1].name").value("Jim Knopf"))
                .andExpect(jsonPath("$.photographers[1].url").value("photographerUrlJim"))
                .andExpect(jsonPath("$.stations[0].country").value("ab"))
                .andExpect(jsonPath("$.stations[0].id").value("3"))
                .andExpect(jsonPath("$.stations[0].title").value("Nimmerland"))
                .andExpect(jsonPath("$.stations[0].lat").value(40.0))
                .andExpect(jsonPath("$.stations[0].lon").value(6.0))
                .andExpect(jsonPath("$.stations[0].shortCode").value("ABC"))
                .andExpect(jsonPath("$.stations[0].inactive").doesNotExist())
                .andExpect(jsonPath("$.stations[0].photos[0].id").value(1L))
                .andExpect(jsonPath("$.stations[0].photos[0].photographer").value("Peter Pan"))
                .andExpect(jsonPath("$.stations[0].photos[0].path").value("/ab/3_1.jpg"))
                .andExpect(jsonPath("$.stations[0].photos[0].license").value("CC_BY_NC_40_INT"))
                .andExpect(jsonPath("$.stations[0].photos[0].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[0].photos[0].outdated").doesNotExist())
                .andExpect(jsonPath("$.stations[0].photos[1].id").value(2L))
                .andExpect(jsonPath("$.stations[0].photos[1].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.stations[0].photos[1].path").value("/ab/3_2.jpg"))
                .andExpect(jsonPath("$.stations[0].photos[1].license").value("CC0_10"))
                .andExpect(jsonPath("$.stations[0].photos[1].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[0].photos[1].outdated").value(true))
                .andExpect(jsonPath("$.stations[1]").doesNotExist());
    }

    @Test
    void get_photoStationById_not_found() throws Exception {
        mvc.perform(get("/photoStationById/ab/not_existing_id"))
                .andExpect(status().isNotFound())
                .andExpect(validOpenApi());
    }

    @Test
    void get_photoStationsByPhotographer_with_country_filter() throws Exception {
        var stationXY5 = createStationXY5();
        stationXY5.getPhotos().add(createPhotoXY5());
        when(photoStationsService.findByPhotographer("Jim Knopf", "xy")).thenReturn(Set.of(stationXY5));

        mvc.perform(get("/photoStationsByPhotographer/Jim Knopf?country=xy"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
                .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
                .andExpect(jsonPath("$.licenses[0].url").value("https://creativecommons.org/publicdomain/zero/1.0/"))
                .andExpect(jsonPath("$.licenses[1]").doesNotExist())
                .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
                .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
                .andExpect(jsonPath("$.photographers[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[0].country").value("xy"))
                .andExpect(jsonPath("$.stations[0].id").value("5"))
                .andExpect(jsonPath("$.stations[0].title").value("Lummerland"))
                .andExpect(jsonPath("$.stations[0].lat").value(50))
                .andExpect(jsonPath("$.stations[0].lon").value(9))
                .andExpect(jsonPath("$.stations[0].shortCode").value("XYZ"))
                .andExpect(jsonPath("$.stations[0].inactive").doesNotExist())
                .andExpect(jsonPath("$.stations[0].photos[0].id").value(0L))
                .andExpect(jsonPath("$.stations[0].photos[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.stations[0].photos[0].path").value("/xy/5.jpg"))
                .andExpect(jsonPath("$.stations[0].photos[0].license").value("CC0_10"))
                .andExpect(jsonPath("$.stations[0].photos[0].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[0].photos[0].outdated").doesNotExist())
                .andExpect(jsonPath("$.stations[0].photos[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[1]").doesNotExist());
    }

    @Test
    void get_photoStationsByPhotographer() throws Exception {
        var stationAB3 = createStationAB3();
        stationAB3.getPhotos().add(createPhotoAB3_2());
        var stationXY5 = createStationXY5();
        stationXY5.getPhotos().add(createPhotoXY5());
        when(photoStationsService.findByPhotographer("Jim Knopf", null)).thenReturn(Set.of(stationAB3, stationXY5));

        mvc.perform(get("/photoStationsByPhotographer/Jim Knopf"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
                .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
                .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
                .andExpect(jsonPath("$.licenses[0].url").value("https://creativecommons.org/publicdomain/zero/1.0/"))
                .andExpect(jsonPath("$.licenses[1]").doesNotExist())
                .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
                .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
                .andExpect(jsonPath("$.photographers[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '5')].country").value("xy"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].id").value("5"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].title").value("Lummerland"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].lat").value(50.0))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].lon").value(9.0))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].shortCode").value("XYZ"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].inactive").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].id").value(0))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].path").value("/xy/5.jpg"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].license").value("CC0_10"))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].outdated").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '3')].country").value("ab"))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].id").value("3"))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].title").value("Nimmerland"))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].lat").value(40.0))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].lon").value(6.0))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].shortCode").value("ABC"))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].inactive").doesNotExist())
                .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].id").value(2))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].path").value("/ab/3_2.jpg"))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].license").value("CC0_10"))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].outdated").value(true))
                .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[2]").doesNotExist());
    }

    static User createTestPhotographer(String name, String url, License license) {
        return User.builder()
                .id(0)
                .name(name)
                .license(license)
                .ownPhotos(true)
                .anonymous(false)
                .url(url)
                .build();
    }

    ResultMatcher validOpenApi() {
        return openApi().isValid("static/openapi.yaml");
    }

    Photo createPhotoAB3_2() {
        return Photo.builder()
                .id(2L)
                .stationKey(KEY_AB_3)
                .urlPath("/ab/3_2.jpg")
                .photographer(PHOTOGRAPHER_JIM_KNOPF)
                .license(License.CC0_10)
                .createdAt(CREATED_AT)
                .outdated(true)
                .build();
    }

    Photo createPhotoAB3_1() {
        return Photo.builder()
                .id(1L)
                .stationKey(KEY_AB_3)
                .urlPath("/ab/3_1.jpg")
                .photographer(PHOTOGRAPHER_PETER_PAN)
                .license(License.CC_BY_NC_40_INT)
                .createdAt(CREATED_AT)
                .build();
    }

    Station createStationAB3() {
        return Station.builder()
                .key(KEY_AB_3)
                .title("Nimmerland")
                .coordinates(new Coordinates(40.0, 6.0))
                .ds100("ABC")
                .active(true)
                .build();
    }

    Photo createPhotoXY5() {
        return Photo.builder()
                .id(0L)
                .stationKey(KEY_XY_5)
                .urlPath("/xy/5.jpg")
                .photographer(PHOTOGRAPHER_JIM_KNOPF)
                .license(License.CC0_10)
                .createdAt(CREATED_AT)
                .build();
    }

    Station createStationXY5() {
        return Station.builder()
                .key(KEY_XY_5)
                .title("Lummerland")
                .coordinates(new Coordinates(50.0, 9.0))
                .ds100("XYZ")
                .active(true)
                .build();
    }

    Station createStationXY1() {
        return Station.builder()
                .key(KEY_XY_1)
                .title("Lummerland Ost")
                .coordinates(new Coordinates(50.1, 9.1))
                .ds100("XYY")
                .active(false)
                .build();
    }

}
