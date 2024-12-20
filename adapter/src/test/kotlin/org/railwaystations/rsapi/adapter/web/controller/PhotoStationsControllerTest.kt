package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.adapter.web.OpenApiValidatorUtil.validOpenApiResponse
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.PhotoTestFixtures
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.StationTestFixtures.createStation
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.railwaystations.rsapi.core.services.PhotoStationsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(controllers = [PhotoStationsController::class])
@ContextConfiguration(classes = [WebMvcTestApplication::class, ErrorHandlingControllerAdvice::class])
@AutoConfigureMockMvc(addFilters = false)
internal class PhotoStationsControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var photoStationsService: PhotoStationsService

    @Test
    fun getPhotoStationsByCountry() {
        val stationXY1 = stationXY1
        val stationXY5 = stationXY5.copy(
            photos = listOf(photoXY5)
        )
        every { photoStationsService.findByCountry(setOf("xy"), null, null) } returns setOf(stationXY1, stationXY5)

        mvc.perform(get("/photoStationsByCountry/xy"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
            .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
            .andExpect(
                jsonPath("$.licenses[0].url")
                    .value("https://creativecommons.org/publicdomain/zero/1.0/")
            )
            .andExpect(jsonPath("$.licenses[1]").doesNotExist())
            .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
            .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
            .andExpect(jsonPath("$.photographers[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[?(@.id == '5')].country").value("xy"))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].title").value("Lummerland"))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].lat").value(50.0))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].lon").value(9.0))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].shortCode").value("XYZ"))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].inactive").doesNotExist())
            .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].id").value(0))
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].photographer")
                    .value("Jim Knopf")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].path").value("/xy/5.jpg")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].license").value("CC0_10")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].createdAt")
                    .value(createdAt.toEpochMilli())
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].outdated").doesNotExist()
            )
            .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[?(@.id == '1')].country").value("xy"))
            .andExpect(jsonPath("$.stations[?(@.id == '1')].title").value("Lummerland Ost"))
            .andExpect(jsonPath("$.stations[?(@.id == '1')].lat").value(50.1))
            .andExpect(jsonPath("$.stations[?(@.id == '1')].lon").value(9.1))
            .andExpect(jsonPath("$.stations[?(@.id == '1')].shortCode").value("XYY"))
            .andExpect(jsonPath("$.stations[?(@.id == '1')].inactive").value(true))
            .andExpect(jsonPath("$.stations[?(@.id == '1')].photos.size()").value(0))
            .andExpect(jsonPath("$.stations[2]").doesNotExist())
    }

    @Test
    fun getPhotoStationsByCountryWithIsActiveFilter() {
        every { photoStationsService.findByCountry(setOf("xy"), null, false) } returns setOf(stationXY1)

        mvc.perform(get("/photoStationsByCountry/xy?isActive=false"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses").isEmpty())
            .andExpect(jsonPath("$.photographers").isEmpty())
            .andExpect(jsonPath("$.stations[0].country").value("xy"))
            .andExpect(jsonPath("$.stations[0].id").value("1"))
            .andExpect(jsonPath("$.stations[0].title").value("Lummerland Ost"))
            .andExpect(jsonPath("$.stations[0].lat").value(50.1))
            .andExpect(jsonPath("$.stations[0].lon").value(9.1))
            .andExpect(jsonPath("$.stations[0].shortCode").value("XYY"))
            .andExpect(jsonPath("$.stations[0].inactive").value(true))
            .andExpect(jsonPath("$.stations[0].photos").isEmpty())
            .andExpect(jsonPath("$.stations[1]").doesNotExist())
    }

    @Test
    fun getPhotoStationsByCountryWithHasPhotoFilter() {
        val stationXY5 = stationXY5.copy(
            photos = listOf(photoXY5)
        )
        every { photoStationsService.findByCountry(setOf("xy"), true, null) } returns setOf(stationXY5)

        mvc.perform(get("/photoStationsByCountry/xy?hasPhoto=true"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
            .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
            .andExpect(
                jsonPath("$.licenses[0].url")
                    .value("https://creativecommons.org/publicdomain/zero/1.0/")
            )
            .andExpect(jsonPath("$.licenses[1]").doesNotExist())
            .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
            .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
            .andExpect(jsonPath("$.photographers[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[0].country").value("xy"))
            .andExpect(jsonPath("$.stations[0].id").value("5"))
            .andExpect(jsonPath("$.stations[0].title").value("Lummerland"))
            .andExpect(jsonPath("$.stations[0].lat").value(50))
            .andExpect(jsonPath("$.stations[0].lon").value(9))
            .andExpect(jsonPath("$.stations[0].shortCode").value("XYZ"))
            .andExpect(jsonPath("$.stations[0].inactive").doesNotExist())
            .andExpect(jsonPath("$.stations[0].photos[0].id").value(0L))
            .andExpect(jsonPath("$.stations[0].photos[0].photographer").value("Jim Knopf"))
            .andExpect(jsonPath("$.stations[0].photos[0].path").value("/xy/5.jpg"))
            .andExpect(jsonPath("$.stations[0].photos[0].license").value("CC0_10"))
            .andExpect(
                jsonPath("$.stations[0].photos[0].createdAt").value(createdAt.toEpochMilli())
            )
            .andExpect(jsonPath("$.stations[0].photos[0].outdated").doesNotExist())
            .andExpect(jsonPath("$.stations[0].photos[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[1]").doesNotExist())
    }

    @Test
    fun getPhotoStationsByCountryWithUnknownCountry() {
        every { photoStationsService.findByCountry(any(), any(), any()) } returns setOf()

        mvc.perform(get("/photoStationsByCountry/00"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses").isEmpty())
            .andExpect(jsonPath("$.photographers").isEmpty())
            .andExpect(jsonPath("$.stations").isEmpty())
    }

    @Test
    fun getPhotoStationByIdOfInactiveStationWithoutPhotos() {
        every { photoStationsService.findByKey(stationXY1.key) } returns stationXY1

        mvc.perform(get("/photoStationById/xy/1"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses").isEmpty())
            .andExpect(jsonPath("$.photographers").isEmpty())
            .andExpect(jsonPath("$.stations[0].country").value("xy"))
            .andExpect(jsonPath("$.stations[0].id").value("1"))
            .andExpect(jsonPath("$.stations[0].title").value("Lummerland Ost"))
            .andExpect(jsonPath("$.stations[0].lat").value(50.1))
            .andExpect(jsonPath("$.stations[0].lon").value(9.1))
            .andExpect(jsonPath("$.stations[0].shortCode").value("XYY"))
            .andExpect(jsonPath("$.stations[0].inactive").value(true))
            .andExpect(jsonPath("$.stations[0].photos").isEmpty())
            .andExpect(jsonPath("$.stations[1]").doesNotExist())
    }

    @Test
    fun getPhotoStationByIdOfActiveStationWithTwoPhotos() {
        val stationAB3 = stationAB3.copy(
            photos = listOf(photoAB3_1, photoAB3_2)
        )
        every { photoStationsService.findByKey(stationAB3.key) } returns stationAB3

        mvc.perform(get("/photoStationById/ab/3"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses[0].id").value("CC_BY_NC_40_INT"))
            .andExpect(jsonPath("$.licenses[0].name").value("CC BY-NC 4.0 International"))
            .andExpect(
                jsonPath("$.licenses[0].url")
                    .value("https://creativecommons.org/licenses/by-nc/4.0/")
            )
            .andExpect(jsonPath("$.licenses[1].id").value("CC0_10"))
            .andExpect(jsonPath("$.licenses[1].name").value("CC0 1.0 Universell (CC0 1.0)"))
            .andExpect(
                jsonPath("$.licenses[1].url")
                    .value("https://creativecommons.org/publicdomain/zero/1.0/")
            )
            .andExpect(jsonPath("$.photographers[0].name").value("Peter Pan"))
            .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlPeter"))
            .andExpect(jsonPath("$.photographers[1].name").value("Jim Knopf"))
            .andExpect(jsonPath("$.photographers[1].url").value("photographerUrlJim"))
            .andExpect(jsonPath("$.stations[0].country").value("ab"))
            .andExpect(jsonPath("$.stations[0].id").value("3"))
            .andExpect(jsonPath("$.stations[0].title").value("Nimmerland"))
            .andExpect(jsonPath("$.stations[0].lat").value(40.0))
            .andExpect(jsonPath("$.stations[0].lon").value(6.0))
            .andExpect(jsonPath("$.stations[0].shortCode").value("ABC"))
            .andExpect(jsonPath("$.stations[0].inactive").doesNotExist())
            .andExpect(jsonPath("$.stations[0].photos[0].id").value(2L))
            .andExpect(jsonPath("$.stations[0].photos[0].photographer").value("Jim Knopf"))
            .andExpect(jsonPath("$.stations[0].photos[0].path").value("/ab/3_2.jpg"))
            .andExpect(jsonPath("$.stations[0].photos[0].license").value("CC0_10"))
            .andExpect(
                jsonPath("$.stations[0].photos[0].createdAt").value(createdAt.toEpochMilli())
            )
            .andExpect(jsonPath("$.stations[0].photos[0].outdated").value(true))
            .andExpect(jsonPath("$.stations[0].photos[1].id").value(1L))
            .andExpect(jsonPath("$.stations[0].photos[1].photographer").value("Peter Pan"))
            .andExpect(jsonPath("$.stations[0].photos[1].path").value("/ab/3_1.jpg"))
            .andExpect(jsonPath("$.stations[0].photos[1].license").value("CC_BY_NC_40_INT"))
            .andExpect(
                jsonPath("$.stations[0].photos[1].createdAt").value(createdAt.toEpochMilli())
            )
            .andExpect(jsonPath("$.stations[0].photos[1].outdated").doesNotExist())
            .andExpect(jsonPath("$.stations[0].photos[2]").doesNotExist())
            .andExpect(jsonPath("$.stations[1]").doesNotExist())
    }

    @Test
    fun getPhotoStationByIdWithNonExistingId() {
        every { photoStationsService.findByKey(any()) } returns null
        mvc.perform(get("/photoStationById/ab/not_existing_id"))
            .andExpect(status().isNotFound())
            .andExpect(validOpenApiResponse())
    }

    @Test
    fun getPhotoStationsByPhotographerWithCountryFilter() {
        val stationXY5 = stationXY5.copy(
            photos = listOf(photoXY5)
        )
        every { photoStationsService.findByPhotographer("Jim Knopf", "xy") } returns setOf(stationXY5)

        mvc.perform(get("/photoStationsByPhotographer/Jim Knopf?country=xy"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
            .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
            .andExpect(
                jsonPath("$.licenses[0].url")
                    .value("https://creativecommons.org/publicdomain/zero/1.0/")
            )
            .andExpect(jsonPath("$.licenses[1]").doesNotExist())
            .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
            .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
            .andExpect(jsonPath("$.photographers[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[0].country").value("xy"))
            .andExpect(jsonPath("$.stations[0].id").value("5"))
            .andExpect(jsonPath("$.stations[0].title").value("Lummerland"))
            .andExpect(jsonPath("$.stations[0].lat").value(50))
            .andExpect(jsonPath("$.stations[0].lon").value(9))
            .andExpect(jsonPath("$.stations[0].shortCode").value("XYZ"))
            .andExpect(jsonPath("$.stations[0].inactive").doesNotExist())
            .andExpect(jsonPath("$.stations[0].photos[0].id").value(0L))
            .andExpect(jsonPath("$.stations[0].photos[0].photographer").value("Jim Knopf"))
            .andExpect(jsonPath("$.stations[0].photos[0].path").value("/xy/5.jpg"))
            .andExpect(jsonPath("$.stations[0].photos[0].license").value("CC0_10"))
            .andExpect(
                jsonPath("$.stations[0].photos[0].createdAt").value(createdAt.toEpochMilli())
            )
            .andExpect(jsonPath("$.stations[0].photos[0].outdated").doesNotExist())
            .andExpect(jsonPath("$.stations[0].photos[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[1]").doesNotExist())
    }

    @Test
    fun getPhotoStationsByPhotographer() {
        val stationAB3 = stationAB3.copy(
            photos = listOf(photoAB3_2)
        )
        val stationXY5 = stationXY5.copy(
            photos = listOf(photoXY5)
        )
        every { photoStationsService.findByPhotographer("Jim Knopf", null) } returns setOf(stationAB3, stationXY5)

        mvc.perform(get("/photoStationsByPhotographer/Jim Knopf"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses[0].id").value("CC0_10"))
            .andExpect(jsonPath("$.licenses[0].name").value("CC0 1.0 Universell (CC0 1.0)"))
            .andExpect(
                jsonPath("$.licenses[0].url")
                    .value("https://creativecommons.org/publicdomain/zero/1.0/")
            )
            .andExpect(jsonPath("$.licenses[1]").doesNotExist())
            .andExpect(jsonPath("$.photographers[0].name").value("Jim Knopf"))
            .andExpect(jsonPath("$.photographers[0].url").value("photographerUrlJim"))
            .andExpect(jsonPath("$.photographers[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[?(@.id == '5')].country").value("xy"))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].id").value("5"))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].title").value("Lummerland"))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].lat").value(50.0))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].lon").value(9.0))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].shortCode").value("XYZ"))
            .andExpect(jsonPath("$.stations[?(@.id == '5')].inactive").doesNotExist())
            .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[0].id").value(0))
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].photographer")
                    .value("Jim Knopf")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].path").value("/xy/5.jpg")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].license").value("CC0_10")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].createdAt")
                    .value(createdAt.toEpochMilli())
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '5')].photos[0].outdated").doesNotExist()
            )
            .andExpect(jsonPath("$.stations[?(@.id == '5')].photos[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[?(@.id == '3')].country").value("ab"))
            .andExpect(jsonPath("$.stations[?(@.id == '3')].id").value("3"))
            .andExpect(jsonPath("$.stations[?(@.id == '3')].title").value("Nimmerland"))
            .andExpect(jsonPath("$.stations[?(@.id == '3')].lat").value(40.0))
            .andExpect(jsonPath("$.stations[?(@.id == '3')].lon").value(6.0))
            .andExpect(jsonPath("$.stations[?(@.id == '3')].shortCode").value("ABC"))
            .andExpect(jsonPath("$.stations[?(@.id == '3')].inactive").doesNotExist())
            .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].id").value(2))
            .andExpect(
                jsonPath("$.stations[?(@.id == '3')].photos[0].photographer")
                    .value("Jim Knopf")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '3')].photos[0].path").value("/ab/3_2.jpg")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '3')].photos[0].license").value("CC0_10")
            )
            .andExpect(
                jsonPath("$.stations[?(@.id == '3')].photos[0].createdAt")
                    .value(createdAt.toEpochMilli())
            )
            .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[0].outdated").value(true))
            .andExpect(jsonPath("$.stations[?(@.id == '3')].photos[1]").doesNotExist())
            .andExpect(jsonPath("$.stations[2]").doesNotExist())
    }

    @Test
    fun getPhotoStationsByRecentPhotoImportsWithDefaultSinceHours() {
        val stationXY5 = stationXY5.copy(
            photos = listOf(photoXY5)
        )
        every { photoStationsService.findRecentImports(10) } returns setOf(stationXY5)

        mvc.perform(get("/photoStationsByRecentPhotoImports"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses.size()").value(1))
            .andExpect(jsonPath("$.photographers.size()").value(1))
            .andExpect(jsonPath("$.stations[0].country").value("xy"))
            .andExpect(jsonPath("$.stations[0].id").value("5"))
            .andExpect(jsonPath("$.stations[0].photos[0].id").value(0L))
    }

    @Test
    fun getPhotoStationsByRecentPhotoImportsWithSinceHours() {
        val stationAB3 = stationAB3.copy(
            photos = listOf(photoAB3_1)
        )
        every { photoStationsService.findRecentImports(100) } returns setOf(stationAB3)

        mvc.perform(get("/photoStationsByRecentPhotoImports?sinceHours=100"))
            .andExpect(status().isOk())
            .andExpect(validOpenApiResponse())
            .andExpect(jsonPath("$.photoBaseUrl").value("http://localhost:8080/photos"))
            .andExpect(jsonPath("$.licenses.size()").value(1))
            .andExpect(jsonPath("$.photographers.size()").value(1))
            .andExpect(jsonPath("$.stations[0].country").value("ab"))
            .andExpect(jsonPath("$.stations[0].id").value("3"))
            .andExpect(jsonPath("$.stations[0].photos[0].id").value(1L))
    }


}

private val keyXY1: Station.Key = Station.Key("xy", "1")
private val keyXY5: Station.Key = Station.Key("xy", "5")
private val photographerJimKnopf: User = UserTestFixtures.someUser.copy(
    name = "Jim Knopf",
    url = "photographerUrlJim",
    license = License.CC0_10,
)
private val keyAB3: Station.Key = Station.Key("ab", "3")
private val createdAt: Instant = Instant.now()
private val photographerPeterPan: User =
    UserTestFixtures.someUser.copy(
        name = "Peter Pan",
        url = "photographerUrlPeter",
        license = License.CC_BY_NC_SA_30_DE,
    )

private val photoAB3_2 = PhotoTestFixtures.createPhoto(keyAB3, photographerJimKnopf).copy(
    id = 2L,
    primary = true,
    createdAt = createdAt,
    license = License.CC0_10,
    outdated = true,
    urlPath = "/${keyAB3.country}/${keyAB3.id}_2.jpg",
)

private val photoAB3_1 = PhotoTestFixtures.createPhoto(keyAB3, photographerPeterPan).copy(
    id = 1L,
    createdAt = createdAt,
    license = License.CC_BY_NC_40_INT,
    urlPath = "/${keyAB3.country}/${keyAB3.id}_1.jpg",
)

private val stationAB3 = createStation(keyAB3, Coordinates(40.0, 6.0), null).copy(
    title = "Nimmerland",
    ds100 = "ABC",
)

private val photoXY5 = PhotoTestFixtures.createPhoto(keyXY5, photographerJimKnopf).copy(
    createdAt = createdAt,
)

private val stationXY5 = createStation(keyXY5, Coordinates(50.0, 9.0), null).copy(
    title = "Lummerland",
    ds100 = "XYZ",
)

private val stationXY1 = createStation(keyXY1, Coordinates(50.1, 9.1), null).copy(
    title = "Lummerland Ost",
    ds100 = "XYY",
    active = false,
)
