package org.railwaystations.rsapi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StationTest {

    private static final Station.Key TEST_KEY = new Station.Key("", "0");
    public static final Coordinates TEST_COORDINATES = new Coordinates(50.554550, 9.683787);

    @Test
    void distanceTo() {
        var station = createStationTestFixtureBuilder()
                .coordinates(TEST_COORDINATES)
                .build();
        assertThat(station.distanceTo(50.196580, 9.189395)).isCloseTo(53.1, within(0.1));
    }

    @Test
    void appliesToNullPhotographer() {
        var station = createStationTestFixtureBuilder()
                .build();
        assertThat(station.appliesTo(null, "test", null)).isEqualTo(false);
        assertThat(station.appliesTo(false, null, null)).isEqualTo(true);
        assertThat(station.appliesTo(true, null, null)).isEqualTo(false);
    }

    @Test
    void appliesToPhotographer() {
        var station = createStationTestFixtureBuilder().build();
        station.getPhotos().add(createTestPhoto());
        assertThat(station.appliesTo(null, "test", null)).isEqualTo(true);
        assertThat(station.appliesTo(false, null, null)).isEqualTo(false);
        assertThat(station.appliesTo(true, null, null)).isEqualTo(true);
    }

    private Station.StationBuilder createStationTestFixtureBuilder() {
        return Station.builder()
                .key(TEST_KEY)
                .title("");
    }

    @Test
    void appliesToActive() {
        var station = createStationTestFixtureBuilder()
                .coordinates(TEST_COORDINATES).build();
        station.getPhotos().add(createTestPhoto());
        assertThat(station.appliesTo(null, "test", true)).isEqualTo(true);
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