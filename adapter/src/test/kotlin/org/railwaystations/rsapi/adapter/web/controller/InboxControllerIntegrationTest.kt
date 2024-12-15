package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.CountryAdapter
import org.railwaystations.rsapi.adapter.db.InboxAdapter
import org.railwaystations.rsapi.adapter.db.PhotoAdapter
import org.railwaystations.rsapi.adapter.db.StationAdapter
import org.railwaystations.rsapi.adapter.db.UserAdapter
import org.railwaystations.rsapi.adapter.monitoring.FakeMonitor
import org.railwaystations.rsapi.adapter.photostorage.PhotoFileStorage
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
import org.railwaystations.rsapi.core.model.InboxEntryTestFixtures.createInboxEntry
import org.railwaystations.rsapi.core.model.PhotoTestFixtures.createPhoto
import org.railwaystations.rsapi.core.model.ProblemReportType
import org.railwaystations.rsapi.core.model.Station
import org.railwaystations.rsapi.core.model.StationTestFixtures.createStation
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.railwaystations.rsapi.core.model.UserTestFixtures.USER_AGENT
import org.railwaystations.rsapi.core.model.UserTestFixtures.userJimKnopf
import org.railwaystations.rsapi.core.model.UserTestFixtures.userNickname
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase
import org.railwaystations.rsapi.core.services.InboxService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock


