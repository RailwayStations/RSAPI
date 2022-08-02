package org.railwaystations.rsapi.core.services;

import org.junit.jupiter.api.Test;
import org.railwaystations.rsapi.core.model.Country;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.User;

import static org.assertj.core.api.Assertions.assertThat;

class InboxServiceTest {

    @Test
    void licenseOfPhotoShouldBeTheLicenseOfUser() {
        var licenseForPhoto = InboxService.getLicenseForPhoto(createUserWithCC0License(), createCountryWithOverrideLicense(null));
        assertThat(licenseForPhoto).isEqualTo(License.CC0_10);
    }

    @Test
    void licenseOfPhotoShouldBeOverridenByLicenseOfCountry() {
        var licenseForPhoto = InboxService.getLicenseForPhoto(createUserWithCC0License(), createCountryWithOverrideLicense(License.CC_BY_NC_SA_30_DE));
        assertThat(licenseForPhoto).isEqualTo(License.CC_BY_NC_SA_30_DE);
    }

    private Country createCountryWithOverrideLicense(License overrideLicense) {
        return Country.builder()
                .code("xx")
                .overrideLicense(overrideLicense)
                .build();
    }

    private User createUserWithCC0License() {
        return User.builder()
                .license(License.CC0_10)
                .build();
    }

}