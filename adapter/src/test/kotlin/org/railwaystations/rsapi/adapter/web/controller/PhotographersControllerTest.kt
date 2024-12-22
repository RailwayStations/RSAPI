package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.railwaystations.rsapi.adapter.db.CountryAdapter
import org.railwaystations.rsapi.adapter.db.StationAdapter
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.adapter.web.OpenApiValidatorUtil.validOpenApiResponse
import org.railwaystations.rsapi.core.model.CountryTestFixtures
import org.railwaystations.rsapi.core.services.PhotographersService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [PhotographersController::class])
@ContextConfiguration(classes = [WebMvcTestApplication::class, ErrorHandlingControllerAdvice::class, PhotographersService::class])
@AutoConfigureMockMvc(addFilters = false)
internal class PhotographersControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var stationAdapter: StationAdapter

    @MockkBean
    private lateinit var countryAdapter: CountryAdapter

    @BeforeEach
    fun setup() {
        every { countryAdapter.findById("de") } returns CountryTestFixtures.createCountry("de")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/photographers?country=x", "/photographers?country=xyz"])
    fun getPhotographersWithInvalidCountry(urlTemplate: String) {
        mvc.perform(get(urlTemplate))
            .andExpect(validOpenApiResponse())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun getPhotographersOfCountryDe() {
        every { stationAdapter.getPhotographerMap("de") } returns photographersMap

        mvc.perform(get("/photographers?country=de"))
            .andExpect(status().isOk())
            .andExpect(content().string(photographersResponse)) // use string comparison because of ordering
            .andExpect(validOpenApiResponse())
    }

    @Test
    fun getPhotographersOfAllCountries() {
        every { stationAdapter.getPhotographerMap(null) } returns photographersMap

        mvc.perform(get("/photographers"))
            .andExpect(status().isOk())
            .andExpect(content().string(photographersResponse)) // use string comparison because of ordering
            .andExpect(validOpenApiResponse())
    }

}

private val photographersMap = mapOf("@user8" to 29, "@user0" to 9, "@user10" to 15, "@user27" to 31)

private const val photographersResponse = """{"@user27":31,"@user8":29,"@user10":15,"@user0":9}"""
