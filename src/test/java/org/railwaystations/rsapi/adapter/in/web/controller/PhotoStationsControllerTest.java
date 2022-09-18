package org.railwaystations.rsapi.adapter.in.web.controller;

import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
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

    static final Instant CREATED_AT = Instant.now();
    @Autowired
    MockMvc mvc;

    @MockBean
    PhotoStationsService photoStationsService;

    @BeforeEach
    void setUp() {
        var key1 = new Station.Key("xy", "1");
        var stationXY1 = Station.builder()
                .key(key1)
                .title("Lummerland Ost")
                .coordinates(new Coordinates(50.1, 9.1))
                .ds100("XYY")
                .active(false)
                .build();

        var key5 = new Station.Key("xy", "5");
        var jimKnopf = createTestPhotographer("Jim Knopf", "photographerUrlJim", License.CC0_10);
        var stationXY5 = Station.builder()
                .key(key5)
                .title("Lummerland")
                .coordinates(new Coordinates(50.0, 9.0))
                .ds100("XYZ")
                .active(true)
                .build();
        stationXY5.getPhotos().add(Photo.builder()
                .id(0L)
                .stationKey(key5)
                .urlPath("/xy/5.jpg")
                .photographer(jimKnopf)
                .license(License.CC0_10)
                .createdAt(CREATED_AT)
                .build());

        var key3 = new Station.Key("ab", "3");
        var stationAB = Station.builder()
                .key(key3)
                .title("Nimmerland")
                .coordinates(new Coordinates(40.0, 6.0))
                .ds100("ABC")
                .active(true)
                .build();
        stationAB.getPhotos().add(Photo.builder()
                .id(1L)
                .stationKey(key3)
                .urlPath("/ab/3_1.jpg")
                .photographer(createTestPhotographer("Peter Pan", "photographerUrlPeter", License.CC_BY_NC_SA_30_DE))
                .license(License.CC_BY_NC_40_INT)
                .createdAt(CREATED_AT)
                .build());
        stationAB.getPhotos().add(Photo.builder()
                .id(2L)
                .stationKey(key3)
                .urlPath("/ab/3_2.jpg")
                .photographer(jimKnopf)
                .license(License.CC0_10)
                .createdAt(CREATED_AT)
                .outdated(true)
                .build());

        when(photoStationsService.findStationsBy(Set.of("xy"), null, null, null)).thenReturn(List.of(stationXY1, stationXY5));
        when(photoStationsService.findStationsBy(Set.of("xy"), null, null, false)).thenReturn(List.of(stationXY1));
        when(photoStationsService.findStationsBy(Set.of("xy"), true, null, null)).thenReturn(List.of(stationXY5));

        when(photoStationsService.findByCountryAndId("xy", "1")).thenReturn(Optional.of(stationXY1));
        when(photoStationsService.findByCountryAndId("ab", "3")).thenReturn(Optional.of(stationAB));
    }

    @Test
    void get_photoStationsByCountry() throws Exception {
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
                .andExpect(jsonPath("$.stations[0].country").value("xy"))
                .andExpect(jsonPath("$.stations[0].id").value("1"))
                .andExpect(jsonPath("$.stations[0].title").value("Lummerland Ost"))
                .andExpect(jsonPath("$.stations[0].lat").value(50.1))
                .andExpect(jsonPath("$.stations[0].lon").value(9.1))
                .andExpect(jsonPath("$.stations[0].shortCode").value("XYY"))
                .andExpect(jsonPath("$.stations[0].inactive").value(true))
                .andExpect(jsonPath("$.stations[0].photos").isEmpty())
                .andExpect(jsonPath("$.stations[1].country").value("xy"))
                .andExpect(jsonPath("$.stations[1].id").value("5"))
                .andExpect(jsonPath("$.stations[1].title").value("Lummerland"))
                .andExpect(jsonPath("$.stations[1].lat").value(50.0))
                .andExpect(jsonPath("$.stations[1].lon").value(9.0))
                .andExpect(jsonPath("$.stations[1].shortCode").value("XYZ"))
                .andExpect(jsonPath("$.stations[1].inactive").doesNotExist())
                .andExpect(jsonPath("$.stations[1].photos[0].id").value(0L))
                .andExpect(jsonPath("$.stations[1].photos[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.stations[1].photos[0].path").value("/xy/5.jpg"))
                .andExpect(jsonPath("$.stations[1].photos[0].license").value("CC0_10"))
                .andExpect(jsonPath("$.stations[1].photos[0].createdAt").value(CREATED_AT.toEpochMilli()))
                .andExpect(jsonPath("$.stations[1].photos[0].outdated").doesNotExist())
                .andExpect(jsonPath("$.stations[1].photos[1]").doesNotExist())
                .andExpect(jsonPath("$.stations[2]").doesNotExist());
    }

    @Test
    void get_photoStationsByCountry_with_isActive_filter() throws Exception {
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

    private ResultMatcher validOpenApi() {
        return openApi().isValid("static/openapi.yaml");
    }

    @Test
    void get_photoStationById_inactive_station_without_photos() throws Exception {
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

    private User createTestPhotographer(String name, String url, License license) {
        return User.builder()
                .id(0)
                .name(name)
                .license(license)
                .ownPhotos(true)
                .anonymous(false)
                .url(url)
                .build();
    }

}
