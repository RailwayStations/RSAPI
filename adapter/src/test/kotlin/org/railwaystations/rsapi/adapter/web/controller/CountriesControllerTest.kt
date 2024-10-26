package org.railwaystations.rsapi.adapter.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.CountryDao
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.adapter.web.OpenApiValidatorUtil.validOpenApiResponse
import org.railwaystations.rsapi.adapter.web.model.CountryDto
import org.railwaystations.rsapi.adapter.web.model.ProviderAppDto
import org.railwaystations.rsapi.core.model.CountryTestFixtures.createCountryList
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.services.CountryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.function.Consumer

@WebMvcTest(controllers = [CountriesController::class])
@ContextConfiguration(classes = [WebMvcTestApplication::class, ErrorHandlingControllerAdvice::class, CountryService::class])
@AutoConfigureMockMvc(addFilters = false)
internal class CountriesControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var countryDao: CountryDao

    @Test
    fun listCountries() {
        every { countryDao.list(true) } returns createCountryList(setOf("xy", "ab"))

        val contentAsString = mvc.perform(get("/countries"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andReturn().response.contentAsString

        val countries =
            objectMapper.readerForListOf(CountryDto::class.java).readValue<List<CountryDto>>(contentAsString)
        assertThat(countries.size).isEqualTo(2)
        countries.forEach(Consumer { country -> assertCountry(country) })
    }

}

fun assertCountry(country: CountryDto) {
    assertThat(country.name).isEqualTo("name-" + country.code)
    assertThat(country.email).isEqualTo("email-" + country.code)
    assertThat(country.timetableUrlTemplate).isEqualTo("timetable-" + country.code)
    assertThat(country.overrideLicense).isEqualTo(License.CC_BY_NC_40_INT.displayName)
    assertThat(country.providerApps!!.size).isEqualTo(3)
    country.providerApps!!.forEach(Consumer { app ->
        when (app.type) {
            ProviderAppDto.Type.ANDROID -> assertThat(app.url).isEqualTo("androidApp-" + country.code)
            ProviderAppDto.Type.IOS -> assertThat(app.url).isEqualTo("iosApp-" + country.code)
            ProviderAppDto.Type.WEB -> assertThat(app.url).isEqualTo("webApp-" + country.code)
            else -> Fail.fail<Any>("unknown app type")
        }
    })
}
