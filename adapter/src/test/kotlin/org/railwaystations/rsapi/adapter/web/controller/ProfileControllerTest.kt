package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.UserDao
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.adapter.web.OpenApiValidatorUtil.validOpenApiResponse
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.app.auth.LazySodiumPasswordEncoder
import org.railwaystations.rsapi.app.auth.RSAuthenticationProvider
import org.railwaystations.rsapi.app.auth.RSUserDetailsService
import org.railwaystations.rsapi.app.auth.WebSecurityConfig
import org.railwaystations.rsapi.core.config.MessageSourceConfig
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.railwaystations.rsapi.core.model.UserTestFixtures.EXISTING_USER_ID
import org.railwaystations.rsapi.core.model.UserTestFixtures.USER_AGENT
import org.railwaystations.rsapi.core.model.UserTestFixtures.USER_EMAIL
import org.railwaystations.rsapi.core.model.UserTestFixtures.USER_NAME
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase.ProfileConflictException
import org.railwaystations.rsapi.core.services.ProfileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@WebMvcTest(controllers = [ProfileController::class], properties = ["mailVerificationUrl=EMAIL_VERIFICATION_URL"])
@Import(
    value = [
        WebSecurityConfig::class,
        WebMvcTestApplication::class,
        ErrorHandlingControllerAdvice::class,
        MockMvcTestConfiguration::class,
        ProfileService::class,
        RSUserDetailsService::class,
        RSAuthenticationProvider::class,
        LazySodiumPasswordEncoder::class,
        MessageSourceConfig::class,
        RequestUtil::class]
)
@ActiveProfiles("mockMvcTest")
class ProfileControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var profileService: ProfileService

    @MockkBean
    private lateinit var userDao: UserDao

    @Test
    fun getMyProfile() {
        givenExistingUser()

        mvc.perform(
            get("/myProfile")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .contentType("application/json")
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isOk())
            .andExpect(
                json().isEqualTo(
                    """
                    {
                      "nickname": "nickname",
                      "license": "CC0 1.0 Universell (CC0 1.0)",
                      "photoOwner": true,
                      "email": "existing@example.com",
                      "anonymous": true,
                      "admin": false,
                      "emailVerified": true,
                      "sendNotifications": false
                    }
            """.trimIndent()
                )
            )
    }

    private fun givenExistingUser(): User {
        val key =
            "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000"
        val user = UserTestFixtures.createUserNickname().copy(
            id = EXISTING_USER_ID,
            email = USER_EMAIL,
            key = key
        )
        every { userDao.findByEmail(user.email!!) } returns user
        every { userDao.findByName(user.name) } returns user
        return user
    }

    private fun basicHttpAuthForExistingUser(): RequestPostProcessor {
        return httpBasic(USER_EMAIL, "y89zFqkL6hro")
    }

    private fun postChangePassword(newPasswordBody: String?): ResultActions {
        var action = post("/changePassword")
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .secure(true)
            .with(basicHttpAuthForExistingUser())
            .with(csrf())

        if (newPasswordBody != null) {
            action = action.contentType("application/json").content(newPasswordBody)
        }

        return mvc.perform(action).andExpect(validOpenApiResponse())
    }

    @Test
    fun changePasswordTooShortBody() {
        givenExistingUser()
        every { profileService.changePassword(any<User>(), eq("secret")) } throws IllegalArgumentException()

        postChangePassword("{\"newPassword\": \"secret\"}")
            .andExpect(status().isBadRequest())
    }

    @Test
    fun changePasswordBody() {
        val user = givenExistingUser()
        every { profileService.changePassword(user, "secretlong") } returns Unit

        postChangePassword("{\"newPassword\": \"secretlong\"}")
            .andExpect(status().isOk())
    }

    @Test
    fun updateMyProfile() {
        val existingUser = givenExistingUser()
        val newProfileJson = """
                    { "nickname": "new_name", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.trimIndent().format(USER_EMAIL)
        val newUser = existingUser.copy(
            id = 0,
            name = "new_name",
            key = null,
            url = "http://twitter.com/",
            email = USER_EMAIL,
            emailVerification = null,
            sendNotifications = true,
        )
        every { profileService.updateProfile(existingUser, newUser, USER_AGENT) } returns Unit

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isOk())
    }

    private fun postMyProfileWithOpenApiValidation(newProfileJson: String): ResultActions {
        return postMyProfile(newProfileJson).andExpect(validOpenApiResponse())
    }

    private fun postMyProfile(newProfileJson: String): ResultActions {
        return mvc.perform(
            post("/myProfile")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .contentType("application/json")
                .content(newProfileJson)
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf())
        )
    }

    @Test
    fun updateMyProfileConflict() {
        every { profileService.updateProfile(any(), any(), any()) } throws ProfileConflictException()
        givenExistingUser()
        val newProfileJson = """
                    { "nickname": "new_name", "email": "%s", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.trimIndent().format(USER_EMAIL)

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isConflict())
    }

    @Test
    fun updateMyProfileNameTooLong() {
        val existingUser = givenExistingUser()
        val newProfileJson = """
                    { "nickname": "A very long name with a lot of extra words to overfill the database column", "email": "${existingUser.email}", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.trimIndent()

        postMyProfile(newProfileJson).andExpect(status().isBadRequest())

        verify(exactly = 0) { profileService.updateProfile(any(), any(), any()) }
    }

    @Test
    fun updateMyProfileNewMail() {
        val existingUser = givenExistingUser()
        val newProfileJson = """
                    { "nickname": "%s", "email": "newname@example.com", "link": "http://twitter.com/", "license": "CC0", "anonymous": true, "sendNotifications": true, "photoOwner": true }
                """.trimIndent().format(USER_NAME)
        val newUser = existingUser.copy(
            id = 0,
            name = USER_NAME,
            email = "newname@example.com",
            url = "http://twitter.com/",
            key = null,
            emailVerification = null,
            sendNotifications = true,
        )
        every { profileService.updateProfile(existingUser, newUser, USER_AGENT) } returns Unit

        postMyProfileWithOpenApiValidation(newProfileJson).andExpect(status().isOk())
    }

    @Test
    fun verifyEmailSuccess() {
        val token = "verification"
        val user = UserTestFixtures.createUserNickname().copy(
            id = EXISTING_USER_ID,
            emailVerification = token,
        )
        every { profileService.emailVerification(token) } returns user

        getEmailVerification(token).andExpect(status().isOk())
    }

    private fun getEmailVerification(token: String): ResultActions {
        return mvc.perform(
            get("/emailVerification/$token")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
    }

    @Test
    fun verifyEmailFailed() {
        every { profileService.emailVerification("wrong_token") } returns null

        getEmailVerification("wrong_token").andExpect(status().isNotFound())
    }

    @Test
    fun resendEmailVerification() {
        val user = givenExistingUser()
        every { profileService.resendEmailVerification(user) } returns Unit

        mvc.perform(
            post("/resendEmailVerification")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf())
        )
            .andExpect(validOpenApiResponse())
            .andExpect(status().isOk())
    }

    @Test
    fun deleteMyProfile() {
        val user = givenExistingUser()
        every { profileService.deleteProfile(user, USER_AGENT) } returns Unit

        mvc.perform(
            delete("/myProfile")
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .secure(true)
                .with(basicHttpAuthForExistingUser())
                .with(csrf())
        )
            .andExpect(status().isNoContent())
            .andExpect(validOpenApiResponse())
    }

}
