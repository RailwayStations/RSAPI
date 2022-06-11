package org.railwaystations.rsapi.core.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoTest {

    private static final Station.Key TEST_KEY = new Station.Key("", "0");

    /**
     * Tests if the mapping of the known license names to the license URLs works as expected.
     */
    @ParameterizedTest
    @EnumSource(License.class)
    void license2LicenseUrlMapping(License license) {
        var photo = Photo.builder()
                .stationKey(TEST_KEY)
                .urlPath("url")
                .photographer(createTestPhotographer(license))
                .license(license)
                .build();
        assertThat(license).isEqualTo(photo.getLicense());
    }

    private User createTestPhotographer(License license) {
        return User.builder().name("photographer").url("photographerUrl").license(license).id(0).ownPhotos(true).anonymous(false).admin(false).build();
    }

}