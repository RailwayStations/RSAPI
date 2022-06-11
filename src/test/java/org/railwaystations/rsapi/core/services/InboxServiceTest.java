package org.railwaystations.rsapi.core.services;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.User;

import static org.assertj.core.api.Assertions.assertThat;

class InboxServiceTest {

    @Test
    void getLicenseNoOverride() {
        assertThat(InboxService.getLicenseForPhoto(createUserWithCC0License(),
                Country.builder()
                        .code("de")
                        .build()))
                .isEqualTo(License.CC0_10);
    }

    private User createUserWithCC0License() {
        return User.builder()
                .license(License.CC0_10)
                .build();
    }

    @Test
    void getLicenseOverride() {
        assertThat(InboxService.getLicenseForPhoto(createUserWithCC0License(),
                Country.builder()
                        .code("fr")
                        .overrideLicense(License.CC_BY_NC_SA_30_DE)
                        .build()))
                .isEqualTo(License.CC_BY_NC_SA_30_DE);
    }

}