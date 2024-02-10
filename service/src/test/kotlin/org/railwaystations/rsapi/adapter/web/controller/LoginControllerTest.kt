package org.railwaystations.rsapi.adapter.web.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.StringContains.containsString
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.web.ErrorHandlingControllerAdvice
import org.railwaystations.rsapi.adapter.web.RequestUtil
import org.railwaystations.rsapi.app.config.MessageSourceConfig
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.UserTestFixtures
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase.ProfileConflictException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

private const val USER_AGENT = "user_agent"

@WebMvcTest(controllers = [LoginController::class], properties = ["mailVerificationUrl=EMAIL_VERIFICATION_URL"])
@ContextConfiguration(
    classes = [WebMvcTestApplication::class, ErrorHandlingControllerAdvice::class, MockMvcTestConfiguration::class, MessageSourceConfig::class, RequestUtil::class
    ]
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("mockMvcTest")
internal class LoginControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var manageProfileUseCase: ManageProfileUseCase

    @Test
    fun getLogin() {
        mvc.perform(
            get("/login")
                .with(csrf())
        )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<form class=\"form-signin\" action=\"/login\"")))
    }

    @Test
    fun postLoginResetPasswordSuccess() {
        mvc.perform(
            post("/loginResetPassword")
                .param("username", "a_user")
                .with(csrf())
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
        )
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, "/login?reset_password_success&username=a_user"))

        verify { manageProfileUseCase.resetPassword("a_user", USER_AGENT) }
    }

    @Test
    fun postLoginResetPasswordError() {
        every { manageProfileUseCase.resetPassword("a_user", USER_AGENT) } throws RuntimeException("test")

        mvc.perform(
            post("/loginResetPassword")
                .param("username", "a_user")
                .with(csrf())
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
        )
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, "/login?reset_password_error&username=a_user"))
    }

    @Test
    fun getLoginRegister() {
        mvc.perform(
            get("/loginRegister")
                .with(csrf())
        )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<form action=\"/loginRegister\"")))
    }

    @Test
    fun postLoginRegister() {
        val user = createValidUser()
        val userSlot = slot<User>()

        postLoginRegister(user)
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, "/login?register_success&username=nickname"))

        verify { manageProfileUseCase.register(capture(userSlot), USER_AGENT) }
        userSlot.captured.apply {
            assertThat(name).isEqualTo(user.name)
            assertThat(email).isEqualTo(user.email)
            assertThat(newPassword).isEqualTo(user.newPassword)
            assertThat(locale).isEqualTo(user.locale)
        }
    }

    private fun postLoginRegister(
        name: String,
        email: String?,
        newPassword: String?,
        passwordRepeat: String?,
        locale: String
    ): ResultActions {
        return mvc.perform(
            post("/loginRegister")
                .param("username", name)
                .param("email", email)
                .param("password", newPassword)
                .param("passwordRepeat", passwordRepeat)
                .with(csrf())
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .header(HttpHeaders.ACCEPT_LANGUAGE, locale)
        )
    }

    private fun postLoginRegister(user: User): ResultActions {
        return postLoginRegister(user.name, user.email, user.newPassword, user.newPassword, user.localeLanguageTag)
    }

    @Test
    fun postLoginRegisterPasswordsDontMatch() {
        val user = createValidUser()

        postLoginRegister(user.name, user.email, user.newPassword, "something_else", user.localeLanguageTag)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<div>Passwörter stimmen nicht überein</div>")))

        verify(exactly = 0) { manageProfileUseCase.register(any(), any()) }
    }

    @Test
    fun postLoginRegisterPasswordTooShort() {
        val user = createValidUser()
        user.newPassword = "blah"

        postLoginRegister(user)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<div>Passwort muss mindestens 8 Zeichen lang sein</div>")))

        verify(exactly = 0) { manageProfileUseCase.register(any(), any()) }
    }

    @Test
    fun postLoginRegisterPasswordMissing() {
        val user = createValidUser()
        user.newPassword = null

        postLoginRegister(user)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<div>Eingabe erforderlich</div>")))
        verify(exactly = 0) { manageProfileUseCase.register(any(), any()) }
    }

    @Test
    fun postLoginRegisterEmptyName() {
        val user = createValidUser()
        user.name = ""

        postLoginRegister(user)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<div>Eingabe erforderlich</div>")))

        verify(exactly = 0) { manageProfileUseCase.register(any(), any()) }
    }

    @Test
    fun postLoginRegisterEmptyEmail() {
        val user = createValidUser()
        user.email = null

        postLoginRegister(user)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("<div>Eingabe erforderlich</div>")))
        verify(exactly = 0) { manageProfileUseCase.register(any(), any()) }
    }

    @Test
    fun postLoginRegisterConflict() {
        val user = createValidUser()
        every { manageProfileUseCase.register(any(), any()) } throws ProfileConflictException()

        postLoginRegister(user)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("Benutzername oder E-Mail sind bereits belegt!")))
    }

    @Test
    fun postLoginRegisterGlobalError() {
        val user = createValidUser()
        every { manageProfileUseCase.register(any(), any()) } throws RuntimeException()

        postLoginRegister(user)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(
                content()
                    .string(containsString("Fehler beim Anlegen des neuen Kontos. Besteht das Problem weiterhin, kontaktiere uns bitte unter info@railway-stations.org."))
            )
    }

    companion object {
        private fun createValidUser(): User {
            return UserTestFixtures.createUserNickname().copy(
                newPassword = "very_secret",
                locale = Locale.GERMAN,
            )
        }
    }
}