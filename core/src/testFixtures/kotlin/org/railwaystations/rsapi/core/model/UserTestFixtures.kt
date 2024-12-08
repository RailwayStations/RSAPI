package org.railwaystations.rsapi.core.model

import java.util.*

object UserTestFixtures {

    const val USER_AGENT = "UserAgent"
    const val USER_NAME = "existing"
    const val USER_EMAIL = "existing@example.com"
    const val EXISTING_USER_ID = 42L

    val user10 = User(
        id = 11,
        name = "@user10",
        email = "user10@example.com",
        url = "https://www.example.com/user10",
        ownPhotos = true,
        anonymous = false,
        license = License.CC0_10,
        key = "246172676F6E32696424763D3139246D3D36353533362C743D322C703D312432634C6B4C6949415958584E52742B6C7A3062614541242B706C7463365A4371386D534551772B4139374B304E37544B386A7072582F774141614B525933456D783400000000000000000000000000000000000000000000000000000000000000",
        admin = true,
        emailVerification = "VERIFIED",
        sendNotifications = true,
        locale = Locale.ENGLISH
    )

    val userJimKnopf = User(
        id = 0,
        name = "Jim Knopf",
        url = "photographerUrl",
        email = "jim.knopf@example.com",
        license = License.CC0_10,
        ownPhotos = true,
    )

    val someUser = User(
        id = 1,
        name = "someuser",
        license = License.CC0_10,
        email = "someuser@example.com",
        ownPhotos = true,
        emailVerification = EMAIL_VERIFIED,
    )

    val userNickname = User(
        id = 42,
        name = "nickname",
        license = License.CC0_10,
        email = "nickname@example.com",
        ownPhotos = true,
        anonymous = true,
        emailVerification = EMAIL_VERIFIED,
    )

}