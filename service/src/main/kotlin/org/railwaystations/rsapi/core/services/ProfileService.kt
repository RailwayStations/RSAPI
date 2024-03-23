package org.railwaystations.rsapi.core.services

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.adapter.db.OAuth2AuthorizationDao
import org.railwaystations.rsapi.adapter.db.UserDao
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.model.User.Companion.createNewEmailVerificationToken
import org.railwaystations.rsapi.core.model.User.Companion.normalizeEmail
import org.railwaystations.rsapi.core.model.User.Companion.normalizeName
import org.railwaystations.rsapi.core.ports.Mailer
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase
import org.railwaystations.rsapi.core.ports.ManageProfileUseCase.ProfileConflictException
import org.railwaystations.rsapi.core.ports.Monitor
import org.railwaystations.rsapi.utils.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*

@Service
class ProfileService(
    private val monitor: Monitor,
    private val mailer: Mailer,
    private val userDao: UserDao,
    private val authorizationDao: OAuth2AuthorizationDao,
    @param:Value(
        "\${mailVerificationUrl}"
    ) private val eMailVerificationUrl: String,
    private val passwordEncoder: PasswordEncoder,
    private val messageSource: MessageSource
) : ManageProfileUseCase {

    private val log by Logger()

    override fun changePassword(user: User, newPassword: String) {
        log.info("Password change for '{}'", user.email)
        val trimmedPassword = StringUtils.trimToEmpty(newPassword)
        require(trimmedPassword.length >= 8) { "Password too short" }
        userDao.updateCredentials(user.id, passwordEncoder.encode(trimmedPassword))
        authorizationDao.deleteAllByUser(user.name)
    }

    override fun resetPassword(nameOrEmail: String, clientInfo: String?) {
        log.info("Password reset requested for '{}'", nameOrEmail)
        val user = userDao.findByEmail(normalizeEmail(nameOrEmail))
            ?: userDao.findByNormalizedName(normalizeName(nameOrEmail))

        requireNotNull(user) { "Can't reset password for unknown user" }

        if (user.email.isNullOrBlank()) {
            monitor.sendMessage("Can't reset password for '$nameOrEmail' failed: no email available\nvia $clientInfo")
            throw IllegalArgumentException("Email '${user.email}' is empty")
        }

        val newPassword = createNewPassword()
        val key = encryptPassword(newPassword)
        userDao.updateCredentials(user.id, key)
        monitor.sendMessage("Reset Password for '${user.name}', email='${user.email}'")

        sendPasswordMail(user.email, newPassword, user.locale)
        if (!user.isEmailVerified) {
            // if the email is not yet verified, we can verify it with the next login
            userDao.updateEmailVerification(user.id, User.EMAIL_VERIFIED_AT_NEXT_LOGIN)
        }
        authorizationDao.deleteAllByUser(user.name)
    }

    @Throws(ProfileConflictException::class)
    override fun register(newUser: User, clientInfo: String?) {
        log.info("New registration for '{}' with '{}'", newUser.name, newUser.email)

        require(newUser.isValidForRegistration) { "Invalid data" }

        val existingName = userDao.findByNormalizedName(
            newUser.normalizedName
        )
        existingName?.let {
            if (newUser.email != it.email) {
                monitor.sendMessage(
                    "Registration for user '${newUser.name}' with eMail '${newUser.email}' failed, name is already taken by different eMail '${it.email}'\nvia $clientInfo"
                )
                throw ProfileConflictException()
            }
        }

        if (userDao.countBlockedUsername(newUser.normalizedName) != 0) {
            monitor.sendMessage(
                "Registration for user '${newUser.name}' with eMail '${newUser.email}' failed, name is blocked\nvia $clientInfo"
            )
            throw ProfileConflictException()
        }

        userDao.findByEmail(newUser.email!!)?.let {
            monitor.sendMessage(
                "Registration for user '${newUser.name}' with eMail '${newUser.email}' failed, eMail is already taken\nvia $clientInfo"
            )
            throw ProfileConflictException()
        }

        var password = newUser.newPassword
        var emailVerificationToken = createNewEmailVerificationToken()
        val passwordProvided = !password.isNullOrBlank()
        if (!passwordProvided) {
            password = createNewPassword()
            emailVerificationToken = User.EMAIL_VERIFIED_AT_NEXT_LOGIN
        }

        val key = encryptPassword(password)
        saveRegistration(newUser, key, emailVerificationToken)

        if (passwordProvided) {
            sendEmailVerification(newUser.email, emailVerificationToken)
        } else {
            sendPasswordMail(newUser.email, password!!, newUser.locale)
        }

        monitor.sendMessage("New registration{nickname='${newUser.name}', email='${newUser.email}'}\nvia $clientInfo")
    }

    private fun createNewPassword(): String {
        val possibleCharacters =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?".toCharArray()
        return RandomStringUtils.random(
            16,
            0,
            possibleCharacters.size - 1,
            false,
            false,
            possibleCharacters,
            SecureRandom()
        )
    }

    private fun encryptPassword(password: String?): String {
        return passwordEncoder.encode(password)
    }

    @Throws(ProfileConflictException::class)
    override fun updateProfile(user: User, newProfile: User, clientInfo: String?) {
        log.info("Update profile for '{}'", user.email)

        if (!newProfile.isValid) {
            log.info("Update Profile failed: User invalid {}", newProfile)
            throw IllegalArgumentException()
        }

        if (newProfile.normalizedName != user.normalizedName) {
            userDao.findByNormalizedName(newProfile.normalizedName)?.let {
                log.info("Name conflict '{}'", newProfile.name)
                throw ProfileConflictException()
            }
            monitor.sendMessage("Update nickname for user '${user.name}' to '${newProfile.name}'\nvia $clientInfo")
        }

        if (newProfile.email != user.email) {
            userDao.findByEmail(newProfile.email!!)?.let {
                log.info("Email conflict '{}'", newProfile.email)
                throw ProfileConflictException()
            }
            monitor.sendMessage(
                "Update email for user '${user.name}' from email '${user.email}' to '${newProfile.email}'\nvia $clientInfo"
            )
            val emailVerificationToken = createNewEmailVerificationToken()
            sendEmailVerification(newProfile.email, emailVerificationToken)
            userDao.updateEmailVerification(user.id, emailVerificationToken)
        }

        userDao.update(user.id, newProfile)
    }

    override fun resendEmailVerification(user: User) {
        log.info("Resend EmailVerification for '{}'", user.email)
        val emailVerificationToken = createNewEmailVerificationToken()
        userDao.updateEmailVerification(user.id, emailVerificationToken)
        sendEmailVerification(user.email!!, emailVerificationToken)
    }

    override fun emailVerification(token: String): User? {
        return userDao.findByEmailVerification(token)
            ?.let { user: User ->
                userDao.updateEmailVerification(user.id, User.EMAIL_VERIFIED)
                monitor.sendMessage("Email verified {nickname='${user.name}', email='${user.email}'}")
                user
            }
    }

    override fun deleteProfile(user: User, userAgent: String?) {
        val normalizedName = user.normalizedName
        userDao.anonymizeUser(user.id)
        userDao.addUsernameToBlocklist(normalizedName)
        authorizationDao.deleteAllByUser(user.name)
        monitor.sendMessage("Closed account ${user.id} - ${user.name}")
    }

    override fun updateLocale(user: User, locale: Locale) {
        userDao.updateLocale(user.id, locale.toLanguageTag())
    }

    private fun sendPasswordMail(email: String, newPassword: String, locale: Locale) {
        val text = messageSource.getMessage("password_mail", arrayOf(newPassword), locale)
        mailer.send(email, "Railway-Stations.org new password", text)
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
        mailer.send(email, "Railway-Stations.org eMail verification", text)
        log.info("Email verification sent to {}", email)
    }

    private fun saveRegistration(registration: User, key: String, emailVerification: String) {
        val id = userDao.insert(registration, key, emailVerification)
        log.info("User '{}' created with id {}", registration.name, id)
    }
}
