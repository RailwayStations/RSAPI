package org.railwaystations.rsapi.adapter.in.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.Statistic;
import org.railwaystations.rsapi.core.services.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatisticController.class)
@ContextConfiguration(classes = {WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, StatisticService.class})
@AutoConfigureMockMvc(addFilters = false)
class StatisticControllerTest {

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

    @Test
    void whenCountryIsInvalidThenReturnsStatus400() throws Exception {
        mvc.perform(get("/x/stats"))
                .andExpect(validOpenApiResponse())
                .andExpect(status().isBadRequest());
    }

    @Test
    void statisticAll() throws Exception {
        when(stationDao.getStatistic(null)).thenReturn(new Statistic(null, 954, 91, 6));

        mvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.total").value(954))
                .andExpect(jsonPath("$.withPhoto").value(91))
                .andExpect(jsonPath("$.withoutPhoto").value(863))
                .andExpect(jsonPath("$.photographers").value(6))
                .andExpect(jsonPath("$.countryCode").doesNotExist());
    }

    @Test
    void statisticDe() throws Exception {
        when(stationDao.getStatistic("de")).thenReturn(new Statistic("de", 729, 84, 4));

        mvc.perform(get("/de/stats"))
                .andExpect(status().isOk())
                .andExpect(validOpenApiResponse())
                .andExpect(jsonPath("$.total").value(729))
                .andExpect(jsonPath("$.withPhoto").value(84))
                .andExpect(jsonPath("$.withoutPhoto").value(645))
                .andExpect(jsonPath("$.photographers").value(4))
                .andExpect(jsonPath("$.countryCode").value("de"));
    }

}