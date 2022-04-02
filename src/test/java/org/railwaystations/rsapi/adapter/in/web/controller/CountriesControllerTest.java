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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CountriesController.class)
@ContextConfiguration(classes={WebMvcTestApplication.class, ErrorHandlingControllerAdvice.class, CountryService.class})
@AutoConfigureMockMvc(addFilters = false)
public class CountriesControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CountryDao countryDao;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @ValueSource(strings = {"/countries", "/countries.json"})
    public void testList(final String urlTemplate) throws Exception {
        when(countryDao.list(true)).thenReturn(createCountryList());

        final var contentAsString = mvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("static/openapi.yaml"))
                .andReturn().getResponse().getContentAsString();

        final List<Country> countries = objectMapper.readerForListOf(Country.class).readValue(contentAsString);
        assertThat(countries.size(), equalTo(2));
        countries.forEach(this::assertCountry);
    }

    @NotNull
    private Set<Country> createCountryList() {
        return Set.of(createCountry("xy"), createCountry("ab"));
    }

    @NotNull
    private Country createCountry(final String code) {
        final var xy = new Country(code, "name-" + code, "email-" + code, "twitter-" + code, "timetable-" + code, "overrideLicense-" + code, true);
        xy.getProviderApps().add(new ProviderApp("android", "Provider-" + code, "providerAndroidApp-" + code));
        xy.getProviderApps().add(new ProviderApp("ios", "Provider-" + code, "providerIosApp-" + code));
        xy.getProviderApps().add(new ProviderApp("web", "Provider-" + code, "providerWebApp-" + code));
        return xy;
    }

    private void assertCountry(final Country country) {
        assertThat(country.getName(), equalTo("name-" + country.getCode()));
        assertThat(country.getEmail(), equalTo("email-" + country.getCode()));
        assertThat(country.getTwitterTags(), equalTo("twitter-" + country.getCode()));
        assertThat(country.getTimetableUrlTemplate(), equalTo("timetable-" + country.getCode()));
        assertThat(country.getOverrideLicense(), equalTo("overrideLicense-" + country.getCode()));
        assertThat(country.getProviderApps().size(), equalTo(3));
        country.getProviderApps().forEach(app -> {
            switch (app.getType()) {
                case "android" -> assertThat(app.getUrl(), equalTo("providerAndroidApp-" + country.getCode()));
                case "ios" -> assertThat(app.getUrl(), equalTo("providerIosApp-" + country.getCode()));
                case "web" -> assertThat(app.getUrl(), equalTo("providerWebApp-" + country.getCode()));
                default -> fail("unknown app type");
            }
        });
    }

}
