package org.railwaystations.rsapi.core.model

import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class UserTest {
    @ParameterizedTest
    @CsvSource(
        "nickname, email@example.com, true",
        "nickname, email@example., false",
        ", email@example.com, false",
        "'', email@example.com, false",
        "nickname, email.example.com, false",
        "nickname,, false",
        "nickname,' ', false"
    )
    fun isValidForRegistration(name: String?, email: String?, expected: Boolean) {
        assertThat(createUser(StringUtils.trimToEmpty(name), email, null, null).isValidForRegistration).isEqualTo(
            expected
        )
    }

    @ParameterizedTest
    @CsvSource(
        "nickname, email@example.com,                    , true",
        "nickname,                  ,                    , false",
        "nickname, email@example.com, http://example.com , true",
        "nickname, email@example.com, https://example.com, true",
        "nickname, email@example.com, ftp://example.com  , false",
        "nickname, email@example.com, email@example.com  , false",
        "nickname, email@example.com, '                 ', true",
        "        , email@example.com,                    , false"
    )
    fun testIsValid(name: String?, email: String?, link: String?, expected: Boolean) {
        assertThat(createUser(StringUtils.trimToEmpty(name), email, link, null).isValid).isEqualTo(expected)
    }

    private fun createUser(name: String, email: String?, url: String?, emailVerification: String?): User {
        return User(
            name = name,
            url = url,
            license = License.CC0_10,
            email = email,
            ownPhotos = true,
            emailVerification = emailVerification,
        )
    }

    @ParameterizedTest
    @CsvSource(
        "nickname, email@example.com, $EMAIL_VERIFIED              , true",
        "nickname,                  , $EMAIL_VERIFIED              , false",
        "nickname, email@example.com, $EMAIL_VERIFIED_AT_NEXT_LOGIN, false",
        "        , email@example.com, VERIFICATION_TOKEN                       , false"
    )
    fun isEligibleToReportProblem(name: String?, email: String?, emailVerification: String?, expected: Boolean) {
        assertThat(createUser(StringUtils.trimToEmpty(name), email, null, emailVerification).isEligibleToReportProblem)
            .isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,  $EMAIL_VERIFIED              , true",
        "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), false, $EMAIL_VERIFIED              , false",
        "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,  $EMAIL_VERIFIED_AT_NEXT_LOGIN, false",
        "nickname, email@example.com, CC4,                          true,  $EMAIL_VERIFIED              , false",
        "        , email@example.com, CC0 1.0 Universell (CC0 1.0), true,  VERIFICATION_TOKEN                       , false"
    )
    fun isEligibleToUploadPhoto(
        name: String?,
        email: String?,
        licenseDisplayName: String?,
        photoOwner: Boolean,
        emailVerification: String?,
        expected: Boolean
    ) {
        assertThat(
            User(
                name = StringUtils.trimToEmpty(name),
                license = License.ofDisplayName(licenseDisplayName),
                email = email,
                ownPhotos = photoOwner,
                emailVerification = emailVerification,
            ).isEligibleToUploadPhoto
        ).isEqualTo(expected)
    }

    @Test
    fun testRoleUser() {
        val user = createUser("@Nick Name", null, null, null)
        val roles = user.roles
        assertThat(roles.size).isEqualTo(1)
        assertThat(roles.contains(ROLE_USER)).isEqualTo(true)
        assertThat(roles.contains(ROLE_ADMIN)).isEqualTo(false)
    }

    @Test
    fun testRolesAdmin() {
        val admin = createUser("@Nick Name", null, null, null).copy(
            admin = true
        )
        val roles = admin.roles
        assertThat(roles.size).isEqualTo(2)
        assertThat(roles.contains(ROLE_USER)).isEqualTo(true)
        assertThat(roles.contains(ROLE_ADMIN)).isEqualTo(true)
    }

    @ParameterizedTest
    @CsvSource(
        "https://example.com/user10, false, https://example.com/user10",
        "https://example.com/user10, true, https://railway-stations.org",
        ", false, https://railway-stations.org"
    )
    fun testDisplayUrl(url: String?, anonymous: Boolean, expectedDisplayUrl: String?) {
        val user = createUser("user10", null, url, null).copy(
            anonymous = anonymous
        )
        assertThat(user.displayUrl).isEqualTo(expectedDisplayUrl)
    }

    @ParameterizedTest
    @CsvSource(
        "user10, false, user10", "user10, true, Anonym"
    )
    fun testDisplayName(name: String, anonymous: Boolean, expectedDisplayName: String?) {
        val user = createUser(name, null, null, null).copy(
            anonymous = anonymous
        )
        assertThat(user.displayName).isEqualTo(expectedDisplayName)
    }
}