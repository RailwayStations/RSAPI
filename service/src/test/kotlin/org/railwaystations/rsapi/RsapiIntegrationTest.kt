package org.railwaystations.rsapi

import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.Filter
import net.javacrumbs.jsonunit.assertj.assertThatJson
import net.javacrumbs.jsonunit.assertj.whenever
import net.javacrumbs.jsonunit.core.Option
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.railwaystations.rsapi.adapter.db.AbstractMariaDBBaseTest
import org.railwaystations.rsapi.adapter.db.PhotoDao
import org.railwaystations.rsapi.adapter.photostorage.WorkDir
import org.railwaystations.rsapi.adapter.web.controller.DeprecatedApiController.StationDto
import org.railwaystations.rsapi.adapter.web.model.PhotoDto
import org.railwaystations.rsapi.adapter.web.model.PhotoLicenseDto
import org.railwaystations.rsapi.adapter.web.model.PhotoStationDto
import org.railwaystations.rsapi.adapter.web.model.PhotoStationsDto
import org.railwaystations.rsapi.adapter.web.model.PhotographerDto
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.core.io.support.EncodedResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.imageio.ImageIO

private val IMAGE: ByteArray = Base64.getDecoder()
    .decode("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=")

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["server.error.include-message=always"]
)
@ActiveProfiles("test")
@Sql(
    scripts = [
        "/testdata/cleanup.sql",
        "/testdata/countries.sql",
        "/testdata/providerApps.sql",
        "/testdata/users.sql",
        "/testdata/stations.sql",
        "/testdata/photos.sql",
        "/testdata/oauth2_registered_client.sql",
    ], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
internal class RsapiIntegrationTest : AbstractMariaDBBaseTest() {
    @Autowired
    private lateinit var mapper: ObjectMapper

    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var workDir: WorkDir

    @Autowired
    private lateinit var photoDao: PhotoDao

    @Test
    fun stationsAllCountriesIsDefaultingToDE() {
        val stations = assertLoadStationsOk("/stations")
        assertThat(stations.size).isEqualTo(730)
        assertThat(findByKey(stations, Station.Key("de", "6721"))).isNotNull()
        assertThat(findByKey(stations, Station.Key("ch", "8500126"))).isNull()
    }

    @Test
    fun stationById() {
        val station = loadStationDe6932()
        assertThat(station!!.idStr).isEqualTo("6932")
        assertThat(station.title).isEqualTo("Wuppertal-Ronsdorf")
        assertThat(station.photoUrl).isEqualTo("https://api.railway-stations.org/photos/de/6932.jpg")
        assertThat(station.photographer).isEqualTo("@user10")
        assertThat(station.license).isEqualTo("CC0 1.0 Universell (CC0 1.0)")
        assertThat(station.active).isTrue()
        assertThat(station.outdated).isFalse()
    }

    @Test
    fun photoStationById() {
        val response = restTemplate.getForEntity<PhotoStationsDto>(
            "http://localhost:$port/photoStationById/de/6932"
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        val photoStationsDto = response.body
        assertThat(photoStationsDto).isNotNull()
        assertThat(photoStationsDto!!.photoBaseUrl).isEqualTo("https://api.railway-stations.org/photos")

        val licenses = photoStationsDto.licenses
        assertThat(licenses).containsExactly(
            PhotoLicenseDto(
                "CC0_10",
                "CC0 1.0 Universell (CC0 1.0)",
                URI.create("https://creativecommons.org/publicdomain/zero/1.0/")
            )
        )

        val photographers = photoStationsDto.photographers
        assertThat(photographers)
            .containsExactly(PhotographerDto("@user10", URI.create("https://www.example.com/user10")))

        val station = photoStationsDto.stations[0]
        assertThat(station.country).isEqualTo("de")
        assertThat(station.id).isEqualTo("6932")
        assertThat(station.title).isEqualTo("Wuppertal-Ronsdorf")
        assertThat(station.shortCode).isEqualTo("KWRO")
        assertThat(station.inactive).isFalse()

        val photo1 = station.photos[0]
        assertThat(photo1.id).isEqualTo(24)
        assertThat(photo1.path).isEqualTo("/de/6932.jpg")
        assertThat(photo1.photographer).isEqualTo("@user10")
        assertThat(photo1.license).isEqualTo("CC0_10")
        assertThat(photo1.outdated).isFalse()

        // assertThat(photo1.getCreatedAt()).isEqualTo(1523037167000L); does fail on GitHub
        val photo2 = station.photos[1]
        assertThat(photo2.id).isEqualTo(128)
        assertThat(photo2.path).isEqualTo("/de/6932_2.jpg")
        assertThat(photo2.photographer).isEqualTo("@user10")
        assertThat(photo2.license).isEqualTo("CC0_10")
        assertThat(photo2.outdated).isTrue()
        // assertThat(photo2.getCreatedAt()).isEqualTo(1659357923000L); does fail on GitHub
    }

    @Test
    fun photoStationsByCountryDe() {
        val response = restTemplate.getForEntity<PhotoStationsDto>(
            "http://localhost:$port/photoStationsByCountry/de"
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val photoStationsDto = response.body
        assertThat(photoStationsDto).isNotNull()
        assertThat(photoStationsDto!!.stations).hasSize(730)
        assertThat(photoStationsDto.stations)
            .anyMatch { photoStationDto: PhotoStationDto -> photoStationDto.country == "de" && photoStationDto.id == "6721" }
        assertThat(photoStationsDto.stations)
            .noneMatch { photoStationDto: PhotoStationDto -> photoStationDto.country != "de" }

        // check if we can find the license and photographer of one stationphoto
        val stationWithPhoto =
            photoStationsDto.stations.firstOrNull { photoStationDto -> !photoStationDto.photos.isEmpty() }
        assertThat(stationWithPhoto).isNotNull
        assertThat(photoStationsDto.licenses)
            .anyMatch { photoLicenseDto -> photoLicenseDto.id == stationWithPhoto!!.photos[0].license }
        assertThat(photoStationsDto.photographers)
            .anyMatch { photographerDto -> photographerDto.name == stationWithPhoto!!.photos[0].photographer }
    }

    @ParameterizedTest
    @CsvSource(
        value = ["true,  729", "false, 1"
        ]
    )
    fun photoStationsByCountryDeActive(active: Boolean, numberOfStations: Int) {
        val response = restTemplate.getForEntity<PhotoStationsDto>(
            "http://localhost:$port/photoStationsByCountry/de?isActive=$active"
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val photoStationsDto = response.body
        assertThat(photoStationsDto).isNotNull()
        assertThat(photoStationsDto!!.stations).hasSize(numberOfStations)
        assertThat(photoStationsDto.stations)
            .allMatch { photoStationDto: PhotoStationDto -> photoStationDto.country == "de" && photoStationDto.inactive != active }
    }

    @Test
    fun photoStationsByCountry_with_unknown_country() {
        val response = restTemplate.getForEntity<PhotoStationsDto>(
            "http://localhost:$port/photoStationsByCountry/00"
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val photoStationsDto = response.body
        assertThat(photoStationsDto).isNotNull()
        assertThat(photoStationsDto!!.licenses).isEmpty()
        assertThat(photoStationsDto.photographers).isEmpty()
        assertThat(photoStationsDto.stations).isEmpty()
    }

    @Test
    fun photoStationsByPhotographerAndCountry() {
        val response = restTemplate.getForEntity<PhotoStationsDto>(
            "http://localhost:$port/photoStationsByPhotographer/@user10?country=de"
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val photoStationsDto = response.body
        assertThat(photoStationsDto).isNotNull()

        val licenses = photoStationsDto!!.licenses
        assertThat(licenses).containsExactlyInAnyOrder(
            PhotoLicenseDto(
                id = "CC0_10",
                name = "CC0 1.0 Universell (CC0 1.0)",
                url = URI.create("https://creativecommons.org/publicdomain/zero/1.0/")
            ),
            PhotoLicenseDto(
                id = "CC_BY_SA_40",
                name = "CC BY-SA 4.0",
                url = URI.create("https://creativecommons.org/licenses/by-sa/4.0/")
            )
        )

        val photographers = photoStationsDto.photographers
        assertThat(photographers)
            .containsExactly(PhotographerDto("@user10", URI.create("https://www.example.com/user10")))
        assertThat(photographers)
            .noneMatch { photographerDto: PhotographerDto -> photographerDto.name != "@user10" }
        assertThat(photoStationsDto.stations)
            .noneMatch { stationDto: PhotoStationDto -> stationDto.country != "de" }
        assertThat(
            photoStationsDto.stations.flatMap { photoStationDto -> photoStationDto.photos })
            .noneMatch { photoDto: PhotoDto -> photoDto.photographer != "@user10" }

        val station = photoStationsDto.stations
            .first { photoStationDto: PhotoStationDto -> photoStationDto.country == "de" && photoStationDto.id == "6932" }
        assertThat(station.country).isEqualTo("de")
        assertThat(station.id).isEqualTo("6932")

        val photo1 = station.photos[0]
        assertThat(photo1.id).isEqualTo(24L)
        assertThat(photo1.path).isEqualTo("/de/6932.jpg")
        assertThat(photo1.photographer).isEqualTo("@user10")
        assertThat(photo1.license).isEqualTo("CC0_10")
        assertThat(photo1.outdated).isFalse()

        val photo2 = station.photos[1]
        assertThat(photo2.id).isEqualTo(128L)
        assertThat(photo2.path).isEqualTo("/de/6932_2.jpg")
        assertThat(photo2.photographer).isEqualTo("@user10")
        assertThat(photo2.license).isEqualTo("CC0_10")
        assertThat(photo2.outdated).isTrue()
    }

    @Test
    fun photoStationsByPhotographerAnonymInAllCountries() {
        val response = restTemplate.getForEntity<PhotoStationsDto>(
            "http://localhost:$port/photoStationsByPhotographer/Anonym"
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val photoStationsDto = response.body
        assertThat(photoStationsDto).isNotNull()

        val licenses = photoStationsDto!!.licenses
        assertThat(licenses).containsExactlyInAnyOrder(
            PhotoLicenseDto(
                "CC0_10",
                "CC0 1.0 Universell (CC0 1.0)",
                URI.create("https://creativecommons.org/publicdomain/zero/1.0/")
            )
        )

        val photographers = photoStationsDto.photographers
        assertThat(photographers)
            .containsExactly(PhotographerDto("Anonym", URI.create("https://railway-stations.org")))
        assertThat(photographers)
            .noneMatch { photographerDto -> photographerDto.name != "Anonym" }
        assertThat(
            photoStationsDto.stations.map(PhotoStationDto::country).toSet()
        ).containsExactlyInAnyOrder("de", "ch")
        assertThat(
            photoStationsDto.stations
                .flatMap { photoStationDto -> photoStationDto.photos })
            .noneMatch { photoDto: PhotoDto -> photoDto.photographer != "Anonym" }

        val station = photoStationsDto.stations
            .first { photoStationDto: PhotoStationDto -> photoStationDto.country == "de" && photoStationDto.id == "6998" }
        assertThat(station.country).isEqualTo("de")
        assertThat(station.id).isEqualTo("6998")

        val photo1 = station.photos[0]
        assertThat(photo1.id).isEqualTo(54)
        assertThat(photo1.path).isEqualTo("/de/6998_1.jpg")
        assertThat(photo1.photographer).isEqualTo("Anonym")
        assertThat(photo1.license).isEqualTo("CC0_10")
        assertThat(photo1.outdated).isFalse()
    }

    @ParameterizedTest
    @ValueSource(longs = [-1, 0, 801])
    fun photoStationsByRecentPhotoImports_with_sinceHours_out_of_range(sinceHours: Long) {
        val response = restTemplate.getForEntity<String>(
            "http://localhost:$port/photoStationsByRecentPhotoImports?sinceHours=$sinceHours",
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun photoStationsByRecentPhotoImports_with_sinceHours() {
        val sinceHours = 5
        insertPhotoForDe7051("/de/7051_1.jpg", Instant.now().minus((sinceHours + 1).toLong(), ChronoUnit.HOURS))
        insertPhotoForDe7051("/de/7051_2.jpg", Instant.now())

        val response = restTemplate.getForEntity<PhotoStationsDto>(
            "http://localhost:$port/photoStationsByRecentPhotoImports?sinceHours=$sinceHours"
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val photoStationsDto = response.body
        assertThat(photoStationsDto).isNotNull()
        val station = photoStationsDto!!.stations
            .first { photoStationDto: PhotoStationDto -> photoStationDto.country == "de" && photoStationDto.id == "7051" }
        assertThat(station.country).isEqualTo("de")
        assertThat(station.id).isEqualTo("7051")
        assertThat(station.photos).hasSize(1)
        val photo1 = station.photos[0]
        assertThat(photo1.id).isNotNull()
        assertThat(photo1.path).isEqualTo("/de/7051_2.jpg")
    }

    private fun insertPhotoForDe7051(urlPath: String, createdAt: Instant) {
        photoDao.insert(
            Photo(
                stationKey = Station.Key("de", "7051"),
                primary = true,
                urlPath = urlPath,
                photographer = User(
                    id = 11,
                    name = "Jim Knopf",
                    url = "photographerUrl",
                    license = License.CC0_10,
                    ownPhotos = true,
                ),
                createdAt = createdAt,
                license = License.CC0_10,
            )
        )
    }

    @Test
    fun outdatedStationById() {
        val station = loadDeStationByStationId("7051")
        assertThat(station!!.country).isEqualTo("de")
        assertThat(station.idStr).isEqualTo("7051")
        assertThat(station.outdated).isTrue()
    }

    @Test
    fun stationByIdNotFound() {
        loadRaw("/de/stations/11111111111", HttpStatus.NOT_FOUND, String::class.java)
    }

    @Test
    fun stationsDe() {
        val stations = assertLoadStationsOk("/de/stations")
        assertThat(findByKey(stations, Station.Key("de", "6721"))).isNotNull()
        assertThat(findByKey(stations, Station.Key("ch", "8500126"))).isNull()
    }

    @Test
    fun stationsDeQueryParam() {
        val stations = assertLoadStationsOk("/stations?country=de")
        assertThat(findByKey(stations, Station.Key("de", "6721"))).isNotNull()
        assertThat(findByKey(stations, Station.Key("ch", "8500126"))).isNull()
    }

    @Test
    fun stationsDeChQueryParam() {
        val stations = assertLoadStationsOk("/stations?country=de&country=ch")
        assertThat(findByKey(stations, Station.Key("de", "6721"))).isNotNull()
        assertThat(findByKey(stations, Station.Key("ch", "8500126"))).isNotNull()
    }

    @Test
    fun stationsDePhotograph() {
        val stations = assertLoadStationsOk("/de/stations?photographer=@user10")
        assertThat(findByKey(stations, Station.Key("de", "6966"))).isNotNull()
    }

    @Test
    fun stationsCh() {
        val stations = assertLoadStationsOk("/ch/stations")
        assertThat(findByKey(stations, Station.Key("ch", "8500126"))).isNotNull()
        assertThat(findByKey(stations, Station.Key("de", "6721"))).isNull()
    }

    @Test
    fun stationsUnknownCountry() {
        val stations = assertLoadStationsOk("/jp/stations")
        assertThat(stations.size).isEqualTo(0)
    }

    @Test
    fun stationsDeFromAnonym() {
        val stations = assertLoadStationsOk("/de/stations?photographer=Anonym")
        assertThat(stations.size).isEqualTo(9)
    }

    @Test
    @Throws(IOException::class)
    fun stationsJson() {
        val response = loadRaw("/de/stations", HttpStatus.OK, String::class.java)
        val jsonNode = mapper.readTree(response.body)
        assertThat(jsonNode).isNotNull()
        assertThat(jsonNode.isArray).isTrue()
        assertThat(jsonNode.size()).isEqualTo(730)
    }

    fun assertLoadStationsOk(path: String?): List<StationDto> {
        val response = loadRaw(path, HttpStatus.OK, Array<StationDto>::class.java)

        if (response.statusCode !== HttpStatus.OK) {
            return listOf()
        }
        return response.body!!.toList()
    }

    fun <T> loadRaw(path: String?, expectedStatus: HttpStatus?, responseType: Class<T>?): ResponseEntity<T> {
        val response = restTemplate.getForEntity("http://localhost:$port$path", responseType)

        assertThat(response.statusCode).isEqualTo(expectedStatus)
        return response
    }

    fun findByKey(stations: List<StationDto>, key: Station.Key): StationDto? {
        return stations
            .firstOrNull { station: StationDto? -> station!!.country == key.country && station.idStr == key.id }
    }

    @Test
    fun photographersDe() {
        val response = loadRaw("/photographers?country=de", HttpStatus.OK, String::class.java)
        assertThatJson(response.body!!).isEqualTo(
            """
            {"@user27":31,"@user8":29,"@user10": "${'$'}{json-unit.any-number}","@user0":9}
        """.trimIndent()
        )
    }

    fun loadStationDe6932(): StationDto? {
        return loadDeStationByStationId("6932")
    }

    fun loadDeStationByStationId(stationId: String): StationDto? {
        return loadRaw("/de/stations/$stationId", HttpStatus.OK, StationDto::class.java).body
    }

    @Test
    fun statisticDeJson() {
        val response = loadRaw("/stats?country=de", HttpStatus.OK, String::class.java)
        assertThatJson(response.body!!).isEqualTo(
            """
            {"total":"${'$'}{json-unit.any-number}","withPhoto": "${'$'}{json-unit.any-number}","withoutPhoto":646,"photographers":4,"countryCode":"de"}
        """.trimIndent()
        )
    }

    @Test
    fun photoUploadForbidden() {
        val headers = HttpHeaders()
        headers.add("Upload-Token", "edbfc44727a6fd4f5b029aff21861a667a6b4195")
        headers.add("Nickname", "nickname")
        headers.add("Email", "nickname@example.com")
        headers.add("Station-Id", "4711")
        headers.add("Country", "de")
        headers.contentType = MediaType.IMAGE_JPEG
        val request = HttpEntity(IMAGE, headers)
        val response = restTemplate.postForEntity<String>(
            "http://localhost:$port/photoUpload", request,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    @Throws(IOException::class)
    fun photoUploadUnknownStationThenDeletePhotoThenDeleteStation() {
        // upload unknown station with photo
        val headers = HttpHeaders()
        headers.add("Station-Title", URLEncoder.encode("Hintertupfingen", StandardCharsets.UTF_8))
        headers.add("Latitude", "50.123")
        headers.add("Longitude", "9.123")
        headers.add("Comment", "Missing Station")
        headers.contentType = MediaType.IMAGE_JPEG
        val request = HttpEntity(IMAGE, headers)
        val uploadResponse = restTemplateWithBasicAuthUser10().postForEntity<String>(
            "http://localhost:$port/photoUpload", request,
        )

        assertThat(uploadResponse.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        val inboxResponse = mapper.readTree(uploadResponse.body)
        val uploadId = inboxResponse["id"].asInt()
        val filename = inboxResponse["filename"].asText()
        assertThat(filename).isNotBlank()
        assertThat(inboxResponse["state"].asText()).isEqualTo("REVIEW")
        assertThat(inboxResponse["crc32"].asLong()).isEqualTo(312729961L)


        // get userInbox
        val userInboxJson = loadUser10UserInboxAsJson()[0]
        assertThat(userInboxJson["id"].asInt()).isEqualTo(uploadId)
        assertThat(userInboxJson["state"].asText()).isEqualTo("REVIEW")
        assertThat(userInboxJson["inboxUrl"].asText().endsWith("/inbox/$filename")).isTrue()


        // download uploaded photo from inbox
        val photoResponse = restTemplate.getForEntity<ByteArray>(
            "http://localhost:$port/inbox/$filename"
        )
        val inputImage = ImageIO.read(ByteArrayInputStream(Objects.requireNonNull(photoResponse.body)))
        assertThat(inputImage).isNotNull()

        // we cannot binary compare the result anymore, the photos are re-encoded
        // assertThat(IOUtils.readFully((InputStream)photoResponse.getEntity(), IMAGE.length)).isEqualTo(IMAGE));

        // simulate VsionAI
        Files.move(workDir.inboxToProcessDir.resolve(filename), workDir.inboxProcessedDir.resolve(filename))


        // get userInbox processed
        val userInboxProcessedJson = loadUser10UserInboxAsJson()[0]
        assertThat(userInboxProcessedJson["id"].asInt()).isEqualTo(uploadId)
        assertThat(userInboxProcessedJson["state"].asText()).isEqualTo("REVIEW")
        assertThat(userInboxProcessedJson["inboxUrl"].asText().endsWith("/inbox/processed/$filename"))
            .isTrue()

        // send import command
        sendInboxCommand(
            """
                {
                	 "id": %s,
                	 "stationId": "%s",
                	 "countryCode": "de",
                	 "title": "Hintertupfingen",
                	 "lat": 50.123,
                	 "lon": 9.123,
                	 "active": true,
                	 "command": "IMPORT_MISSING_STATION"
                }
                
                """.trimIndent().format(uploadId, "Z")
        )

        val stationId = "Z1192"

        // assert station is imported
        val newStation = loadDeStationByStationId(stationId)
        assertThat(newStation!!.title).isEqualTo("Hintertupfingen")
        assertThat(newStation.lat).isEqualTo(50.123)
        assertThat(newStation.lon).isEqualTo(9.123)
        assertThat(newStation.photographer).isEqualTo("@user10")
        assertThat(newStation.photoUrl).isNotNull()


        // get userInbox processed
        val userInboxImportedJson = loadUser10UserInboxAsJson()[0]
        assertThat(userInboxImportedJson["id"].asInt()).isEqualTo(uploadId)
        assertThat(userInboxImportedJson["state"].asText()).isEqualTo("ACCEPTED")
        assertThat(userInboxImportedJson["inboxUrl"].asText().endsWith("/inbox/done/$filename")).isTrue()


        // send problem report wrong photo
        val problemReportWrongPhotoJson = """
                {
                	"countryCode": "de",
                	"stationId": "%s",
                	"type": "WRONG_PHOTO",
                	"comment": "This photo is clearly wrong"
                }
                """.trimIndent().format(stationId)
        val idWrongPhoto = sendProblemReport(problemReportWrongPhotoJson)

        // get userInbox with problem report
        val userInboxWithProblemJson = loadUser10UserInboxAsJson()
        assertThat(userInboxWithProblemJson[1]["id"].asInt())
            .isEqualTo(uploadId) // upload is now second entry
        assertThat(userInboxWithProblemJson[0]["id"].asInt()).isEqualTo(idWrongPhoto)
        assertThat(userInboxWithProblemJson[0]["state"].asText()).isEqualTo("REVIEW")


        // delete photo
        sendInboxCommand("{\"id\": $idWrongPhoto, \"command\": \"DELETE_PHOTO\"}")

        // assert station has no photo anymore
        val deletedPhotoStation = loadDeStationByStationId(stationId)
        assertThat(deletedPhotoStation!!.photoUrl).isNull()


        // get userInbox with problem report
        val userInboxProblemAcceptedJson = loadUser10UserInboxAsJson()
        assertThat(userInboxProblemAcceptedJson[0]["id"].asInt()).isEqualTo(idWrongPhoto)
        assertThat(userInboxProblemAcceptedJson[0]["state"].asText()).isEqualTo("ACCEPTED")


        // send problem report station not existing
        val problemReportStationNonExistentJson = """
                {
                	"countryCode": "de",
                	"stationId": "%s",
                	"type": "STATION_NONEXISTENT",
                	"comment": "This photo is clearly wrong"
                }
                """.trimIndent().format(stationId)
        val idStationNonExistent = sendProblemReport(problemReportStationNonExistentJson)


        // get userInbox with problem report
        val userInboxProblem2Json = loadUser10UserInboxAsJson()
        assertThat(userInboxProblem2Json[0]["id"].asInt()).isEqualTo(idStationNonExistent)
        assertThat(userInboxProblem2Json[0]["state"].asText()).isEqualTo("REVIEW")


        // delete station
        sendInboxCommand("{\"id\": $idStationNonExistent, \"command\": \"DELETE_STATION\"}")

        // assert station doesn't exist anymore
        loadRaw("/de/stations/$stationId", HttpStatus.NOT_FOUND, String::class.java)


        // get userInbox with problem report
        val userInboxProblem2AcceptedJson = loadUser10UserInboxAsJson()
        assertThat(userInboxProblem2AcceptedJson[0]["id"].asInt()).isEqualTo(idStationNonExistent)
        assertThat(userInboxProblem2AcceptedJson[0]["state"].asText()).isEqualTo("ACCEPTED")
    }

    @Throws(JsonProcessingException::class)
    private fun loadUser10UserInboxAsJson(): JsonNode {
        val userInboxResponse = restTemplateWithBasicAuthUser10().getForEntity<String>(
            "http://localhost:$port/userInbox?showCompletedEntries=true"
        )
        return mapper.readTree(userInboxResponse.body)
    }

    @Test
    fun getInboxWithBasicAuthPasswordFail() {
        val response = restTemplate.withBasicAuth("@user27", "blahblubb")
            .getForEntity<String>("http://localhost:$port/adminInbox")

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun problemReportWithWrongLocation() {
        assertCoordinatesOfStation6815(51.5764217543438, 11.281417285)

        val problemReportJson = """
                {
                	"countryCode": "de",
                	"stationId": "6815",
                	"type": "WRONG_LOCATION",
                	"comment": "Station at wrong location",
                	"lat": 51.123,
                	"lon": 11.123
                }
                """.trimIndent()
        val id = sendProblemReport(problemReportJson)
        sendInboxCommand("{ \"id\": $id, \"command\": \"UPDATE_LOCATION\", \"lat\": 51.129, \"lon\": 11.129 }")

        assertCoordinatesOfStation6815(51.129, 11.129)
    }

    @Throws(JsonProcessingException::class)
    fun sendProblemReport(problemReportJson: String?): Int {
        val responsePostProblem = restTemplateWithBasicAuthUser10()
            .postForEntity<String>(
                "http://localhost:$port/reportProblem",
                HttpEntity(problemReportJson, createJsonHeaders()),
            )
        assertThat(responsePostProblem.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        val jsonNodePostProblemReponse = mapper.readTree(responsePostProblem.body)
        assertThat(jsonNodePostProblemReponse).isNotNull()
        assertThat(jsonNodePostProblemReponse["state"].asText()).isEqualTo("REVIEW")
        return jsonNodePostProblemReponse["id"].asInt()
    }

    fun assertCoordinatesOfStation6815(lat: Double, lon: Double) {
        val station = loadDeStationByStationId("6815")
        assertThat(station).isNotNull()
        assertThat(station!!.lat).isEqualTo(lat)
        assertThat(station.lon).isEqualTo(lon)
    }

    @Test
    fun problemReportWithWrongStationName() {
        val stationBefore = loadDeStationByStationId("6815")
        assertThat(stationBefore).isNotNull()
        assertThat(stationBefore!!.title).isEqualTo("Wippra")

        val problemReportJson = """
                {
                	"countryCode": "de",
                	"stationId": "6815",
                	"type": "WRONG_NAME",
                	"comment": "Correct Name is 'New Name'"
                }
                """.trimIndent()
        val id = sendProblemReport(problemReportJson)
        sendInboxCommand("{\"id\": $id, \"command\": \"CHANGE_NAME\", \"title\": \"Admin New Name\"}")

        val stationAfter = loadDeStationByStationId("6815")
        assertThat(stationAfter).isNotNull()
        assertThat(stationAfter!!.title).isEqualTo("Admin New Name")
    }

    @Test
    fun problemReportWithOutdatedPhoto() {
        assertOutdatedPhotoOfStation7065(false)

        val problemReportJson = """
                {
                	"countryCode": "de",
                	"stationId": "7065",
                	"photoId": 79,
                	"type": "PHOTO_OUTDATED",
                	"comment": "Photo is outdated"
                }
                """.trimIndent()
        val id = sendProblemReport(problemReportJson)
        sendInboxCommand("{\"id\": $id, \"command\": \"PHOTO_OUTDATED\"}")

        assertOutdatedPhotoOfStation7065(true)
    }

    fun createJsonHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        return headers
    }

    fun sendInboxCommand(inboxCommand: String?) {
        val responseInboxCommand = restTemplateWithBasicAuthUser10()
            .postForEntity<String>(
                "http://localhost:$port/adminInbox",
                HttpEntity(inboxCommand, createJsonHeaders()),
            )
        assertThat(responseInboxCommand.statusCode).isEqualTo(HttpStatus.OK)
        val jsonNodeInboxCommandReponse = mapper.readTree(responseInboxCommand.body)
        assertThat(jsonNodeInboxCommandReponse).isNotNull()
        assertThat(jsonNodeInboxCommandReponse["status"].asInt()).isEqualTo(200)
        assertThat(jsonNodeInboxCommandReponse["message"].asText()).isEqualTo("ok")
    }

    fun assertOutdatedPhotoOfStation7065(outdated: Boolean) {
        val station = loadDeStationByStationId("7065")
        assertThat(station).isNotNull()
        assertThat(station!!.outdated).isEqualTo(outdated)
    }

    fun restTemplateWithBasicAuthUser10(): TestRestTemplate {
        return restTemplate.withBasicAuth("@user10", "uON60I7XWTIN")
    }

    @Test
    fun getInboxWithBasicAuthNotAuthorized() {
        val response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
            .getForEntity<String>("http://localhost:$port/adminInbox")

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun getInboxWithBasicAuth() {
        val response = restTemplateWithBasicAuthUser10()
            .getForEntity<String>("http://localhost:$port/adminInbox")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val jsonNode = mapper.readTree(response.body)
        assertThat(jsonNode).isNotNull()
        assertThat(jsonNode.isArray).isTrue()
    }

    @Test
    fun postAdminInboxCommandWithUnknownInboxExntry() {
        val headers = createJsonHeaders()
        val response = restTemplateWithBasicAuthUser10()
            .postForEntity<String>(
                "http://localhost:$port/adminInbox",
                HttpEntity("{\"id\": -1, \"command\": \"IMPORT_PHOTO\"}", headers),
            )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val jsonNode = mapper.readTree(response.body)
        assertThat(jsonNode["status"].asInt()).isEqualTo(400)
        assertThat(jsonNode["message"].asText()).isEqualTo("No pending inbox entry found")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/countries", "/countries.json"])
    fun countries(path: String?) {
        val response = loadRaw(path, HttpStatus.OK, String::class.java)
        assertThatJson(response.body!!)
            .whenever(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(
                """
                [
                 {
                    "code": "de",
                    "name": "Deutschland",
                    "active": true,
                    "email": "info@railway-stations.org",
                    "timetableUrlTemplate": "https://mobile.bahn.de/bin/mobil/bhftafel.exe/dox?bt=dep&max=10&rt=1&use_realtime_filter=1&start=yes&input={title}",
                    "providerApps": [
                      {
                        "type": "android",
                        "name": "DB Navigator",
                        "url": "https://play.google.com/store/apps/details?id=de.hafas.android.db"
                      },
                      {
                        "type": "android",
                        "name": "FlixTrain",
                        "url": "https://play.google.com/store/apps/details?id=de.meinfernbus"
                      },
                      {
                        "type": "ios",
                        "name": "DB Navigator",
                        "url": "https://apps.apple.com/app/db-navigator/id343555245"
                      }
                    ]
                  },
                  {
                    "code": "ch",
                    "name": "Schweiz",
                    "active": true,
                    "email": "fotos@schweizer-bahnhoefe.ch",
                    "timetableUrlTemplate": "http://fahrplan.sbb.ch/bin/stboard.exe/dn?input={title}&REQTrain_name=&boardType=dep&time=now&maxJourneys=20&selectDate=today&productsFilter=1111111111&start=yes",
                    "providerApps": [
                      {
                        "type": "android",
                        "name": "SBB Mobile",
                        "url": "https://play.google.com/store/apps/details?id=ch.sbb.mobile.android.b2c"
                      },
                      {
                        "type": "ios",
                        "name": "SBB Mobile",
                        "url": "https://apps.apple.com/app/sbb-mobile/id294855237"
                      }
                    ]
                  }
                 ]
            """.trimIndent()
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["/countries", "/countries.json"])
    fun countriesAll(path: String) {
        val response = loadRaw("$path?onlyActive=false", HttpStatus.OK, String::class.java)
        val jsonNode = mapper.readTree(response.body)
        assertThat(jsonNode).isNotNull()
        assertThat(jsonNode.isArray).isTrue()
        assertThat(jsonNode.size()).isEqualTo(4)
    }

    @TestConfiguration
    internal class SpringConfig {
        @Bean
        fun workDir(): WorkDir {
            return WorkDir(createTempWorkDir(), null)
        }

        private fun createTempWorkDir(): String {
            try {
                return Files.createTempDirectory("workDir-" + System.currentTimeMillis()).toFile().absolutePath
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        @Bean
        fun openApiValidationFilter(): Filter {
            return OpenApiValidationFilter(true, true)
        }

        @Bean
        fun addOpenApiValidationInterceptor(@Value("classpath:static/openapi.yaml") apiSpecification: Resource?): WebMvcConfigurer {
            val specResource = EncodedResource(
                apiSpecification!!, StandardCharsets.UTF_8
            )
            val openApiValidationInterceptor = OpenApiValidationInterceptor(specResource)
            return object : WebMvcConfigurer {
                override fun addInterceptors(registry: InterceptorRegistry) {
                    registry.addInterceptor(openApiValidationInterceptor)
                }
            }
        }
    }

}
