package org.railwaystations.rsapi.adapter.in.web.controller;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.services.PhotographersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotographersController.class)
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, PhotographersService.class})
@AutoConfigureMockMvc(addFilters = false)
class PhotographersControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StationDao stationDao;

    @MockBean
    private CountryDao countryDao;

    @BeforeEach
    void setup() {
        when(countryDao.findById("de")).thenReturn(Optional.of(Country.builder().name("de").build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/photographers?country=x", "/photographers?country=xyz"})
    void getPhotographersWithInvalidCountry(String urlTemplate) throws Exception {
        mvc.perform(get(urlTemplate))
                .andExpect(validOpenApiResponse())
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPhotographersOfCountryDe() throws Exception {
        when(stationDao.getPhotographerMap("de")).thenReturn(createPhotographersResponse());

        mvc.perform(get("/photographers?country=de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@user27").value(31))
                .andExpect(jsonPath("$.@user8").value(29))
                .andExpect(jsonPath("$.@user10").value(15))
                .andExpect(jsonPath("$.@user0").value(9))
                .andExpect(validOpenApiResponse());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/photographers"})
    void getPhotographersOfAllCountries(String urlTemplate) throws Exception {
        when(stationDao.getPhotographerMap(null)).thenReturn(createPhotographersResponse());

        mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@user27").value(31))
                .andExpect(jsonPath("$.@user8").value(29))
                .andExpect(jsonPath("$.@user10").value(15))
                .andExpect(jsonPath("$.@user0").value(9))
                .andExpect(validOpenApiResponse());
    }

    @NotNull
    private Map<String, Long> createPhotographersResponse() {
        return Map.of("@user27", 31L, "@user8", 29L, "@user10", 15L, "@user0", 9L);
    }

}