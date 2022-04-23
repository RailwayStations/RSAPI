package org.railwaystations.rsapi.adapter.in.web.controller;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.core.ports.in.LoadPhotographersUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Map;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotographersController.class)
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class})
@AutoConfigureMockMvc(addFilters = false)
class PhotographersControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private LoadPhotographersUseCase loadPhotographersUseCase;

    @ParameterizedTest
    @ValueSource(strings = {"/x/photographers", "/xyz/photographers", "/photographers?country=x", "/photographers?country=xyz",
            "/x/photographers.json", "/xyz/photographers.json",  "/photographers.json?country=x", "/photographers.json?country=xyz",
            "/x/photographers.txt", "/xyz/photographers.txt", "/photographers.txt?country=x", "/photographers.txt?country=xyz"})
    void whenCountryIsInvalidThenReturnsStatus400(final String urlTemplate) throws Exception {
        mvc.perform(get(urlTemplate))
                .andExpect(validOpenApi())
                .andExpect(status().isBadRequest());
    }

    private ResultMatcher validOpenApi() {
        return openApi().isValid("static/openapi.yaml");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/de/photographers.json", "/de/photographers", "/photographers?country=de", "/photographers.json?country=de"})
    public void photographersDeJson(final String urlTemplate) throws Exception {
        when(loadPhotographersUseCase.getPhotographersPhotocountMap("de")).thenReturn(createPhotographersResponse());

        mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@user27").value(31))
                .andExpect(jsonPath("$.@user8").value(29))
                .andExpect(jsonPath("$.@user10").value(15))
                .andExpect(jsonPath("$.@user0").value(9))
                .andExpect(validOpenApi());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/photographers.json", "/photographers"})
    public void photographersAllJson(final String urlTemplate) throws Exception {
        when(loadPhotographersUseCase.getPhotographersPhotocountMap(null)).thenReturn(createPhotographersResponse());

        mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@user27").value(31))
                .andExpect(jsonPath("$.@user8").value(29))
                .andExpect(jsonPath("$.@user10").value(15))
                .andExpect(jsonPath("$.@user0").value(9))
                .andExpect(validOpenApi());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/de/photographers.txt", "/de/photographers.txt?country=de"})
    public void photographersDeTxt(final String urlTemplate) throws Exception {
        when(loadPhotographersUseCase.getPhotographersPhotocountMap("de")).thenReturn(createPhotographersResponse());

        mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(validOpenApi())
                .andExpect(content().string("""
					count	photographer
					31	@user27
					29	@user8
					15	@user10
					9	@user0
					"""));
    }

    @NotNull
    private Map<String, Long> createPhotographersResponse() {
        return Map.of("@user27", 31L, "@user8", 29L, "@user10", 15L, "@user0", 9L);
    }

}