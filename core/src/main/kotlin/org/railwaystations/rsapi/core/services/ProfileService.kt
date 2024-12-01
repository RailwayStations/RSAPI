package org.railwaystations.rsapi.core.services

import org.railwaystations.rsapi.core.model.*
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase
import org.railwaystations.rsapi.core.ports.inbound.ManageProfileUseCase.ProfileConflictException
import org.railwaystations.rsapi.core.ports.outbound.MailerPort
import org.railwaystations.rsapi.core.ports.outbound.MonitorPort
import org.railwaystations.rsapi.core.ports.outbound.OAuth2AuthorizationPort
import org.railwaystations.rsapi.core.ports.outbound.UserPort
import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class ProfileService(
    private val monitorPort: MonitorPort,
    private val mailerPort: MailerPort,
    private val userPort: UserPort,
    private val authorizationPort: OAuth2AuthorizationPort,
    @param:Value(
        "\${mailVerificationUrl}"
    ) private val eMailVerificationUrl: String,
    private val passwordEncoder: PasswordEncoder,
    private val messageSource: MessageSource
) : ManageProfileUseCase {

    private val log by Logger()

    override fun changePassword(user: User, newPassword: String) {
        log.info("Password change for '{}'", user.email)
        val trimmedPassword = newPassword.trim()
        require(trimmedPassword.length >= 8) { "Password too short" }
        userPort.updateCredentials(user.id, passwordEncoder.encode(trimmedPassword))
        authorizationPort.deleteAllByUser(user.name)
    }

    override fun resetPassword(nameOrEmail: String, clientInfo: String?) {
        log.info("Password reset requested for '{}'", nameOrEmail)
        val user = userPort.findByEmail(normalizeEmail(nameOrEmail))
            ?: userPort.findByName(nameOrEmail)

        requireNotNull(user) { "Can't reset password for unknown user" }

        if (user.email.isNullOrBlank()) {
            monitorPort.sendMessage("Can't reset password for '$nameOrEmail' failed: no email available\nvia $clientInfo")
            throw IllegalArgumentException("Email '${user.email}' is empty")
        }

        val newPassword = createNewPassword()
        val key = encryptPassword(newPassword)
        userPort.updateCredentials(user.id, key)
        monitorPort.sendMessage("Reset Password for '${user.name}', email='${user.email}'")

        sendPasswordMail(user.email, newPassword, user.locale)
        if (!user.isEmailVerified) {
            // if the email is not yet verified, we can verify it with the next login
            userPort.updateEmailVerification(user.id, EMAIL_VERIFIED_AT_NEXT_LOGIN)
        }
        authorizationPort.deleteAllByUser(user.name)
    }

    override fun register(newUser: User, clientInfo: String?) {
        log.info("New registration for '{}' with '{}'", newUser.name, newUser.email)

        require(newUser.isValid) { "Invalid data" }

        val existingName = userPort.findByName(newUser.name)
        existingName?.let {
            if (newUser.email != it.email) {
                monitorPort.sendMessage(
                    "Registration for user '${newUser.name}' with eMail '${newUser.email}' failed, name is already taken by different eMail '${it.email}'\nvia $clientInfo"
                )
                throw ProfileConflictException()
            }
        }

        if (userPort.countBlockedUsername(newUser.name) != 0) {
            monitorPort.sendMessage(
                "Registration for user '${newUser.name}' with eMail '${newUser.email}' failed, name is blocked\nvia $clientInfo"
            )
            throw ProfileConflictException()
        }

        userPort.findByEmail(newUser.email!!)?.let {
            monitorPort.sendMessage(
                "Registration for user '${newUser.name}' with eMail '${newUser.email}' failed, eMail is already taken\nvia $clientInfo"
            )
            throw ProfileConflictException()
        }

        var password = newUser.newPassword
        var emailVerificationToken = createNewEmailVerificationToken()
        val passwordProvided = !password.isNullOrBlank()
        if (!passwordProvided) {
            password = createNewPassword()
            emailVerificationToken = EMAIL_VERIFIED_AT_NEXT_LOGIN
        }

        val key = encryptPassword(password)
        saveRegistration(newUser, key, emailVerificationToken)

        if (passwordProvided) {
            sendEmailVerification(newUser.email, emailVerificationToken)
        } else {
            sendPasswordMail(newUser.email, password, newUser.locale)
        }

        monitorPort.sendMessage(
            """
                New registration{nickname='${newUser.name}', email='${newUser.email}'}
                via $clientInfo
            """.trimIndent()
        )
    }

    private fun encryptPassword(password: String?): String {
        return passwordEncoder.encode(password)
    }

    override fun updateProfile(user: User, newProfile: User, clientInfo: String?) {
        log.info("Update profile for '{}'", user.email)

        if (!newProfile.isValid) {
            log.info("Update Profile failed: User invalid {}", newProfile)
            throw IllegalArgumentException()
        }

        if (newProfile.name != user.name) {
            userPort.findByName(newProfile.name)?.let {
                log.info("Name conflict '{}'", newProfile.name)
                throw ProfileConflictException()
            }
            monitorPort.sendMessage("Update nickname for user '${user.name}' to '${newProfile.name}'\nvia $clientInfo")
        }

        if (newProfile.email != user.email) {
            userPort.findByEmail(newProfile.email!!)?.let {
                log.info("Email conflict '{}'", newProfile.email)
                throw ProfileConflictException()
            }
            monitorPort.sendMessage(
                "Update email for user '${user.name}' from email '${user.email}' to '${newProfile.email}'\nvia $clientInfo"
            )
            val emailVerificationToken = createNewEmailVerificationToken()
            sendEmailVerification(newProfile.email, emailVerificationToken)
            userPort.updateEmailVerification(user.id, emailVerificationToken)
        }

        userPort.update(user.id, newProfile)
    }

    override fun resendEmailVerification(user: User) {
        log.info("Resend EmailVerification for '{}'", user.email)
        val emailVerificationToken = createNewEmailVerificationToken()
        userPort.updateEmailVerification(user.id, emailVerificationToken)
        sendEmailVerification(user.email!!, emailVerificationToken)
    }

    override fun emailVerification(token: String): User? {
        return userPort.findByEmailVerification(token)
            ?.let { user: User ->
                userPort.updateEmailVerification(user.id, EMAIL_VERIFIED)
                monitorPort.sendMessage("Email verified {nickname='${user.name}', email='${user.email}'}")
                user
            }
    }

    override fun deleteProfile(user: User, userAgent: String?) {
        userPort.anonymizeUser(user.id)
        userPort.addUsernameToBlocklist(user.name)
        authorizationPort.deleteAllByUser(user.name)
        monitorPort.sendMessage("Closed account ${user.id} - ${user.name}")
    }

    override fun updateLocale(user: User, locale: Locale) {
        userPort.updateLocale(user.id, locale.toLanguageTag())
    }

    private fun sendPasswordMail(email: String, newPassword: String, locale: Locale) {
        val text = messageSource.getMessage("password_mail", arrayOf(newPassword), locale)
        mailerPort.send(email, "Railway-Stations.org new password", text)
        log.info("Password sent to {}", email)
    }

    private fun sendEmailVerification(email: String, emailVerificationToken: String) {
        val url = eMailVerificationUrl + emailVerificationToken
        val text = """
                Hello,
                                        
                please click on $url to verify your eMail-Address.
                                        
                Cheers
                Your Railway-Stations-Team
                                        
                ---
                Hallo,
                                        
                bitte klicke auf $url, um Deine eMail-Adresse zu verifizieren.
                                        
                Viele Grüße
                Dein Bahnhofsfoto-Team
                """.trimIndent()
        mailerPort.send(email, "Railway-Stations.org eMail verification", text)
        log.info("Email verification sent to {}", email)
    }

    private fun saveRegistration(registration: User, key: String, emailVerification: String) {
        val id = userPort.insert(registration, key, emailVerification)
        log.info("User '{}' created with id {}", registration.name, id)
    }
}

private val possibleCharacters =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?".toCharArray()

private fun createNewPassword() = List(20) { possibleCharacters.random() }.joinToString("")
