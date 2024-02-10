package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.CountryDao
import org.railwaystations.rsapi.adapter.db.StationDao
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.core.model.CountryTestFixtures
import org.railwaystations.rsapi.core.model.Statistic
import org.railwaystations.rsapi.core.services.StatisticService
import org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [StatisticController::class])
@ContextConfiguration(classes = [WebMvcTestApplication::class, ErrorHandlingControllerAdvice::class, StatisticService::class])
@AutoConfigureMockMvc(addFilters = false)
internal class StatisticControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var stationDao: StationDao

    @MockkBean
    private lateinit var countryDao: CountryDao

    @BeforeEach
    fun setup() {
        every { countryDao.findById("de") } returns CountryTestFixtures.createCountry("de")
    }

    @Test
    fun whenCountryIsInvalidThenReturnsStatus400() {
        mvc.perform(get("/stats?country=x"))
            .andExpect(validOpenApiResponse())
            .andExpect(status().isBadRequest())
    }

    @Test
    fun getOverallStatistic() {
        every { stationDao.getStatistic(null) } returns Statistic(null, 954, 91, 6)

        mvc.perform(get("/stats"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.total").value(954))
            .andExpect(jsonPath("$.withPhoto").value(91))
            .andExpect(jsonPath("$.withoutPhoto").value(863))
            .andExpect(jsonPath("$.photographers").value(6))
            .andExpect(jsonPath("$.countryCode").doesNotExist())
    }

    @Test
    fun getStatisticForCountryDe() {
        every { stationDao.getStatistic("de") } returns Statistic("de", 729, 84, 4)

        mvc.perform(get("/stats?country=de"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.total").value(729))
            .andExpect(jsonPath("$.withPhoto").value(84))
            .andExpect(jsonPath("$.withoutPhoto").value(645))
            .andExpect(jsonPath("$.photographers").value(4))
            .andExpect(jsonPath("$.countryCode").value("de"))
    }
}