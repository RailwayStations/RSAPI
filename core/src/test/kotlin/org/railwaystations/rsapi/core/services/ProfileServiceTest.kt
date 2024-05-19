package org.railwaystations.rsapi.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.core.config.MessageSourceConfig
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.UserTestFixtures.EXISTING_USER_ID
import org.railwaystations.rsapi.core.model.UserTestFixtures.USER_AGENT
import org.railwaystations.rsapi.core.model.UserTestFixtures.USER_EMAIL
import org.railwaystations.rsapi.core.model.UserTestFixtures.USER_NAME
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase.ProfileConflictException
import org.railwaystations.rsapi.core.ports.outbound.MailerPort
import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import org.railwaystations.rsapi.core.ports.outbound.OAuth2AuthorizationPort
import org.railwaystations.rsapi.core.ports.outbound.UserPort
import org.springframework.security.crypto.password.PasswordEncoder

internal class ProfileServiceTest {
    private val userPort = mockk<UserPort>()

    private val monitorPort = mockk<MonitorPort>()

    private val mailerPort = mockk<MailerPort>(relaxed = true)

    private val authorizationPort = mockk<OAuth2AuthorizationPort>(relaxed = true)

    private val sut = ProfileService(
        monitorPort = monitorPort,
        mailerPort = mailerPort,
        userPort = userPort,
        authorizationPort = authorizationPort,
        eMailVerificationUrl = "EMAIL_VERIFICATION_URL",
        passwordEncoder = mockk<PasswordEncoder>(relaxed = true),
        messageSource = MessageSourceConfig().messageSource()
    )

    @BeforeEach
    fun setup() {
        every { userPort.addUsernameToBlocklist(any()) } returns Unit
        every { userPort.anonymizeUser(any()) } returns Unit
        every { userPort.countBlockedUsername(any()) } returns 0
        every { userPort.findByEmail(any()) } returns null
        every { userPort.findByNormalizedName(any()) } returns null
        every { userPort.findByEmailVerification(any()) } returns null
        every { userPort.insert(any(), any(), any()) } returns 0
        every { userPort.update(any(), any()) } returns Unit
        every { userPort.updateCredentials(any(), any()) } returns Unit
        every { userPort.updateEmailVerification(any(), any()) } returns Unit
        every { monitorPort.sendMessage(any()) } returns Unit
    }

