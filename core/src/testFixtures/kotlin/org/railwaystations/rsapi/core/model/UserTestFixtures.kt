package org.railwaystations.rsapi.core.model

object UserTestFixtures {

    const val USER_AGENT = "UserAgent"
    const val USER_NAME = "existing"
    const val USER_EMAIL = "existing@example.com"
    const val EXISTING_USER_ID = 42L

    fun createUserJimKnopf(): User {
        return User(
            id = 0,
            name = "Jim Knopf",
            url = "photographerUrl",
            email = "jim.knopf@example.com",
            license = License.CC0_10,
            ownPhotos = true,
        )
    }

    fun createUserNickname(): User {
        return User(
            id = 42,
            name = "nickname",
            license = License.CC0_10,
            email = "nickname@example.com",
            ownPhotos = true,
            anonymous = true,
            emailVerification = EMAIL_VERIFIED,
        )
    }

    fun createSomeUser(): User {
        return User(
            id = 1,
            name = "someuser",
            license = License.CC0_10,
            email = "someuser@example.com",
            ownPhotos = true,
            emailVerification = EMAIL_VERIFIED,
        )
    }
}