@WebMvcTest(controllers = [InboxController::class], properties = ["inboxBaseUrl=http://inbox.railway-stations.org"])
@ContextConfiguration(
    classes = [
        WebMvcTestApplication::class,
        ErrorHandlingControllerAdvice::class,
        MockMvcTestConfiguration::class,
        WebSecurityConfig::class,
        InboxService::class,
        PhotoFileStorage::class,
        RSUserDetailsService::class,
        ClockTestConfiguration::class,
        RequestUtil::class]
)
@ActiveProfiles("mockMvcTest")
internal class InboxControllerIntegrationTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var monitor: FakeMonitor

    @Autowired
    private lateinit var clock: Clock

    @MockkBean
    private lateinit var inboxAdapter: InboxAdapter

    @MockkBean
    private lateinit var stationAdapter: StationAdapter

    @MockkBean
    private lateinit var userAdapter: UserAdapter

    @MockkBean
    private lateinit var countryAdapter: CountryAdapter

    @MockkBean
    private lateinit var photoAdapter: PhotoAdapter

    @MockkBean
    private lateinit var manageProfileUseCase: ManageProfileUseCase

    @MockkBean
    private lateinit var authenticator: RSAuthenticationProvider

    @BeforeEach
    fun setUp() {
        val userNickname = userNickname
        every { userAdapter.findByEmail(userNickname.email!!) } returns userNickname
        val userSomeuser = UserTestFixtures.someUser
        every { userAdapter.findByEmail(userSomeuser.email!!) } returns userSomeuser

        val key0815 = Station.Key("ch", "0815")
        val station0815 = createStation(key0815, Coordinates(40.1, 7.0), createPhoto(key0815, userJimKnopf))
        every { stationAdapter.findByKey(key0815.country, key0815.id) } returns station0815

        val key1234 = Station.Key("de", "1234")
        val station1234 = createStation(key1234, Coordinates(40.1, 7.0), createPhoto(key1234, userJimKnopf))
        every { stationAdapter.findByKey(key1234.country, key1234.id) } returns station1234

        monitor.reset()
    }

    @Test
    fun userInbox() {
        val user = userNickname

        every { inboxAdapter.findById(1) } returns createInboxEntry(user, 1, "de", "4711", null, false)
        every { inboxAdapter.findById(2) } returns createInboxEntry(user, 2, "de", "1234", null, true)
        every { inboxAdapter.findById(3) } returns createInboxEntry(user, 3, "de", "5678", "rejected", true)
        every { inboxAdapter.findById(4) } returns createInboxEntry(user, 4, "ch", "0815", null, false)

        val inboxStateQueries = """
                [
                    {"id": 1},
                    {"id": 2},
                    {"id": 3},
                    {"id": 4}
                ]
                """.trimIndent()

        mvc.perform(
            post("/userInbox")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.AUTHORIZATION, "not_used_but_required")
                .contentType("application/json")
                .content(inboxStateQueries)
                .with(user(AuthUser(user, listOf())))
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].state").value("REVIEW"))
            .andExpect(jsonPath("$.[0].filename").value("1.jpg"))
            .andExpect(jsonPath("$.[1].state").value("ACCEPTED"))
            .andExpect(jsonPath("$.[2].state").value("REJECTED"))
            .andExpect(jsonPath("$.[3].state").value("REVIEW"))
    }

    private fun whenPostProblemReport(emailVerification: String, problemReportJson: String): ResultActions {
        return mvc.perform(
            post("/reportProblem")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.AUTHORIZATION, "not_used_but_required")
                .contentType("application/json")
                .content(problemReportJson)
                .with(
                    user(
                        AuthUser(
                            userNickname.copy(
                                emailVerification = emailVerification
                            ), listOf()
                        )
                    )
                )
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
    }

    @Test
    fun postProblemReportOther() {
        every {
            inboxAdapter.insert(
                InboxEntry(
                    countryCode = "de",
                    stationId = "1234",
                    photographerId = 42,
                    comment = "something is wrong",
                    createdAt = clock.instant(),
                    problemReportType = ProblemReportType.OTHER,
                )
            )
        } returns 6L
        val problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "OTHER", "comment": "something is wrong" }
                """.trimIndent()

        whenPostProblemReport(EMAIL_VERIFIED, problemReportJson)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(6))
            .andExpect(jsonPath("$.filename").doesNotExist())

        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New problem report for de 1234 - de:1234
                OTHER: something is wrong
                by nickname
                via UserAgent
                """.trimIndent()
        )
    }

    @Test
    fun postProblemReportWrongLocation() {
        every {
            inboxAdapter.insert(
                InboxEntry(
                    countryCode = "de",
                    stationId = "1234",
                    coordinates = Coordinates(50.0, 9.1),
                    photographerId = 42,
                    comment = "coordinates are slightly off",
                    createdAt = clock.instant(),
                    problemReportType = ProblemReportType.WRONG_LOCATION,
                )
            )
        } returns 6L
        val problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "WRONG_LOCATION", "lat": 50.0, "lon": 9.1, "comment": "coordinates are slightly off" }
                """.trimIndent()

        whenPostProblemReport(EMAIL_VERIFIED, problemReportJson)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(6))
            .andExpect(jsonPath("$.filename").doesNotExist())

        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New problem report for de 1234 - de:1234
                WRONG_LOCATION: coordinates are slightly off
                by nickname
                via UserAgent
                """.trimIndent()
        )
    }

    @Test
    fun postProblemReportWrongName() {
        every {
            inboxAdapter.insert(
                InboxEntry(
                    countryCode = "de",
                    stationId = "1234",
                    title = "New Name",
                    photographerId = 42,
                    comment = "name is wrong",
                    createdAt = clock.instant(),
                    problemReportType = ProblemReportType.WRONG_NAME,
                )
            )
        } returns 6L
        val problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "WRONG_NAME", "title": "New Name", "comment": "name is wrong" }
                """.trimIndent()

        whenPostProblemReport(EMAIL_VERIFIED, problemReportJson)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("REVIEW"))
            .andExpect(jsonPath("$.id").value(6))
            .andExpect(jsonPath("$.filename").doesNotExist())

        assertThat(monitor.getMessages()[0]).isEqualTo(
            """
                New problem report for de 1234 - de:1234
                WRONG_NAME: name is wrong
                by nickname
                via UserAgent
                """.trimIndent()
        )
    }

    @Test
    fun postProblemReportEmailNotVerified() {
        val problemReportJson = """
                    { "countryCode": "de", "stationId": "1234", "type": "OTHER", "comment": "something is wrong" }
                """.trimIndent()

        whenPostProblemReport("blah", problemReportJson)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.state").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.filename").doesNotExist())
    }

    @Test
    fun adminInbox() {

        every { inboxAdapter.findPendingInboxEntries() } returns listOf(
            createInboxEntry(userNickname, 1, "de", "4711", null, false),
        )

        mvc.perform(
            get("/adminInbox")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.AUTHORIZATION, "not_used_but_required")
                .contentType("application/json")
                .with(user(AuthUser(userNickname, listOf())))
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isForbidden)
    }

}
