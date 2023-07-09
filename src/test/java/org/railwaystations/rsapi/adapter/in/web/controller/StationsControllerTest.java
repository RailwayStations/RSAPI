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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StationsController.class)
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class})
@AutoConfigureMockMvc(addFilters = false)
class StationsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PhotoStationsService photoStationsService;
    private Set<Station> stationsAll;

    @BeforeEach
    void setUp() {
        var key5 = new Station.Key("de", "5");
        var stationXY = Station.builder()
                .key(key5)
                .title("Lummerland")
                .coordinates(new Coordinates(50.0, 9.0))
                .ds100("XYZ")
                .active(false)
                .build();
        stationXY.getPhotos().add(Photo.builder()
                .stationKey(key5)
                .urlPath("/de/5.jpg")
                .photographer(createTestPhotographer("Jim Knopf", "photographerUrl", License.CC0_10))
                .license(License.CC0_10)
                .primary(true)
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
                .stationKey(key3)
                .urlPath("/ab/3.jpg")
                .photographer(createTestPhotographer("Peter Pan", "photographerUrl2", License.CC_BY_NC_SA_30_DE))
                .license(License.CC_BY_NC_40_INT)
                .primary(true)
                .build());

        stationsAll = Set.of(stationAB, stationXY);

        when(photoStationsService.findByCountry(Collections.singleton("de"), null, null, null)).thenReturn(Set.of(stationXY));
        when(photoStationsService.findByCountry(Collections.singleton("ab"), null, null, null)).thenReturn(Set.of(stationAB));
        when(photoStationsService.findByCountry(Set.of("ab", "de"), null, null, null)).thenReturn(stationsAll);
        when(photoStationsService.findByCountryAndId("ab", "3")).thenReturn(Optional.of(stationAB));
    }

    @Test
    void getStationsByCountryXY() throws Exception {
        mvc.perform(get("/stations?country=de"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.[0].country").value("de"))
                .andExpect(jsonPath("$.[0].idStr").value("5"))
                .andExpect(jsonPath("$.[0].title").value("Lummerland"))
                .andExpect(jsonPath("$.[0].lat").value(50.0))
                .andExpect(jsonPath("$.[0].lon").value(9.0))
                .andExpect(jsonPath("$.[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.[0].DS100").value("XYZ"))
                .andExpect(jsonPath("$.[0].photoUrl").value("http://localhost:8080/photos/de/5.jpg"))
                .andExpect(jsonPath("$.[0].license").value("CC0 1.0 Universell (CC0 1.0)"))
                .andExpect(jsonPath("$.[0].photographerUrl").value("photographerUrl"))
                .andExpect(jsonPath("$.[0].active").value(false));
    }

    @Test
    void getStationsByCountryXYWithFilterActive() throws Exception {
        mvc.perform(get("/stations?country=de&active=true"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getStationsByCountryAB() throws Exception {
        mvc.perform(get("/stations?country=ab"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.[0].country").value("ab"))
                .andExpect(jsonPath("$.[0].idStr").value("3"))
                .andExpect(jsonPath("$.[0].title").value("Nimmerland"))
                .andExpect(jsonPath("$.[0].lat").value(40.0))
                .andExpect(jsonPath("$.[0].lon").value(6.0))
                .andExpect(jsonPath("$.[0].photographer").value("Peter Pan"))
                .andExpect(jsonPath("$.[0].DS100").value("ABC"))
                .andExpect(jsonPath("$.[0].photoUrl").value("http://localhost:8080/photos/ab/3.jpg"))
                .andExpect(jsonPath("$.[0].license").value("CC BY-NC 4.0 International"))
                .andExpect(jsonPath("$.[0].photographerUrl").value("photographerUrl2"))
                .andExpect(jsonPath("$.[0].active").value(true));
    }

    @Test
    void getStationById() throws Exception {
        mvc.perform(get("/ab/stations/3"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.country").value("ab"))
                .andExpect(jsonPath("$.idStr").value("3"))
                .andExpect(jsonPath("$.title").value("Nimmerland"))
                .andExpect(jsonPath("$.lat").value(40.0))
                .andExpect(jsonPath("$.lon").value(6.0))
                .andExpect(jsonPath("$.photographer").value("Peter Pan"))
                .andExpect(jsonPath("$.DS100").value("ABC"))
                .andExpect(jsonPath("$.photoUrl").value("http://localhost:8080/photos/ab/3.jpg"))
                .andExpect(jsonPath("$.license").value("CC BY-NC 4.0 International"))
                .andExpect(jsonPath("$.photographerUrl").value("photographerUrl2"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getStationsByCountriesABandDE() throws Exception {
        mvc.perform(get("/stations?country=ab&country=de"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.[0]").isNotEmpty())
                .andExpect(jsonPath("$.[1]").isNotEmpty())
                .andExpect(jsonPath("$.[2]").doesNotExist());
    }

    @Test
    void getAllStationsDefaultsToDE() throws Exception {
        mvc.perform(get("/stations"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.[0]").isNotEmpty())
                .andExpect(jsonPath("$.[0].country").value("de"))
                .andExpect(jsonPath("$.[1]").doesNotExist())
                .andExpect(jsonPath("$.[2]").doesNotExist());
    }

    @Test
    void getStationsIsLimitedToThreeCountries() throws Exception {
        when(photoStationsService.findByCountry(Set.of("ab", "de", "xy"), null, null, null)).thenReturn(stationsAll);

        mvc.perform(get("/stations?country=ab&country=de&country=xy&country=zz"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.[0]").isNotEmpty())
                .andExpect(jsonPath("$.[1]").isNotEmpty())
                .andExpect(jsonPath("$.[2]").doesNotExist());
    }

    User createTestPhotographer(String name, String url, License license) {
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
