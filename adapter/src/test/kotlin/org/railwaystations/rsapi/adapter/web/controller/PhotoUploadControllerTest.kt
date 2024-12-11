package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.railwaystations.rsapi.adapter.db.CountryAdapter
import org.railwaystations.rsapi.adapter.db.InboxAdapter
import org.railwaystations.rsapi.adapter.db.PhotoAdapter
import org.railwaystations.rsapi.adapter.db.StationDao
import org.railwaystations.rsapi.adapter.db.UserAdapter
import org.railwaystations.rsapi.adapter.monitoring.FakeMonitor
import org.railwaystations.rsapi.adapter.photostorage.PhotoFileStorage
import org.railwaystations.rsapi.adapter.photostorage.WorkDir
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.adapter.web.OpenApiValidatorUtil.validOpenApiResponse
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.adapter.web.auth.AuthUser
import org.railwaystations.rsapi.adapter.web.auth.RSAuthenticationProvider
import org.railwaystations.rsapi.adapter.web.auth.RSUserDetailsService
import org.railwaystations.rsapi.adapter.web.auth.WebSecurityConfig
import org.railwaystations.rsapi.app.ClockTestConfiguration
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.EMAIL_VERIFIED
import org.railwaystations.rsapi.core.model.InboxEntry
import org.railwaystations.rsapi.core.model.Photo
import org.railwaystations.rsapi.core.model.PhotoTestFixtures.createPhoto
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.StationTestFixtures.createStation
import org.railwaystations.rsapi.core.model.UserTestFixtures.someUser
import org.railwaystations.rsapi.core.model.UserTestFixtures.userJimKnopf
import org.railwaystations.rsapi.core.model.UserTestFixtures.userNickname
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase
import org.railwaystations.rsapi.core.services.InboxService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.Charset
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

const val IMAGE_CONTENT: String = "image-content"

private const val USER_AGENT = "UserAgent"

