package org.railwaystations.rsapi.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.out.db.CountryDao;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.ProviderApp;
import org.railwaystations.rsapi.core.services.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CountriesController.class)
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, CountryService.class})
@AutoConfigureMockMvc(addFilters = false)
class CountriesControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CountryDao countryDao;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @ValueSource(strings = {"/countries", "/countries.json"})
    void testList(String urlTemplate) throws Exception {
        when(countryDao.list(true)).thenReturn(createCountryList());

        var contentAsString = mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andReturn().getResponse().getContentAsString();

        List<Country> countries = objectMapper.readerForListOf(Country.class).readValue(contentAsString);
        assertThat(countries.size()).isEqualTo(2);
        countries.forEach(this::assertCountry);
    }

    @NotNull
    private Set<Country> createCountryList() {
        return Set.of(createCountry("xy"), createCountry("ab"));
    }

    @NotNull
    private Country createCountry(String code) {
        var country = Country.builder()
                .code(code)
                .name("name-" + code)
                .email("email-" + code)
                .twitterTags("twitter-" + code)
                .timetableUrlTemplate("timetable-" + code)
                .overrideLicense("overrideLicense-" + code)
                .active(true)
                .build();
        country.getProviderApps().add(createProviderApp("android", code));
        country.getProviderApps().add(createProviderApp("ios", code));
        country.getProviderApps().add(createProviderApp("web", code));
        return country;
    }

    private ProviderApp createProviderApp(String type, String code) {
        return ProviderApp.builder().type(type).name("Provider-" + code).url(type + "App-" + code).build();
    }

    private void assertCountry(Country country) {
        assertThat(country.getName()).isEqualTo("name-" + country.getCode());
        assertThat(country.getEmail()).isEqualTo("email-" + country.getCode());
        assertThat(country.getTwitterTags()).isEqualTo("twitter-" + country.getCode());
        assertThat(country.getTimetableUrlTemplate()).isEqualTo("timetable-" + country.getCode());
        assertThat(country.getOverrideLicense()).isEqualTo("overrideLicense-" + country.getCode());
        assertThat(country.getProviderApps().size()).isEqualTo(3);
        country.getProviderApps().forEach(app -> {
            switch (app.getType()) {
                case "android" -> assertThat(app.getUrl()).isEqualTo("androidApp-" + country.getCode());
                case "ios" -> assertThat(app.getUrl()).isEqualTo("iosApp-" + country.getCode());
                case "web" -> assertThat(app.getUrl()).isEqualTo("webApp-" + country.getCode());
                default -> fail("unknown app type");
            }
        });
    }

}
