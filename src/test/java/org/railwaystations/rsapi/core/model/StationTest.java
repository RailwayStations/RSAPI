package org.railwaystations.rsapi.core.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StationTest {

    private static final Station.Key TEST_KEY = new Station.Key("", "0");

    @ParameterizedTest
    @CsvSource({
            "test,    false",
            ",        true"
    })
    void appliesToNullPhotographer(String photographer, boolean expectedAppliesTo) {
        var station = createStationTestFixtureBuilder()
                .build();
        assertThat(station.appliesTo(photographer)).isEqualTo(expectedAppliesTo);
    }

    @ParameterizedTest
    @CsvSource({
            "test,    true",
            ",        true"
    })
    void appliesToPhotographer(String photographer, boolean expectedAppliesTo) {
        var station = createStationTestFixtureBuilder().build();
        station.getPhotos().add(createTestPhoto());
        assertThat(station.appliesTo(photographer)).isEqualTo(expectedAppliesTo);
    }

    private Station.StationBuilder createStationTestFixtureBuilder() {
        return Station.builder()
                .key(TEST_KEY)
                .title("");
    }

    private Photo createTestPhoto() {
        return Photo.builder()
                .stationKey(TEST_KEY)
                .urlPath("URL")
                .photographer(User.builder()
                        .id(0)
                        .name("test")
                        .url("photographerUrl")
                        .license(License.CC0_10)
                        .ownPhotos(true)
                        .sendNotifications(true)
                        .build())
                .license(License.CC0_10)
                .build();
    }

}