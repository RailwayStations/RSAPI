package org.railwaystations.rsapi.core.services;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.User;

import static org.assertj.core.api.Assertions.assertThat;

class InboxServiceTest {

    @Test
    public void getLicenseNoOverride() {
        assertThat(InboxService.getLicenseForPhoto(createUserWithCC0License(),
                Country.builder()
                        .code("de")
                        .build()))
                .isEqualTo("CC0");
    }

    private User createUserWithCC0License() {
        return User.builder()
                .license("CC0")
                .build();
    }

    @Test
    public void getLicenseOverride() {
        assertThat(InboxService.getLicenseForPhoto(createUserWithCC0License(),
                Country.builder()
                        .code("fr")
                        .name("France")
                        .overrideLicense("CC1")
                        .active(true)
                        .build()))
                .isEqualTo("CC1");
    }

}