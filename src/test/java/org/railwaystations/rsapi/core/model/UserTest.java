package org.railwaystations.rsapi.core.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;


public class UserTest {

    @ParameterizedTest
    @CsvSource({ "nickname, email@example.com, true",
            "nickname, email@example., false",
            ", email@example.com, false",
            "'', email@example.com, false",
            "nickname, email.example.com, false",
            "nickname,, false",
            "nickname,' ', false"})
    public void testIsValidForRegistration(final String name, final String email, final boolean expected) {
        assertThat(User.builder().name(name).email(email).ownPhotos(true).anonymous(false).sendNotifications(true).build().isValidForRegistration()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,                    , true",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true,                    , true",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, http://example.com , true",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, https://example.com, true",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, ftp://example.com  , false",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, email@example.com  , false",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, '                 ', true",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), false,                   , false",
                 "nickname, email@example.com, CC4,                          true,                    , false",
                 "        , email@example.com, CC0 1.0 Universell (CC0 1.0), false,                   , false"})
    public void testIsValid(final String name, final String email, final String license, final boolean photoOwner, final String link, final boolean expected) {
        assertThat(User.builder().name(name).email(email).license(license).ownPhotos(photoOwner).url(link).anonymous(false).sendNotifications(true).build().isValid()).isEqualTo(expected);
    }

    @Test
    public void testRoleUser() {
        final var user = User.builder().name("@Nick Name").build();
        final var roles = user.getRoles();
        assertThat(roles.size()).isEqualTo(1);
        assertThat(roles.contains(User.ROLE_USER)).isEqualTo(true);
        assertThat(roles.contains(User.ROLE_ADMIN)).isEqualTo(false);
    }

    @Test
    public void testRolesAdmin() {
        final var admin = User.builder().name("@Nick Name").admin(true).build();
        final var roles = admin.getRoles();
        assertThat(roles.size()).isEqualTo(2);
        assertThat(roles.contains(User.ROLE_USER)).isEqualTo(true);
        assertThat(roles.contains(User.ROLE_ADMIN)).isEqualTo(true);
    }

}