    @Test
    fun testRegisterInvalidData() {
        val newUser = User(
            name = "nickname",
            license = License.CC0_10,
            ownPhotos = true,
        )
        assertThatThrownBy { sut.register(newUser, USER_AGENT) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun registerNewUser() {
        val newUser = createNewUser()

        sut.register(newUser, USER_AGENT)

        verify { userPort.findByNormalizedName(USER_NAME) }
        verify { userPort.countBlockedUsername(USER_NAME) }
        verify { userPort.findByEmail(USER_EMAIL) }
        verify { userPort.insert(any(), any(), any()) }
        verify(exactly = 0) { userPort.updateCredentials(any(), any()) }
        verify {
            monitorPort.sendMessage(
                "New registration{nickname='%s', email='%s'}\nvia %s".format(
                    USER_NAME, USER_EMAIL, USER_AGENT
                )
            )
        }
        assertNewPasswordEmail()
    }


    @Test
    fun registerNewUserWithPassword() {
        val newUser = createNewUser().copy(
            newPassword = "verySecretPassword"
        )

        sut.register(newUser, USER_AGENT)

        verify { userPort.findByNormalizedName(USER_NAME) }
        verify { userPort.countBlockedUsername(USER_NAME) }
        verify { userPort.findByEmail(USER_EMAIL) }
        verify { userPort.insert(any(), any(), any()) }
        verify(exactly = 0) { userPort.updateCredentials(any(), any()) }
        verify {
            monitorPort.sendMessage(
                "New registration{nickname='%s', email='%s'}\nvia %s".format(
                    USER_NAME, USER_EMAIL, USER_AGENT
                )
            )
        }
        assertVerificationEmail(mailerPort)
    }

    @Test
    fun updateMyProfileNewMail() {
        every { userPort.findByEmail("newname@example.com") } returns null
        val existingUser = givenExistingUser()
        val updatedUser = createNewUser().copy(
            id = existingUser.id,
            email = "newname@example.com"
        )

        sut.updateProfile(existingUser, updatedUser, USER_AGENT)

        assertVerificationEmail(mailerPort)
        val user = givenExistingUser().copy(
            email = "newname@example.com"
        )
        verify { userPort.update(user.id, user) }
    }

    @Test
    fun changePasswordTooShortBody() {
        val user = givenExistingUser()

        assertThatThrownBy { sut.changePassword(user, "secret") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun changePasswordBody() {
        val user = givenExistingUser()

        sut.changePassword(user, "secretlong")

        verify { userPort.updateCredentials(eq(user.id), any()) }
        verify { authorizationPort.deleteAllByUser(user.name) }
    }

    @Test
    fun verifyEmailSuccess() {
        val token = "verification"
        val user = createNewUser().copy(
            id = EXISTING_USER_ID,
            emailVerification = token
        )
        every { userPort.findByEmailVerification(token) } returns user

        sut.emailVerification(token)

        verify {
            monitorPort.sendMessage(
                "Email verified {nickname='${USER_NAME}', email='${USER_EMAIL}'}"
            )
        }
        verify { userPort.updateEmailVerification(EXISTING_USER_ID, User.EMAIL_VERIFIED) }
    }

    @Test
    fun verifyEmailFailed() {
        val token = "verification"
        val user = createNewUser().copy(
            id = EXISTING_USER_ID,
            emailVerification = token
        )
        every { userPort.findByEmailVerification(token) } returns user

        sut.emailVerification("wrong_token")

        verify(exactly = 0) { monitorPort.sendMessage(any()) }
        verify(exactly = 0) {
            userPort.updateEmailVerification(
                EXISTING_USER_ID,
                User.EMAIL_VERIFIED
            )
        }
    }

    @Test
    fun resendEmailVerification() {
        val user = givenExistingUser()

        sut.resendEmailVerification(user)

        assertVerificationEmail(mailerPort)
        verify { userPort.updateEmailVerification(eq(EXISTING_USER_ID), any()) }
    }

    @Test
    fun deleteMyProfile() {
        val user = givenExistingUser()

        sut.deleteProfile(user, USER_AGENT)

        verify { userPort.anonymizeUser(EXISTING_USER_ID) }
        verify { userPort.addUsernameToBlocklist(USER_NAME) }
        verify { authorizationPort.deleteAllByUser(USER_NAME) }
    }

    @Test
    fun registerUserNameTaken() {
        val user = givenExistingUser()
        val newUser = createNewUser().copy(
            name = user.name
        )

        assertThatThrownBy { sut.register(newUser, USER_AGENT) }
            .isInstanceOf(ProfileConflictException::class.java)
    }

    @Test
    fun registerUserNameBlocked() {
        val newUser = createNewUser().copy(
            name = "Blocked Name"
        )
        every { userPort.countBlockedUsername("blockedname") } returns 1

        assertThatThrownBy { sut.register(newUser, USER_AGENT) }
            .isInstanceOf(ProfileConflictException::class.java)
    }

    @Test
    fun registerUserEmailTaken() {
        val user = givenExistingUser()
        val newUser = createNewUser().copy(
            name = "othername",
            email = user.email
        )

        assertThatThrownBy { sut.register(newUser, USER_AGENT) }
            .isInstanceOf(ProfileConflictException::class.java)

        verify { monitorPort.sendMessage("Registration for user 'othername' with eMail 'existing@example.com' failed, eMail is already taken\nvia UserAgent") }
    }

    @Test
    fun registernUserNameTaken() {
        val user = givenExistingUser()
        val newUser = createNewUser().copy(
            name = user.name,
            email = "otheremail@example.com"
        )

        assertThatThrownBy { sut.register(newUser, USER_AGENT) }
            .isInstanceOf(ProfileConflictException::class.java)

        verify {
            monitorPort.sendMessage(
                "Registration for user '%s' with eMail '%s' failed, name is already taken by different eMail '%s'%nvia %s"
                    .format(user.name, newUser.email, user.email, USER_AGENT)
            )
        }
    }

    @Test
    fun registerUserWithEmptyName() {
        val newUser = createNewUser().copy(
            name = ""
        )

        assertThatThrownBy { sut.register(newUser, USER_AGENT) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun assertNewPasswordEmail() {
        verify(exactly = 1) {
            mailerPort.send(
                any(),
                any(), match {
                    it.matches(
                        """
                        Hello,
                        
                        your new password is: .*
                        
                        Cheers
                        Your Railway-Stations-Team
                        """.trimIndent().toRegex()
                    )
                })
        }
    }

    private fun createNewUser(): User {
        val key =
            "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124426D4F637165757646794E44754132726B566A6A3177246A7568362F6E6C2F49437A4B475570446E6B674171754A304F7A486A62694F587442542F2B62584D49476300000000000000000000000000000000000000000000000000000000000000"
        return User(
            name = USER_NAME,
            url = "https://link@example.com",
            license = License.CC0_10,
            email = USER_EMAIL,
            ownPhotos = true,
            key = key,
        )
    }

    private fun givenExistingUser(): User {
        val user = createNewUser().copy(
            id = EXISTING_USER_ID
        )
        every { userPort.findByEmail(user.email!!) } returns user
        every { userPort.findByNormalizedName(user.name) } returns user
        return user
    }

    @Test
    fun resetPasswordUnknownUser() {
        assertThatThrownBy { sut.resetPassword("unknown_user", USER_AGENT) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun resetPasswordEmptyEmail() {
        every { userPort.findByNormalizedName(USER_NAME) } returns createNewUser().copy(email = null)

        assertThatThrownBy { sut.resetPassword(USER_NAME, USER_AGENT) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun resetPasswordViaUsernameEmailNotVerified() {
        val user = createNewUser().copy(
            id = 123
        )
        every { userPort.findByNormalizedName(USER_NAME) } returns user

        sut.resetPassword(USER_NAME, USER_AGENT)

        verify { userPort.updateCredentials(eq(123), any()) }
        verify {
            monitorPort.sendMessage(
                "Reset Password for '%s', email='%s'".format(
                    USER_NAME, USER_EMAIL
                )
            )
        }
        assertNewPasswordEmail()
        verify { userPort.updateEmailVerification(123, User.EMAIL_VERIFIED_AT_NEXT_LOGIN) }
        verify { authorizationPort.deleteAllByUser(USER_NAME) }
    }

    @Test
    fun resetPasswordViaEmailAndEmailVerified() {
        val user = createNewUser().copy(
            id = 123,
            emailVerification = User.EMAIL_VERIFIED

        )
        every { userPort.findByEmail(user.email!!) } returns user

        sut.resetPassword(user.email!!, USER_AGENT)

        verify { userPort.updateCredentials(eq(123), any()) }
        verify {
            monitorPort.sendMessage(
                "Reset Password for '%s', email='%s'".format(
                    USER_NAME, USER_EMAIL
                )
            )
        }
        assertNewPasswordEmail()
        verify(exactly = 0) { userPort.updateEmailVerification(123, User.EMAIL_VERIFIED_AT_NEXT_LOGIN) }
        verify { authorizationPort.deleteAllByUser(user.name) }
    }

    fun assertVerificationEmail(mailerPort: MailerPort) {
        verify(exactly = 0) {
            mailerPort.send(
                any(),
                any(), match {
                    it.matches(
                        """
                                Hello,

                                please click on EMAIL_VERIFICATION_URL.* to verify your eMail-Address.

                                Cheers
                                Your Railway-Stations-Team

                                ---
                                Hallo,

                                bitte klicke auf EMAIL_VERIFICATION_URL.*, um Deine eMail-Adresse zu verifizieren.

                                Viele Grüße
                                Dein Bahnhofsfoto-Team
                                """.trimIndent().toRegex()
                    )
                }
            )
        }
    }
}