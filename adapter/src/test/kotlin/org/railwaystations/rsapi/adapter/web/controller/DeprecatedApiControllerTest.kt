package org.railwaystations.rsapi.adapter.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.adapter.web.auth.LazySodiumPasswordEncoder
import org.railwaystations.rsapi.adapter.web.model.CountryDto
import org.railwaystations.rsapi.core.config.MessageSourceConfig
import org.railwaystations.rsapi.core.model.CountryTestFixtures
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.StationTestFixtures.stationDe5WithPhoto
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.railwaystations.rsapi.core.ports.inbound.FindPhotoStationsUseCase
import org.railwaystations.rsapi.core.ports.inbound.ListCountriesUseCase
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase.ProfileConflictException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.function.Consumer

private const val USER_AGENT = "UserAgent"

@WebMvcTest(controllers = [DeprecatedApiController::class], properties = ["mailVerificationUrl=EMAIL_VERIFICATION_URL"])
@Import(
    value = [ErrorHandlingControllerAdvice::class, MockMvcTestConfiguration::class, LazySodiumPasswordEncoder::class, MessageSourceConfig::class, RequestUtil::class]
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("mockMvcTest")
internal class DeprecatedApiControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var findPhotoStationsUseCase: FindPhotoStationsUseCase

    @MockkBean(relaxed = true)
    private lateinit var listCountriesUseCase: ListCountriesUseCase

    @MockkBean(relaxed = true)
    private lateinit var manageProfileUseCase: ManageProfileUseCase

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun getCountryStations() {
        every {
            findPhotoStationsUseCase.findByCountry(
                setOf("de"),
                true,
                stationDe5WithPhoto.photos[0].photographer.displayName,
                stationDe5WithPhoto.active
            )
        } returns setOf(stationDe5WithPhoto)

        val request = mvc.perform(
            get("/de/stations")
                .param("hasPhoto", "true")
                .param("photographer", stationDe5WithPhoto.photos[0].photographer.displayName)
                .param("active", stationDe5WithPhoto.active.toString())
        )

        assertJsonResponse(request, stationDe5WithPhoto, "$.[0]")
    }

    @Test
    fun countryStationId() {
        every {
            findPhotoStationsUseCase.findByCountryAndId(stationDe5WithPhoto.key.country, stationDe5WithPhoto.key.id)
        } returns stationDe5WithPhoto

        val request = mvc.perform(get("/de/stations/5"))

        assertJsonResponse(request, stationDe5WithPhoto, "$")
    }

    @Test
    fun getCountryStationIdNotFound() {
        every { findPhotoStationsUseCase.findByCountryAndId("de", "00") } returns null

        mvc.perform(get("/de/stations/00"))
            .andExpect(status().isNotFound())
    }

    @Test
    fun getStationsByCountryXY() {
        every { findPhotoStationsUseCase.findByCountry(setOf("de"), null, null, null) } returns setOf(
            stationDe5WithPhoto
        )

        val request = mvc.perform(get("/stations?country=de"))

        assertJsonResponse(request, stationDe5WithPhoto, "$.[0]")
    }

    @Test
    fun getStationsByCountryXYWithFilterActive() {
        mvc.perform(get("/stations?country=de&active=true"))
            .andExpect(status().isOk())
            .andExpect(deprecationHeader())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty())

        verify { findPhotoStationsUseCase.findByCountry(setOf("de"), null, null, true) }
    }

    @Test
    fun getAllStationsDefaultsToDE() {
        every { findPhotoStationsUseCase.findByCountry(setOf("de"), null, null, null) } returns setOf(
            stationDe5WithPhoto
        )

        mvc.perform(get("/stations"))
            .andExpect(status().isOk())
            .andExpect(deprecationHeader())
            .andExpect(jsonPath("$.[0]").isNotEmpty())
    }

    @Test
    fun getStationsIsLimitedToThreeCountries() {
        every {
            findPhotoStationsUseCase.findByCountry(setOf("ab", "de", "xy"), null, null, null)
        } returns setOf(stationDe5WithPhoto)

        mvc.perform(get("/stations?country=ab&country=de&country=xy&country=zz"))
            .andExpect(status().isOk())
            .andExpect(deprecationHeader())
            .andExpect(jsonPath("$.[0]").isNotEmpty())
    }

    @Test
    fun getCountriesJson() {
        every { listCountriesUseCase.list(null) } returns CountryTestFixtures.createCountryList(setOf("xy", "ab"))

        val contentAsString = mvc.perform(get("/countries.json"))
            .andExpect(status().isOk())
            .andExpect(deprecationHeader())
            .andReturn().response.contentAsString

        val countries =
            objectMapper.readerForListOf(CountryDto::class.java).readValue<List<CountryDto>>(contentAsString)
        assertThat(countries.size).isEqualTo(2)
        countries.forEach(Consumer { country -> assertCountry(country) })
    }

    @Test
    fun getCountriesJsonOnlyActiveFalse() {
        every { listCountriesUseCase.list(false) } returns CountryTestFixtures.createCountryList(setOf("xy", "ab"))

        val contentAsString = mvc.perform(
            get("/countries.json")
                .param("onlyActive", "false")
        )
            .andExpect(status().isOk())
            .andExpect(deprecationHeader())
            .andReturn().response.contentAsString

        val countries =
            objectMapper.readerForListOf(CountryDto::class.java).readValue<List<CountryDto>>(contentAsString)
        assertThat(countries.size).isEqualTo(2)
        countries.forEach(Consumer { country -> assertCountry(country) })
    }

    @Test
    fun testRegisterInvalidData() {
        val givenUserProfileWithoutEmail = """
                    { "nickname": "nickname", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """.trimIndent()
        postRegistration(givenUserProfileWithoutEmail).andExpect(status().isBadRequest())
    }

    private fun postRegistration(userProfileJson: String): ResultActions {
        return mvc.perform(
            post("/registration")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .contentType("application/json")
                .content(userProfileJson)
                .with(csrf())
        )
    }

    @Test
    fun registerNewUser() {
        val user = UserTestFixtures.userJimKnopf
        val givenUserProfile = """
                    { "nickname": "%s", "email": "%s", "link": "%s", "license": "CC0", "anonymous": %b, "sendNotifications": %b, "photoOwner": %b }
                """.format(
            user.name,
            user.email,
            user.url,
            user.anonymous,
            user.sendNotifications,
            user.ownPhotos
        )
        postRegistration(givenUserProfile)
            .andExpect(deprecationHeader())
            .andExpect(status().isAccepted())

        verify { manageProfileUseCase.register(user, USER_AGENT) }
    }

    @Test
    fun registerNewUserWithPassword() {
        val user = UserTestFixtures.userJimKnopf.copy(
            newPassword = "verySecretPassword"
        )
        val givenUserProfileWithPassword = """
                    { "nickname": "%s", "email": "%s", "link": "%s", "license": "CC0", "anonymous": %b, "sendNotifications": %b, "photoOwner": %b, "newPassword": "%s" }
                """.format(
            user.name,
            user.email,
            user.url,
            user.anonymous,
            user.sendNotifications,
            user.ownPhotos,
            user.newPassword
        )
        postRegistration(givenUserProfileWithPassword)
            .andExpect(deprecationHeader())
            .andExpect(status().isAccepted())

        verify { manageProfileUseCase.register(user, USER_AGENT) }
    }

    @Test
    fun registerUserNameTaken() {
        every { manageProfileUseCase.register(any(), any()) } throws ProfileConflictException()
        val givenUserProfileWithSameName = """
                    { "nickname": "nickname", "email": "other@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """.trimIndent()
        postRegistration(givenUserProfileWithSameName).andExpect(status().isConflict())
    }

    @Test
    fun registerExistingUserEmptyName() {
        every { manageProfileUseCase.register(any(), any()) } throws IllegalArgumentException()
        val givenUserProfileWithEmptyName = """
                    { "nickname": "", "email": "nickname@example.com", "link": "https://link@example.com", "license": "CC0", "anonymous": false, "sendNotifications": true, "photoOwner": true }
                """.trimIndent()
        postRegistration(givenUserProfileWithEmptyName).andExpect(status().isBadRequest())
    }

    private fun assertJsonResponse(request: ResultActions, station: Station, prefix: String) {
        val photo = station.photos[0]
        request
            .andExpect(status().isOk())
            .andExpect(deprecationHeader())
            .andExpect(jsonPath("$prefix.country").value(station.key.country))
            .andExpect(jsonPath("$prefix.idStr").value(station.key.id))
            .andExpect(jsonPath("$prefix.id").value(station.key.id.toInt()))
            .andExpect(jsonPath("$prefix.title").value(station.title))
            .andExpect(jsonPath("$prefix.lat").value(station.coordinates.lat))
            .andExpect(jsonPath("$prefix.lon").value(station.coordinates.lon))
            .andExpect(jsonPath("$prefix.photographer").value(photo.photographer.displayName))
            .andExpect(jsonPath("$prefix.DS100").value(station.ds100))
            .andExpect(jsonPath("$prefix.photoUrl").value("http://localhost:8080/photos${photo.urlPath}"))
            .andExpect(jsonPath("$prefix.license").value(photo.license.displayName))
            .andExpect(jsonPath("$prefix.photographerUrl").value(photo.photographer.displayUrl))
            .andExpect(jsonPath("$prefix.active").value(station.active))
    }

    private fun deprecationHeader(): ResultMatcher {
        return header().string("Deprecation", "@1661983200")
    }

}
