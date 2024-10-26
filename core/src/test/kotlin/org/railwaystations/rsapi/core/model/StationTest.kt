package org.railwaystations.rsapi.core.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

private val TEST_KEY = Station.Key("", "0")

internal class StationTest {
    @ParameterizedTest
    @CsvSource(
        "test,    false", ",        true"
    )
    fun appliesToNullPhotographer(photographer: String?, expectedAppliesTo: Boolean) {
        val station = createValidStation()
        assertThat(station.appliesTo(photographer)).isEqualTo(expectedAppliesTo)
    }

    @ParameterizedTest
    @CsvSource(
        "test,    true", ",        true"
    )
    fun appliesToPhotographer(photographer: String?, expectedAppliesTo: Boolean) {
        val station = createValidStation().copy(
            photos = listOf(createTestPhoto())
        )
        assertThat(station.appliesTo(photographer)).isEqualTo(expectedAppliesTo)
    }

    private fun createValidStation(): Station {
        return Station(
            key = TEST_KEY,
            title = "",
        )
    }

    private fun createTestPhoto(): Photo {
        return Photo(
            stationKey = TEST_KEY,
            primary = true,
            urlPath = "URL",
            photographer = User(
                name = "test",
                url = "photographerUrl",
                license = License.CC0_10,
                ownPhotos = true,
            ),
            createdAt = Instant.now(),
            license = License.CC0_10,
        )
    }

}