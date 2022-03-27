package org.railwaystations.rsapi.adapter.web.resources;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.web.writer.StatisticTxtWriter;
import org.railwaystations.rsapi.core.model.Statistic;
import org.railwaystations.rsapi.core.services.PhotoStationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StatisticResource.class)
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, StatisticTxtWriter.class})
@AutoConfigureMockMvc(addFilters = false)
class StatisticResourceTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PhotoStationsService photoStationsService;

    @Test
    void whenCountryIsInvalidThenReturnsStatus400() throws Exception {
        mvc.perform(get("/x/stats"))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void statisticAllJson() throws Exception {
        when(photoStationsService.getStatistic(null)).thenReturn(new Statistic(null, 954, 91, 6));

        mvc.perform(get("/stats.json"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(jsonPath("$.total").value(954))
                .andExpect(jsonPath("$.withPhoto").value(91))
                .andExpect(jsonPath("$.withoutPhoto").value(863))
                .andExpect(jsonPath("$.photographers").value(6))
                .andExpect(jsonPath("$.countryCode").doesNotExist());
    }

    @Test
    public void statisticDeJson() throws Exception {
        when(photoStationsService.getStatistic("de")).thenReturn(new Statistic("de", 729, 84, 4));

        mvc.perform(get("/de/stats.json"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(jsonPath("$.total").value(729))
                .andExpect(jsonPath("$.withPhoto").value(84))
                .andExpect(jsonPath("$.withoutPhoto").value(645))
                .andExpect(jsonPath("$.photographers").value(4))
                .andExpect(jsonPath("$.countryCode").value("de"));
    }

    @Test
    public void statisticDeTxt() throws Exception {
        when(photoStationsService.getStatistic("de")).thenReturn(new Statistic("de", 729, 84, 4));

        mvc.perform(get("/de/stats.txt"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(content().string(is("""
                      name	value
                      total	729
                      withPhoto	84
                      withoutPhoto	645
                      photographers	4
                      countryCode	de
                          """)));
    }

}