package org.railwaystations.rsapi.core.model

import org.apache.commons.lang3.StringUtils
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import java.net.URI
import java.net.URISyntaxException
import java.util.*

data class User(
    var id: Int = 0,
    var name: String,
    var url: String? = null,
    var license: License = License.UNKNOWN,
    var email: String? = null,
    var ownPhotos: Boolean = false,
    var anonymous: Boolean = false,
    var key: String? = null,
    var admin: Boolean = false,
    var emailVerification: String? = null,
    var newPassword: String? = null,
    var sendNotifications: Boolean = false,
    var locale: Locale = Locale.ENGLISH,
) {
    val normalizedName: String
        get() = normalizeName(name)

    val displayUrl: String
        get() = if (anonymous || StringUtils.isBlank(url)) "https://railway-stations.org" else url!!

    val displayName: String
        get() = if (anonymous) ANONYM else name

    val isValidForRegistration: Boolean
        /**
         * Checks if we have got a name and valid email for registration.
         */
        get() = StringUtils.isNotBlank(name) &&
                StringUtils.isNotBlank(email) &&
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

    companion object {
        const val EMAIL_VERIFIED: String = "VERIFIED"
        const val EMAIL_VERIFIED_AT_NEXT_LOGIN: String = "NEXT_LOGIN"
        const val ROLE_ADMIN: String = "ROLE_ADMIN"
        const val ROLE_USER: String = "ROLE_USER"
        const val ANONYM: String = "Anonym"

        @JvmStatic
        fun normalizeName(name: String?): String {
            return StringUtils.trimToEmpty(name).lowercase().replace("[^a-z\\d]".toRegex(), "")
        }

        @JvmStatic
        fun normalizeEmail(email: String): String {
            val trimmedEmail = StringUtils.trimToNull(email)
            return trimmedEmail.lowercase()
        }

        @JvmStatic
        fun createNewEmailVerificationToken(): String {
            return UUID.randomUUID().toString()
        }
    }
}
