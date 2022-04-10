package org.railwaystations.rsapi.adapter.in.web.controller;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.in.web.writer.StatisticTxtWriter;
import org.railwaystations.rsapi.core.model.Statistic;
import org.railwaystations.rsapi.core.ports.in.GetStatisticUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatisticController.class)
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, StatisticTxtWriter.class})
@AutoConfigureMockMvc(addFilters = false)
class StatisticControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetStatisticUseCase getStatisticUseCase;

    @Test
    void whenCountryIsInvalidThenReturnsStatus400() throws Exception {
        mvc.perform(get("/x/stats"))
                .andExpect(validOpenApi())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void statisticAllJson() throws Exception {
        when(getStatisticUseCase.getStatistic(null)).thenReturn(new Statistic(null, 954, 91, 6));

        mvc.perform(get("/stats.json"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.total").value(954))
                .andExpect(jsonPath("$.withPhoto").value(91))
                .andExpect(jsonPath("$.withoutPhoto").value(863))
                .andExpect(jsonPath("$.photographers").value(6))
                .andExpect(jsonPath("$.countryCode").doesNotExist());
    }

    private ResultMatcher validOpenApi() {
        return openApi().isValid("static/openapi.yaml");
    }

    @Test
    public void statisticDeJson() throws Exception {
        when(getStatisticUseCase.getStatistic("de")).thenReturn(new Statistic("de", 729, 84, 4));

        mvc.perform(get("/de/stats.json"))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(jsonPath("$.total").value(729))
                .andExpect(jsonPath("$.withPhoto").value(84))
                .andExpect(jsonPath("$.withoutPhoto").value(645))
                .andExpect(jsonPath("$.photographers").value(4))
                .andExpect(jsonPath("$.countryCode").value("de"));
    }

    @Test
    public void statisticDeTxt() throws Exception {
        when(getStatisticUseCase.getStatistic("de")).thenReturn(new Statistic("de", 729, 84, 4));

        mvc.perform(get("/de/stats.txt"))
            .andExpect(status().isOk())
            .andExpect(validOpenApi())
            .andExpect(content().string("""
                  name	value
                  total	729
                  withPhoto	84
                  withoutPhoto	645
                  photographers	4
                  countryCode	de
                  """));
    }

}