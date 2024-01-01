package org.railwaystations.rsapi.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.adapter.in.web.ErrorHandlingControllerAdvice;
import org.railwaystations.rsapi.adapter.in.web.model.CountryDto;
import org.railwaystations.rsapi.app.auth.LazySodiumPasswordEncoder;
import org.railwaystations.rsapi.app.config.MessageSourceConfig;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.railwaystations.rsapi.core.services.CountryService;
import org.railwaystations.rsapi.core.services.PhotoStationsService;
import org.railwaystations.rsapi.core.services.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeprecatedApiController.class, properties = {"mailVerificationUrl=EMAIL_VERIFICATION_URL"})
@ContextConfiguration(classes = {
        WebMvcTestApplication.class,
        ErrorHandlingControllerAdvice.class,
        ProfileService.class,
        MockMvcTestConfiguration.class,
        LazySodiumPasswordEncoder.class,
        MessageSourceConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("mockMvcTest")
class DeprecatedApiControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PhotoStationsService photoStationsService;

    @MockBean
    private CountryService countryService;

    @MockBean
    private ProfileService profileService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Station stationDE5 = createStationDE5();

    @NotNull
    private Station createStationDE5() {
        var keyDE5 = new Station.Key("de", "5");
        var stationDE5 = Station.builder()
                .key(keyDE5)
                .title("Lummerland")
                .coordinates(new Coordinates(50.0, 9.0))
                .ds100("XYZ")
                .active(false)
                .build();
        stationDE5.getPhotos().add(Photo.builder()
                .stationKey(keyDE5)
                .urlPath("/de/5.jpg")
                .photographer(User.builder()
                        .id(0)
                        .name("Jim Knopf")
                        .license(License.CC0_10)
                        .ownPhotos(true)
                        .anonymous(false)
                        .url("photographerUrl")
                        .build())
                .license(License.CC0_10)
                .primary(true)
                .build());
        return stationDE5;
    }


    @Test
    void getCountryStations() throws Exception {
        when(photoStationsService.findByCountry(Collections.singleton("de"), true, stationDE5.getPhotos().getFirst().getPhotographer().getDisplayName(), stationDE5.isActive())).thenReturn(Set.of(stationDE5));

        var request = mvc.perform(get("/de/stations")
                .param("hasPhoto", "true")
                .param("photographer", stationDE5.getPhotos().getFirst().getPhotographer().getDisplayName())
                .param("active", Boolean.toString(stationDE5.isActive())));

        assertJsonResponse(request, stationDE5, "$.[0]");
    }

    private static void assertJsonResponse(ResultActions request, Station station, String prefix) throws Exception {
        var photo = station.getPhotos().getFirst();
        request
                .andExpect(status().isOk())
                .andExpect(deprecationHeader())
                .andExpect(jsonPath(prefix + ".country").value(station.getKey().getCountry()))
                .andExpect(jsonPath(prefix + ".idStr").value(station.getKey().getId()))
                .andExpect(jsonPath(prefix + ".id").value(Integer.parseInt(station.getKey().getId())))
                .andExpect(jsonPath(prefix + ".title").value(station.getTitle()))
                .andExpect(jsonPath(prefix + ".lat").value(station.getCoordinates().getLat()))
                .andExpect(jsonPath(prefix + ".lon").value(station.getCoordinates().getLon()))
                .andExpect(jsonPath(prefix + ".photographer").value(photo.getPhotographer().getDisplayName()))
                .andExpect(jsonPath(prefix + ".DS100").value(station.getDs100()))
                .andExpect(jsonPath(prefix + ".photoUrl").value("http://localhost:8080/photos" + photo.getUrlPath()))
                .andExpect(jsonPath(prefix + ".license").value(photo.getLicense().getDisplayName()))
                .andExpect(jsonPath(prefix + ".photographerUrl").value(photo.getPhotographer().getDisplayUrl()))
                .andExpect(jsonPath(prefix + ".active").value(station.isActive()));
    }

    @Test
    void getCountryStationsWithInvalidCountry() throws Exception {
        mvc.perform(get("/d/stations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCountryStationId() throws Exception {
        when(photoStationsService.findByCountryAndId(stationDE5.getKey().getCountry(), stationDE5.getKey().getId())).thenReturn(Optional.of(stationDE5));

        var request = mvc.perform(get("/de/stations/5"));

        assertJsonResponse(request, stationDE5, "$");
    }

    @Test
    void getCountryStationIdNotFound() throws Exception {
        when(photoStationsService.findByCountryAndId("de", "00")).thenReturn(Optional.empty());

        mvc.perform(get("/de/stations/00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCountryStationIdWithInvalidCountry() throws Exception {
        mvc.perform(get("/a/stations/3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStationsByCountryXY() throws Exception {
        when(photoStationsService.findByCountry(Collections.singleton("de"), null, null, null)).thenReturn(Set.of(stationDE5));

        var request = mvc.perform(get("/stations?country=de"));

        assertJsonResponse(request, stationDE5, "$.[0]");
    }

    @Test
    void getStationsByCountryXYWithFilterActive() throws Exception {
        mvc.perform(get("/stations?country=de&active=true"))
                .andExpect(status().isOk())
                .andExpect(deprecationHeader())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(photoStationsService).findByCountry(Set.of("de"), null, null, true);
    }

    @Test
    void getAllStationsDefaultsToDE() throws Exception {
        when(photoStationsService.findByCountry(Collections.singleton("de"), null, null, null)).thenReturn(Set.of(stationDE5));

        mvc.perform(get("/stations"))
                .andExpect(status().isOk())
                .andExpect(deprecationHeader())
                .andExpect(jsonPath("$.[0]").isNotEmpty());
    }

    @Test
    void getStationsIsLimitedToThreeCountries() throws Exception {
        when(photoStationsService.findByCountry(Set.of("ab", "de", "xy"), null, null, null)).thenReturn(Set.of(stationDE5));

        mvc.perform(get("/stations?country=ab&country=de&country=xy&country=zz"))
                .andExpect(status().isOk())
                .andExpect(deprecationHeader())
                .andExpect(jsonPath("$.[0]").isNotEmpty());
    }

    @Test
    void getCountriesJson() throws Exception {
        when(countryService.list(null)).thenReturn(CountriesControllerTest.createCountryList());

        var contentAsString = mvc.perform(get("/countries.json"))
                .andExpect(status().isOk())
                .andExpect(deprecationHeader())
                .andReturn().getResponse().getContentAsString();

        List<CountryDto> countries = objectMapper.readerForListOf(CountryDto.class).readValue(contentAsString);
        assertThat(countries.size()).isEqualTo(2);
        countries.forEach(CountriesControllerTest::assertCountry);
    }

    @Test
    void getCountriesJsonOnlyActiveFalse() throws Exception {
        when(countryService.list(false)).thenReturn(CountriesControllerTest.createCountryList());

        var contentAsString = mvc.perform(get("/countries.json")
                        .param("onlyActive", "false"))
                .andExpect(status().isOk())
                .andExpect(deprecationHeader())
                .andReturn().getResponse().getContentAsString();

        List<CountryDto> countries = objectMapper.readerForListOf(CountryDto.class).readValue(contentAsString);
        assertThat(countries.size()).isEqualTo(2);
        countries.forEach(CountriesControllerTest::assertCountry);
    }

    @Test
    void testRegisterInvalidData() throws Exception {
        var givenUserProfileWithoutEmail = """
                    { "nickname": "nickname", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithoutEmail).andExpect(status().isBadRequest());
    }

    @NotNull
    private ResultActions postRegistration(String userProfileJson) throws Exception {
        return mvc.perform(post("/registration")
                        .header("User-Agent", "UserAgent")
                        .contentType("application/json")
                        .content(userProfileJson)
                        .with(csrf()))
                .andExpect(validOpenApiResponse());
    }

    @NotNull
    private static ResultMatcher deprecationHeader() {
        return header().string("Deprecation", "@1661983200");
    }

    @Test
    void registerNewUser() throws Exception {
        var givenUserProfile = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfile)
                .andExpect(deprecationHeader())
                .andExpect(status().isAccepted());

        verify(profileService).register(User.builder()
                .name("nickname")
                .email("nickname@example.com")
                .url("https://link@example.com")
                .license(License.CC0_10)
                .ownPhotos(true)
                .build(), "UserAgent");
    }

    @Test
    void registerNewUserWithPassword() throws Exception {
        var givenUserProfileWithPassword = """
                    { "nickname": "nickname", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true, "newPassword": "verySecretPassword" }
                """;
        postRegistration(givenUserProfileWithPassword)
                .andExpect(deprecationHeader())
                .andExpect(status().isAccepted());

        verify(profileService).register(User.builder()
                .name("nickname")
                .email("nickname@example.com")
                .url("https://link@example.com")
                .license(License.CC0_10)
                .ownPhotos(true)
                .newPassword("verySecretPassword")
                .build(), "UserAgent");
    }

    @Test
    void registerUserNameTaken() throws Exception {
        doThrow(new ManageProfileUseCase.ProfileConflictException()).when(profileService).register(any(User.class), anyString());
        var givenUserProfileWithSameName = """
                    { "nickname": "nickname", "email": "other@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithSameName).andExpect(status().isConflict());
    }

    @Test
    void registerExistingUserEmptyName() throws Exception {
        doThrow(new IllegalArgumentException()).when(profileService).register(any(User.class), anyString());
        var givenUserProfileWithEmptyName = """
                    { "nickname": "", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """;
        postRegistration(givenUserProfileWithEmptyName).andExpect(status().isBadRequest());
    }


}
