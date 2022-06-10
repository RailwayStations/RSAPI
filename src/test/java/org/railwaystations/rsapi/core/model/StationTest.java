package org.railwaystations.rsapi.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StationTest {

    private static final Station.Key TEST_KEY = new Station.Key("", "0");

    @Test
    void distanceTo() {
        var station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), null, null, true);
        assertThat(station.distanceTo(50.196580, 9.189395)).isCloseTo(53.1, within(0.1));
    }

    @Test
    void appliesToNullPhotographer() {
        var station = new Station(TEST_KEY, "", new Coordinates(0.0, 0.0), null, null, true);
        assertThat(station.appliesTo(null, "test", null, null, null, null)).isEqualTo(false);
        assertThat(station.appliesTo(false, null, null, null, null, null)).isEqualTo(true);
        assertThat(station.appliesTo(true, null, null, null, null, null)).isEqualTo(false);
    }

    @Test
    void appliesToPhotographer() {
        var station = new Station(TEST_KEY, "", new Coordinates(0.0, 0.0), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), true);
        assertThat(station.appliesTo(null, "test", null, null, null, null)).isEqualTo(true);
        assertThat(station.appliesTo(false, null, null, null, null, null)).isEqualTo(false);
        assertThat(station.appliesTo(true, null, null, null, null, null)).isEqualTo(true);
    }

    @Test
    void appliesToDistance() {
        var station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), null, null, true);
        assertThat(station.appliesTo(null, null, 50, 50.8, 9.8, null)).isEqualTo(true);
        assertThat(station.appliesTo(null, null, 50, 55.0, 8.0, null)).isEqualTo(false);
    }

    @Test
    void appliesToDistanceAndPhotographer() {
        var station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), true);
        assertThat(station.appliesTo(null, "test", 50, 50.8, 9.8, null)).isEqualTo(true);
    }

    @Test
    void appliesToActive() {
        var station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), true);
        assertThat(station.appliesTo(null, "test", null, null, null, true)).isEqualTo(true);
    }

    @Test
    void appliesToInactive() {
        var station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), false);
        assertThat(station.appliesTo(null, "test", null, null, null, false)).isEqualTo(true);
    }

    private User createTestPhotographer() {
        return User.builder().name("test").url("photographerUrl").license("CC0").id(0).ownPhotos(true).sendNotifications(true).build();
    }

}