package org.railwaystations.rsapi.adapter.in.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.core.model.Coordinates;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StationsController.class)
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class})
@AutoConfigureMockMvc(addFilters = false)
public class StationsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PhotoStationsService photoStationsService;

    @BeforeEach
    public void setUp() {
        final var key5 = new Station.Key("xy", "5");
        final var stationXY = new Station(key5, "Lummerland", new Coordinates(50.0, 9.0), "XYZ", new Photo(key5, "/fotos/xy/5.jpg", createTestPhotographer("Jim Knopf", "photographerUrl", "CC0"), null, "CC0"), false);

        final var key3 = new Station.Key("ab", "3");
        final var stationAB = new Station(key3, "Nimmerland", new Coordinates(40.0, 6.0), "ABC", new Photo(key3, "/fotos/ab/3.jpg", createTestPhotographer("Peter Pan", "photographerUrl2", "CC0 by SA"), null, "CC0 by SA"), true);

        final var stationsAll = List.of(stationAB, stationXY);

        when(photoStationsService.findStationsBy(Collections.singleton("xy"), null, null, null, null, null, null)).thenReturn(List.of(stationXY));
        when(photoStationsService.findStationsBy(Collections.singleton("ab"), null, null, null, null, null, null)).thenReturn(List.of(stationAB));
        when(photoStationsService.findStationsBy(null, null, null, null, null, null, null)).thenReturn(stationsAll);
        when(photoStationsService.findStationsBy(allCountries(), null, null, null, null, null, null)).thenReturn(stationsAll);
        when(photoStationsService.findByCountryAndId("ab", "3")).thenReturn(Optional.of(stationAB));
    }

    private Set<String> allCountries() {
        return Set.of("ab", "xy");
    }

    @Test
    public void testGetXY() throws Exception {
        mvc.perform(get("/stations?country=xy"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(jsonPath("$.[0].country").value("xy"))
                .andExpect(jsonPath("$.[0].idStr").value("5"))
                .andExpect(jsonPath("$.[0].title").value("Lummerland"))
                .andExpect(jsonPath("$.[0].lat").value(50.0))
                .andExpect(jsonPath("$.[0].lon").value(9.0))
                .andExpect(jsonPath("$.[0].photographer").value("Jim Knopf"))
                .andExpect(jsonPath("$.[0].DS100").value("XYZ"))
                .andExpect(jsonPath("$.[0].photoUrl").value("/fotos/xy/5.jpg"))
                .andExpect(jsonPath("$.[0].license").value("CC0"))
                .andExpect(jsonPath("$.[0].photographerUrl").value("photographerUrl"))
                .andExpect(jsonPath("$.[0].active").value(false));
    }

    @Test
    public void testGetXYWithFilterActive() throws Exception {
        mvc.perform(get("/stations?country=xy&active=true"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void testGetAB() throws Exception {
        mvc.perform(get("/stations?country=ab"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(jsonPath("$.[0].country").value("ab"))
                .andExpect(jsonPath("$.[0].idStr").value("3"))
                .andExpect(jsonPath("$.[0].title").value("Nimmerland"))
                .andExpect(jsonPath("$.[0].lat").value(40.0))
                .andExpect(jsonPath("$.[0].lon").value(6.0))
                .andExpect(jsonPath("$.[0].photographer").value("Peter Pan"))
                .andExpect(jsonPath("$.[0].DS100").value("ABC"))
                .andExpect(jsonPath("$.[0].photoUrl").value("/fotos/ab/3.jpg"))
                .andExpect(jsonPath("$.[0].license").value("CC0 by SA"))
                .andExpect(jsonPath("$.[0].photographerUrl").value("photographerUrl2"))
                .andExpect(jsonPath("$.[0].active").value(true));
    }

    @Test
    public void testGetById() throws Exception {
        mvc.perform(get("/ab/stations/3"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(jsonPath("$.country").value("ab"))
                .andExpect(jsonPath("$.idStr").value("3"))
                .andExpect(jsonPath("$.title").value("Nimmerland"))
                .andExpect(jsonPath("$.lat").value(40.0))
                .andExpect(jsonPath("$.lon").value(6.0))
                .andExpect(jsonPath("$.photographer").value("Peter Pan"))
                .andExpect(jsonPath("$.DS100").value("ABC"))
                .andExpect(jsonPath("$.photoUrl").value("/fotos/ab/3.jpg"))
                .andExpect(jsonPath("$.license").value("CC0 by SA"))
                .andExpect(jsonPath("$.photographerUrl").value("photographerUrl2"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    public void testGetABXY() throws Exception {
        mvc.perform(get("/stations?country=ab&country=xy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]").isNotEmpty())
                .andExpect(jsonPath("$.[1]").isNotEmpty())
                .andExpect(jsonPath("$.[2]").doesNotExist());
    }

    @Test
    public void testGetAll() throws Exception {
        mvc.perform(get("/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0]").isNotEmpty())
                .andExpect(jsonPath("$.[1]").isNotEmpty())
                .andExpect(jsonPath("$.[2]").doesNotExist());
    }

    private User createTestPhotographer(final String name, final String url, final String license) {
        return new User(name, url, license, 0, null, true, false, null, false, null, true);
    }
}
