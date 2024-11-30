package org.railwaystations.rsapi.app.auth

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.web.auth.LazySodiumPasswordEncoder

internal class LazySodiumPasswordEncoderTest {
    private val encoder = LazySodiumPasswordEncoder()

    @Test
    fun bashAndVerifyPasswordRandom() {
        val password = RandomStringUtils.randomAlphanumeric(12)
        val key = encoder.encode(password)
        Assertions.assertThat(encoder.matches(password, key)).isEqualTo(true)
    }

    @Test
    fun hashAndVerifyPasswordFixed() {
        val password = "secret"
        val key = encoder.encode(password)
        println(key)
        Assertions.assertThat(encoder.matches(password, key)).isEqualTo(true)
    }

    @Test
    fun hashAndVerifyChangedPassword() {
        val password = RandomStringUtils.randomAlphanumeric(12)
        val key = encoder.encode(password)
        Assertions.assertThat(encoder.matches("something else", key)).isEqualTo(false)
    }

    @Test
    fun verifyExistingPassword() {
        val key =
            "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124564A50666459365174574B786B6361745A2B37443241246D71324959726138695A564A6D5A2F2B53777A376868672B7659744341484861667A796A7469664A70426300000000000000000000000000000000000000000000000000000000000000"
        Assertions.assertThat(encoder.matches("y89zFqkL6hro", key)).isEqualTo(true)
    }
}