@WebMvcTest(
    controllers = [PhotoUploadController::class],
    properties = ["inboxBaseUrl=http://inbox.railway-stations.org"]
)
@ContextConfiguration(classes = [WebMvcTestApplication::class, ErrorHandlingControllerAdvice::class, MockMvcTestConfiguration::class, WebSecurityConfig::class])
@Import(
    InboxService::class,
    PhotoFileStorage::class,
    RSUserDetailsService::class,
    ClockTestConfiguration::class,
    RequestUtil::class
)
@ActiveProfiles("mockMvcTest")
internal class PhotoUploadControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var monitor: FakeMonitor

    @Autowired
    private lateinit var workDir: WorkDir

    @MockkBean(relaxed = true)
    private lateinit var inboxAdapter: InboxAdapter

    @MockkBean
    private lateinit var stationDao: StationDao

    @MockkBean(relaxed = true)
    private lateinit var authenticator: RSAuthenticationProvider

    @MockkBean(relaxed = true)
    private lateinit var userAdapter: UserAdapter

    @MockkBean
    private lateinit var countryAdapter: CountryAdapter

    @MockkBean(relaxed = true)
    private lateinit var photoAdapter: PhotoAdapter

    @MockkBean(relaxed = true)
    private lateinit var manageProfileUseCase: ManageProfileUseCase

    @BeforeEach
    fun setUp() {
        val userNickname = userNickname
        every { userAdapter.findByEmail(userNickname.email!!) } returns userNickname
        val userSomeuser = someUser
        every { userAdapter.findByEmail(userSomeuser.email!!) } returns userSomeuser

        val key4711 = Station.Key("de", "4711")
        val station4711 = createStationWithDs100(key4711, Coordinates(50.0, 9.0), "XYZ", null)
        val key1234 = Station.Key("de", "1234")
        val station1234 =
            createStationWithDs100(key1234, Coordinates(40.1, 7.0), "LAL", createPhoto(key1234, userJimKnopf))

        every { stationDao.findByKey(key4711.country, key4711.id) } returns station4711
        every { stationDao.findByKey(key1234.country, key1234.id) } returns station1234
        every { stationDao.countNearbyCoordinates(any()) } returns 0

        monitor.reset()
    }

    private fun createStationWithDs100(
        key: Station.Key,
        coordinates: Coordinates,
        ds100: String,
        photo: Photo?
    ): Station {
        val station = createStation(key, coordinates, photo).copy(
            ds100 = ds100,
        )
        return station
    }

    private fun whenPostImage(
        nickname: String,
        userId: Long,
        email: String,
        stationId: String?,
        country: String?,
        stationTitle: String?,
        latitude: Double?,
        longitude: Double?,
        comment: String?,
        emailVerification: String = EMAIL_VERIFIED
    ): ResultActions {
        return whenPostPhotoUpload(
            nickname,
            userId,
            email,
            stationId,
            country,
            stationTitle,
            latitude,
            longitude,
            comment,
            emailVerification,
            "image-content".toByteArray(
                Charset.defaultCharset()
            ),
            "image/jpeg"
        )
    }

    private fun whenPostPhotoUpload(
        nickname: String,
        userId: Long,
        email: String,
        stationId: String?,
        country: String?,
        stationTitle: String?,
        latitude: Double?,
        longitude: Double?,
        comment: String?,
        emailVerification: String,
        inputBytes: ByteArray?,
        contentType: String
    ): ResultActions {
        val headers = HttpHeaders()
        if (country != null) {
            headers.add("Country", country)
        }
        if (stationId != null) {
            headers.add("Station-Id", stationId)
        }
        if (stationTitle != null) {
            headers.add("Station-Title", stationTitle)
        }
        if (latitude != null) {
            headers.add("Latitude", latitude.toString())
        }
        if (longitude != null) {
            headers.add("Longitude", longitude.toString())
        }
        if (comment != null) {
            headers.add("Comment", comment)
        }
        headers.add(HttpHeaders.CONTENT_TYPE, contentType)
        headers.add(HttpHeaders.USER_AGENT, USER_AGENT)

        return mvc.perform(
            post("/photoUpload")
                .headers(headers)
                .content(inputBytes ?: byteArrayOf())
                .with(
                    user(
                        AuthUser(
                            userNickname.copy(
                                name = nickname,
                                id = userId,
                                email = email,
                                emailVerification = emailVerification,
                            ), listOf()
                        )
                    )
                )
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
    }

    @Test
    fun postMultipartFormdataEmailNotVerified() {
        every {
            authenticator.authenticate(
                UsernamePasswordAuthenticationToken("someuser@example.com", "secretUploadToken")
            )
        } returns UsernamePasswordAuthenticationToken("", "", listOf())
        every { inboxAdapter.insert(any()) } returns 1L
        val response = whenPostImageMultipartFormdata("someuser@example.com", "some_verification_token")

        assertThat(response).contains("UNAUTHORIZED")
        assertThat(response).contains("Profile incomplete, not allowed to upload photos")
        verify(exactly = 0) { inboxAdapter.insert(any()) }
        assertThat(monitor.getMessages().size).isEqualTo(0)
    }

    @Test
    fun postPhotoForExistingStationViaMultipartFormdata() {
        val uploadCaptor = slot<InboxEntry>()
        every { inboxAdapter.insert(any()) } returns 1L
        val response = whenPostImageMultipartFormdata("nickname@example.com", EMAIL_VERIFIED)

        assertThat(response).contains("REVIEW")
        assertFileWithContentExistsInInbox("image-content", "1.jpg")
        verify { inboxAdapter.insert(capture(uploadCaptor)) }
        assertUpload(uploadCaptor.captured, "de", "4711", null, null)

        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New photo upload for de 4711 - de:4711
                Some Comment
                http://inbox.railway-stations.org/1.jpg
                by nickname
                via $USER_AGENT
                """.trimIndent()
        )
    }

    private fun whenPostImageMultipartFormdata(
        email: String,
        emailVerified: String
    ): String {
        return mvc.perform(
            multipart("/photoUploadMultipartFormdata")
                .file(
                    MockMultipartFile(
                        "file",
                        "1.jpg",
                        "image/jpeg",
                        "image-content".toByteArray(Charset.defaultCharset())
                    )
                )
                .with(
                    user(
                        AuthUser(
                            userNickname.copy(
                                email = email,
                                emailVerification = emailVerified,
                            ), listOf()
                        )
                    )
                )
                .param("stationId", "4711")
                .param("countryCode", "de")
                .param("comment", "Some Comment")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.REFERER, "http://localhost/uploadPage.php")
                .header(HttpHeaders.ACCEPT, "application/json")
                .with(csrf())
        )
            .andReturn().response.contentAsString
    }

    @Test
    fun repostMissingStationWithoutPhotoViaMultipartFormdata() {
        val uploadCaptor = slot<InboxEntry>()
        every { inboxAdapter.insert(any()) } returns 1L
        val response = mvc.perform(
            multipart("/photoUploadMultipartFormdata")
                .file(MockMultipartFile("file", null, "application/octet-stream", null as ByteArray?))
                .with(user(AuthUser(userNickname, listOf())))
                .param("stationTitle", "Missing Station")
                .param("latitude", "10")
                .param("longitude", "20")
                .param("active", "true")
                .param("countryCode", "de")
                .param("comment", "Some Comment")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.REFERER, "http://localhost/uploadPage.php")
                .header(HttpHeaders.ACCEPT, "application/json")
                .with(csrf())
        )
            .andReturn().response.contentAsString

        assertThat(response).contains("REVIEW")
        verify { inboxAdapter.insert(capture(uploadCaptor)) }
        assertUpload(uploadCaptor.captured, "de", null, "Missing Station", Coordinates(10.0, 20.0))

        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                Report missing station Missing Station at https://map.railway-stations.org/index.php?countryCode=de&mlat=10.0&mlon=20.0&zoom=18&layers=M
                Some Comment
                by nickname
                via $USER_AGENT
                """.trimIndent()
        )
    }

    @Test
    fun uploadPhoto() {
        val uploadCaptor = slot<InboxEntry>()
        every { inboxAdapter.insert(any()) } returns 1L

        whenPostImage("@nick name", 42, "nickname@example.com", "4711", "de", null, null, null, "Some Comment")
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.filename").value("1.jpg"))


        assertFileWithContentExistsInInbox("image-content", "1.jpg")
        verify { inboxAdapter.insert(capture(uploadCaptor)) }
        assertUpload(uploadCaptor.captured, "de", "4711", null, null)
        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New photo upload for de 4711 - de:4711
                Some Comment
                http://inbox.railway-stations.org/1.jpg
                by @nick name
                via $USER_AGENT
                """.trimIndent()
        )
    }

    private fun assertUpload(
        inboxEntry: InboxEntry,
        countryCode: String?,
        stationId: String?,
        title: String?,
        coordinates: Coordinates?
    ) {
        assertThat(inboxEntry.countryCode).isEqualTo(countryCode)
        assertThat(inboxEntry.stationId).isEqualTo(stationId)
        assertThat(inboxEntry.title).isEqualTo(title)
        assertThat(inboxEntry.photographerId).isEqualTo(42)
        assertThat(inboxEntry.comment).isEqualTo("Some Comment")
        assertThat(Duration.between(inboxEntry.createdAt, Instant.now()).seconds < 5).isTrue()
        if (coordinates != null) {
            assertThat(inboxEntry.coordinates).isEqualTo(coordinates)
        } else {
            assertThat(inboxEntry.coordinates).isNull()
        }
        assertThat(inboxEntry.done).isFalse()
    }

    @Test
    fun postMissingStation() {
        every { inboxAdapter.insert(any()) } returns 4L
        val uploadCaptor = slot<InboxEntry>()

        whenPostImage(
            nickname = userNickname.name,
            userId = userNickname.id,
            email = userNickname.email!!,
            stationId = null,
            country = null,
            stationTitle = "Missing Station",
            latitude = 50.9876,
            longitude = 9.1234,
            comment = "Some Comment"
        )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(4))
            .andExpect(jsonPath("$.filename").value("4.jpg"))

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "4.jpg")
        verify { inboxAdapter.insert(capture(uploadCaptor)) }
        assertUpload(uploadCaptor.captured, null, null, "Missing Station", Coordinates(50.9876, 9.1234))

        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                Photo upload for missing station Missing Station at https://map.railway-stations.org/index.php?mlat=50.9876&mlon=9.1234&zoom=18&layers=M
                Some Comment
                http://inbox.railway-stations.org/4.jpg
                by ${userNickname.name}
                via $USER_AGENT
                """.trimIndent()
        )
    }

    @Test
    fun postMissingStationWithoutPhoto() {
        every { inboxAdapter.insert(any()) } returns 4L
        val uploadCaptor = slot<InboxEntry>()

        whenPostPhotoUpload(
            nickname = userNickname.name,
            userId = userNickname.id,
            email = userNickname.email!!,
            stationId = null,
            country = "de",
            stationTitle = "Missing Station",
            latitude = 50.9876,
            longitude = 9.1234,
            comment = "Some Comment",
            emailVerification = EMAIL_VERIFIED,
            inputBytes = null,
            contentType = "application/octet-stream"
        )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(4))
            .andExpect(jsonPath("$.filename").doesNotExist())

        verify { inboxAdapter.insert(capture(uploadCaptor)) }
        assertUpload(uploadCaptor.captured, "de", null, "Missing Station", Coordinates(50.9876, 9.1234))

        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                Report missing station Missing Station at https://map.railway-stations.org/index.php?countryCode=de&mlat=50.9876&mlon=9.1234&zoom=18&layers=M
                Some Comment
                by ${userNickname.name}
                via $USER_AGENT
                """.trimIndent()
        )
    }

    @ParameterizedTest
    @CsvSource(
        "-91d, 9.1234d", "91d, 9.1234d", "50.9876d, -181d", "50.9876d, 181d"
    )
    fun postMissingStationLatLonOutOfRange(latitude: Double?, longitude: Double?) {
        whenPostImage(
            nickname = userNickname.name,
            userId = userNickname.id,
            email = userNickname.email!!,
            stationId = null,
            country = null,
            stationTitle = "Missing Station",
            latitude = latitude,
            longitude = longitude,
            comment = null
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.state").value("LAT_LON_OUT_OF_RANGE"))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.filename").doesNotExist())
    }

    @Test
    fun postPhotoWithSomeUserWithTokenSalt() {
        every { inboxAdapter.insert(any()) } returns 3L
        whenPostImage("@someuser", 11, "someuser@example.com", "4711", "de", null, null, null, null)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.filename").value("3.jpg"))

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "3.jpg")
        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New photo upload for de 4711 - de:4711
                
                http://inbox.railway-stations.org/3.jpg
                by @someuser
                via $USER_AGENT
                """.trimIndent()
        )
    }

    @Test
    fun postDuplicateInbox() {
        every { inboxAdapter.insert(any()) } returns 2L
        every { inboxAdapter.countPendingInboxEntriesForStation(null, "de", "4711") } returns 1

        whenPostImage("@nick name", 42, "nickname@example.com", "4711", "de", null, null, null, null)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.filename").value("2.jpg"))

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "2.jpg")
        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New photo upload for de 4711 - de:4711 (possible duplicate!)
                
                http://inbox.railway-stations.org/2.jpg
                by @nick name
                via $USER_AGENT
                """.trimIndent()
        )
    }

    private fun assertFileWithContentExistsInInbox(content: String, filename: String) {
        val image = workDir.inboxDir.resolve(filename)
        assertThat(Files.exists(image)).isTrue()

        val inputBytes = content.toByteArray(Charset.defaultCharset())
        val outputBytes = ByteArray(inputBytes.size)
        IOUtils.readFully(Files.newInputStream(image), outputBytes)
        assertThat(outputBytes).isEqualTo(inputBytes)
    }

    @Test
    fun postDuplicate() {
        every { inboxAdapter.insert(any()) } returns 5L
        whenPostImage("@nick name", 42, "nickname@example.com", "1234", "de", null, null, null, null)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.filename").value("5.jpg"))

        assertFileWithContentExistsInInbox(IMAGE_CONTENT, "5.jpg")
        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New photo upload for de 1234 - de:1234 (possible duplicate!)
                
                http://inbox.railway-stations.org/5.jpg
                by @nick name
                via $USER_AGENT
                """.trimIndent()
        )
    }

    @Test
    fun postEmailNotVerified() {
        whenPostImage("@nick name", 42, "nickname@example.com", "4711", "de", null, null, null, null, "blahblah")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.state").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.filename").doesNotExist())
    }

    @Test
    fun postInvalidCountry() {
        every { stationDao.findByKey(eq("xy"), any()) } returns null

        whenPostImage("nickname", 42, "nickname@example.com", "4711", "xy", null, null, null, null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.state").value("NOT_ENOUGH_DATA"))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.filename").doesNotExist())
    }

}
