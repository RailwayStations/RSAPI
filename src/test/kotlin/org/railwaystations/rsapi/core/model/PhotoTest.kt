package org.railwaystations.rsapi.core.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant

private val TEST_KEY = Station.Key("", "0")

internal class PhotoTest {
    /**
     * Tests if the mapping of the known license names to the license URLs works as expected.
     */
    @ParameterizedTest
    @EnumSource(License::class)
    fun license2LicenseUrlMapping(license: License) {
        val photo = Photo(
            id = 0,
            stationKey = TEST_KEY,
            primary = true,
            urlPath = "url",
            photographer = createTestPhotographer(license),
            createdAt = Instant.now(),
            license = license,
            outdated = false
        )
        assertThat(license).isEqualTo(photo.license)
    }

    private fun createTestPhotographer(license: License): User {
        return User(
            name = "photographer",
            url = "photographerUrl",
            license = license,
            ownPhotos = true,
        )
    }

}