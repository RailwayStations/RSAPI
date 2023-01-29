package org.railwaystations.rsapi.core.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;


class UserTest {

    @ParameterizedTest
    @CsvSource({
            "nickname, email@example.com, true",
            "nickname, email@example., false",
            ", email@example.com, false",
            "'', email@example.com, false",
            "nickname, email.example.com, false",
            "nickname,, false",
            "nickname,' ', false"})
    void testIsValidForRegistration(String name, String email, boolean expected) {
        assertThat(User.builder().name(name).email(email).ownPhotos(true).anonymous(false).sendNotifications(true).build().isValidForRegistration()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,                    , true",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,                    , true",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, http://example.com , true",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, https://example.com, true",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, ftp://example.com  , false",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, email@example.com  , false",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, '                 ', true",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), false,                   , false",
            "nickname, email@example.com, CC4,                          true,                    , false",
            "        , email@example.com, CC0 1.0 Universell (CC0 1.0), false,                   , false"})
    void testIsValid(String name, String email, String licenseDisplayName, boolean photoOwner, String link, boolean expected) {
        assertThat(User.builder().name(name).email(email).license(License.ofDisplayName(licenseDisplayName)).ownPhotos(photoOwner).url(link).anonymous(false).sendNotifications(true).build().isValid()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,  " + User.EMAIL_VERIFIED + "              , true",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), false, " + User.EMAIL_VERIFIED + "              , false",
            "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,  " + User.EMAIL_VERIFIED_AT_NEXT_LOGIN + ", false",
            "nickname, email@example.com, CC4,                          true,  " + User.EMAIL_VERIFIED + "              , false",
            "        , email@example.com, CC0 1.0 Universell (CC0 1.0), true,  VERIFICATION_TOKEN                       , false"})
    void testIsEligableForContributions(String name, String email, String licenseDisplayName, boolean photoOwner, String emailVerification, boolean expected) {
        assertThat(User.builder().name(name).email(email).license(License.ofDisplayName(licenseDisplayName)).ownPhotos(photoOwner).emailVerification(emailVerification).anonymous(false).sendNotifications(true).build().isEligableForContributions()).isEqualTo(expected);
    }

    @Test
    void testRoleUser() {
        var user = User.builder().name("@Nick Name").build();
        var roles = user.getRoles();
        assertThat(roles.size()).isEqualTo(1);
        assertThat(roles.contains(User.ROLE_USER)).isEqualTo(true);
        assertThat(roles.contains(User.ROLE_ADMIN)).isEqualTo(false);
    }

    @Test
    void testRolesAdmin() {
        var admin = User.builder().name("@Nick Name").admin(true).build();
        var roles = admin.getRoles();
        assertThat(roles.size()).isEqualTo(2);
        assertThat(roles.contains(User.ROLE_USER)).isEqualTo(true);
        assertThat(roles.contains(User.ROLE_ADMIN)).isEqualTo(true);
    }

    @ParameterizedTest
    @CsvSource({"https://example.com/user10, false, https://example.com/user10",
            "https://example.com/user10, true, https://railway-stations.org",
            ", false, https://railway-stations.org"})
    void testDisplayUrl(String url, boolean anonymous, String expectedDisplayUrl) {
        assertThat(User.builder().name("user10").anonymous(anonymous).url(url).build().getDisplayUrl()).isEqualTo(expectedDisplayUrl);
    }

    @ParameterizedTest
    @CsvSource({"user10, false, user10",
            "user10, true, Anonym"})
    void testDisplayName(String name, boolean anonymous, String expectedDisplayName) {
        assertThat(User.builder().name(name).anonymous(anonymous).build().getDisplayName()).isEqualTo(expectedDisplayName);
    }

}
