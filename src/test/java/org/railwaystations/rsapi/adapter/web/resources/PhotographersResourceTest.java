package org.railwaystations.rsapi.adapter.web.resources;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.web.writer.PhotographersTxtWriter;
import org.railwaystations.rsapi.core.services.PhotoStationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PhotographersResource.class)
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, PhotographersTxtWriter.class})
@AutoConfigureMockMvc(addFilters = false)
class PhotographersResourceTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PhotoStationsService photoStationsService;

    @ParameterizedTest
    @ValueSource(strings = {"/x/photographers", "/xyz/photographers", "/photographers?country=x", "/photographers?country=xyz",
            "/x/photographers.json", "/xyz/photographers.json",  "/photographers.json?country=x", "/photographers.json?country=xyz",
            "/x/photographers.txt", "/xyz/photographers.txt", "/photographers.txt?country=x", "/photographers.txt?country=xyz"})
    void whenCountryIsInvalidThenReturnsStatus400(final String urlTemplate) throws Exception {
        mvc.perform(get(urlTemplate))
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/de/photographers.json", "/de/photographers", "/photographers?country=de", "/photographers.json?country=de"})
    public void photographersDeJson(final String urlTemplate) throws Exception {
        when(photoStationsService.getPhotographerMap("de")).thenReturn(createPhotographersResponse());

        mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@user27").value(31))
                .andExpect(jsonPath("$.@user8").value(29))
                .andExpect(jsonPath("$.@user10").value(15))
                .andExpect(jsonPath("$.@user0").value(9))
                .andExpect(openApi().isValid("static/openapi.yaml"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/photographers.json", "/photographers"})
    public void photographersAllJson(final String urlTemplate) throws Exception {
        when(photoStationsService.getPhotographerMap(null)).thenReturn(createPhotographersResponse());

        mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@user27").value(31))
                .andExpect(jsonPath("$.@user8").value(29))
                .andExpect(jsonPath("$.@user10").value(15))
                .andExpect(jsonPath("$.@user0").value(9))
                .andExpect(openApi().isValid("static/openapi.yaml"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/de/photographers.txt", "/de/photographers.txt?country=de"})
    public void photographersDeTxt(final String urlTemplate) throws Exception {
        when(photoStationsService.getPhotographerMap("de")).thenReturn(createPhotographersResponse());

        mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andExpect(content().string(is("""
					count	photographer
					31	@user27
					29	@user8
					15	@user10
					9	@user0
					""")));
    }

    @NotNull
    private Map<String, Long> createPhotographersResponse() {
        return Map.of("@user27", 31L, "@user8", 29L, "@user10", 15L, "@user0", 9L);
    }

}