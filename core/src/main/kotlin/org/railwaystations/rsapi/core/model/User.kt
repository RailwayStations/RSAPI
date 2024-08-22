package org.railwaystations.rsapi.core.model

import org.apache.commons.lang3.StringUtils
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import java.net.URI
import java.net.URISyntaxException
import java.util.*

const val EMAIL_VERIFIED: String = "VERIFIED"
const val EMAIL_VERIFIED_AT_NEXT_LOGIN: String = "NEXT_LOGIN"
const val ROLE_ADMIN: String = "ROLE_ADMIN"
const val ROLE_USER: String = "ROLE_USER"
const val ANONYM: String = "Anonym"

data class User(
    val id: Int = 0,
    val name: String,
    val url: String? = null,
    val license: License = License.UNKNOWN,
    val email: String? = null,
    val ownPhotos: Boolean = false,
    val anonymous: Boolean = false,
    val key: String? = null,
    val admin: Boolean = false,
    val emailVerification: String? = null,
    val newPassword: String? = null,
    val sendNotifications: Boolean = false,
    val locale: Locale = Locale.ENGLISH,
) {

    val displayUrl: String
        get() = if (anonymous || url.isNullOrBlank()) "https://railway-stations.org" else url

    val displayName: String
        get() = if (anonymous) ANONYM else name

    val isValidForRegistration: Boolean
        /**
         * Checks if we have got a name and valid email for registration.
         */
        get() = name.isNotBlank() &&
                !email.isNullOrBlank() &&
                EmailValidator().isValid(email, null)

    val isValid: Boolean
        get() {
            if (!isValidForRegistration) {
                return false
            }
            StringUtils.trimToNull(url)?.let {
                val validatedUri: URI
                try {
                    validatedUri = URI(it)
                } catch (e: URISyntaxException) {
                    return false
                }
                return validatedUri.scheme != null && validatedUri.scheme.matches("https?".toRegex())
            }

            return true
        }

    val isEligibleToUploadPhoto: Boolean
        get() = isValid && isEmailVerified && ownPhotos && License.CC0_10 == license

    val isEligibleToReportProblem: Boolean
        get() = isEmailVerified && isValid

    val isEmailVerified: Boolean
        get() = EMAIL_VERIFIED == emailVerification

    val isEmailVerifiedWithNextLogin: Boolean
        get() = EMAIL_VERIFIED_AT_NEXT_LOGIN == emailVerification

    val roles: Set<String>
        get() {
            val roles = HashSet<String>()
            roles.add(ROLE_USER)
            if (admin) {
                roles.add(ROLE_ADMIN)
            }
            return roles
        }

    val localeLanguageTag: String
        get() = locale.toLanguageTag()

}

fun normalizeEmail(email: String): String {
    val trimmedEmail = StringUtils.trimToNull(email)
    return trimmedEmail.lowercase()
}

fun createNewEmailVerificationToken(): String {
    return UUID.randomUUID().toString()
}