package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.app.auth.AuthUser
import org.railwaystations.rsapi.core.model.Coordinates
import org.railwaystations.rsapi.core.model.InboxResponse
import org.railwaystations.rsapi.core.model.InboxStateQuery
import org.railwaystations.rsapi.core.model.InboxStateQuery.InboxState
import org.railwaystations.rsapi.core.model.ProblemReport
import org.railwaystations.rsapi.core.model.ProblemReportType
import org.railwaystations.rsapi.core.model.PublicInboxEntry
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.railwaystations.rsapi.core.ports.ManageInboxUseCase
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.railwaystations.rsapi.utils.OpenApiValidatorUtil.validOpenApiResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.LocaleResolver
import java.time.Instant
import java.util.*

private const val USER_AGENT = "UserAgent"

@WebMvcTest(controllers = [InboxController::class])
@Import(RequestUtil::class)
@ActiveProfiles("mockMvcTest")
internal class InboxControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var manageInboxUseCase: ManageInboxUseCase

    @MockkBean(relaxed = true)
    private lateinit var manageProfileUseCase: ManageProfileUseCase

    @MockkBean
    private lateinit var localeResolver: LocaleResolver

    private val userNickname = UserTestFixtures.createUserNickname()

    @BeforeEach
    fun setUp() {
        every { localeResolver.resolveLocale(any()) } returns Locale.GERMAN
    }

    @Test
    fun getUserInboxUnauthorized() {
        mvc.perform(
            get("/userInbox")
                .header(HttpHeaders.AUTHORIZATION, "any")
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun getUserInbox() {
        every {
            manageInboxUseCase.userInbox(userNickname)
        } returns listOf(
            InboxStateQuery(id = 1, countryCode = "de", stationId = "4711", state = InboxState.REVIEW),
            InboxStateQuery(id = 2, countryCode = "de", stationId = "1234", state = InboxState.ACCEPTED),
            InboxStateQuery(id = 3, countryCode = "de", stationId = "5678", state = InboxState.REJECTED),
            InboxStateQuery(id = 4, countryCode = "ch", stationId = "0815", state = InboxState.REVIEW),
        )

        mvc.perform(
            get("/userInbox")
                .header(HttpHeaders.AUTHORIZATION, "any")
                .with(user(AuthUser(userNickname, listOf())))
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isOk())
            .andExpect(
                json().isEqualTo(
                    """
                [
                  {
                    "id": 1,
                    "state": "REVIEW",
                    "countryCode": "de",
                    "stationId": "4711"
                  },
                  {
                    "id": 2,
                    "state": "ACCEPTED",
                    "countryCode": "de",
                    "stationId": "1234"
                  },
                  {
                    "id": 3,
                    "state": "REJECTED",
                    "countryCode": "de",
                    "stationId": "5678"
                  },
                  {
                    "id": 4,
                    "state": "REVIEW",
                    "countryCode": "ch",
                    "stationId": "0815"
                  }
                ]                
            """.trimIndent()
                )
            )
    }

    @Test
    fun postUserInboxUnauthorized() {
        val inboxStateQueries = """
                [
                    {"id": 1}
                ]
                """.trimIndent()

        mvc.perform(
            post("/userInbox")
                .header(HttpHeaders.AUTHORIZATION, "any")
                .contentType("application/json")
                .content(inboxStateQueries)
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun postUserInbox() {
        val inboxStateQuery = InboxStateQuery(
            id = 1,
            countryCode = "de",
            stationId = "4711",
            title = "title",
            newTitle = "newTitle",
            coordinates = Coordinates(1.0, 2.0),
            newCoordinates = Coordinates(3.0, 4.0),
            state = InboxState.REJECTED,
            rejectedReason = "reject reason",
            problemReportType = ProblemReportType.PHOTO_OUTDATED,
            inboxUrl = "http://api.railway-stations.org/inbox/1.jpg",
            filename = "1.jpg",
            comment = "a single comment",
            crc32 = 1234L,
            createdAt = Instant.now(),
        )
        every { manageInboxUseCase.userInbox(userNickname, listOf(1L, 2L)) } returns listOf(inboxStateQuery)

        val inboxStateQueries = """
                [
                    {"id": 1},
                    {"id": 2}
                ]
                """.trimIndent()

        mvc.perform(
            post("/userInbox")
                .header(HttpHeaders.AUTHORIZATION, "any")
                .contentType("application/json")
                .content(inboxStateQueries)
                .with(user(AuthUser(userNickname, listOf())))
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isOk())
            .andExpect(
                json().isEqualTo(
                    """
                    [
                      {
                        "id": 1,
                        "state": "REJECTED",
                        "countryCode": "de",
                        "stationId": "4711",
                        "title": "title",
                        "lat": 1.0,
                        "lon": 2.0,
                        "newTitle": "newTitle",
                        "newLat": 3.0,
                        "newLon": 4.0,
                        "comment": "a single comment",
                        "problemReportType": "PHOTO_OUTDATED",
                        "rejectedReason": "reject reason",
                        "inboxUrl":"http://api.railway-stations.org/inbox/1.jpg",
                        "filename": "1.jpg",
                        "crc32": 1234,
                        "createdAt": "${'$'}{json-unit.any-number}"
                      }
                    ]
            """.trimIndent()
                )
            )
    }

    private fun whenPostProblemReport(problemReportJson: String): ResultActions {
        return mvc.perform(
            post("/reportProblem")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.AUTHORIZATION, "any")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "de")
                .contentType("application/json")
                .content(problemReportJson)
                .with(user(AuthUser(userNickname, listOf())))
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
    }

    @Test
    fun postProblemReportUnauthorized() {
        val problemReportJson = """
                { "countryCode": "de", "stationId": "1234", "type": "OTHER", "comment": "something is wrong" }
            """.trimIndent()

        mvc.perform(
            post("/reportProblem")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.AUTHORIZATION, "any")
                .contentType("application/json")
                .content(problemReportJson)
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun postProblemReport() {
        every {
            manageInboxUseCase.reportProblem(
                ProblemReport(
                    countryCode = "de",
                    stationId = "1234",
                    title = "title",
                    comment = "something is wrong",
                    type = ProblemReportType.OTHER,
                    coordinates = Coordinates(50.0, 9.1),
                ),
                userNickname,
                USER_AGENT,
            )
        } returns InboxResponse(
            id = 6L,
            state = InboxResponse.InboxResponseState.REVIEW,
        )
        val problemReportJson = """
                { "countryCode": "de", "stationId": "1234", "type": "OTHER", "title": "title", "comment": "something is wrong", "lat": 50.0, "lon": 9.1}
            """.trimIndent()

        whenPostProblemReport(problemReportJson)
            .andExpect(status().isAccepted())
            .andExpect(
                json().isEqualTo(
                    """
                    {"state":"REVIEW","id":6}
                    """.trimIndent()
                )
            )

        verify { manageProfileUseCase.updateLocale(userNickname, Locale.GERMAN) }
    }

    @Test
    fun getPublicInbox() {
        every { manageInboxUseCase.publicInbox() } returns listOf(
            PublicInboxEntry(
                countryCode = "de",
                stationId = "4711",
                title = "Station Title",
                coordinates = Coordinates(50.0, 9.1),
            )
        )

        mvc.perform(
            get("/publicInbox")
                .with(user(AuthUser(userNickname, listOf())))
        )
            .andExpect(status().isOk)
            .andExpect(
                json().isEqualTo(
                    """
                    [
                      {
                        "countryCode": "de",
                        "stationId": "4711",
                        "title": "Station Title",
                        "lat": 50.0,
                        "lon": 9.1
                      }
                    ]
                    """.trimIndent()
                )
            )
    }

}