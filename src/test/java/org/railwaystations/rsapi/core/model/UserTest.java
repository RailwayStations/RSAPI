package org.railwaystations.rsapi.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

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
        assertThat(new User(name, email, null, true, null, false, null, true).isValidForRegistration()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "nickname, email@example.com, CC0, true,                    , true",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, , true",
                 "nickname, email@example.com, CC0, true, http://example.com , true",
                 "nickname, email@example.com, CC0, true, https://example.com, true",
                 "nickname, email@example.com, CC0, true, ftp://example.com  , false",
                 "nickname, email@example.com, CC0, true, email@example.com  , false",
                 "nickname, email@example.com, CC0, true, '                 ', true",
                 "nickname, email@example.com, CC0, false,                   , false",
                 "nickname, email@example.com, CC4, true,                    , false",
                 "        , email@example.com, CC0, false,                   , false"})
    public void testIsValid(final String name, final String email, final String license, final boolean photoOwner, final String link, final boolean expected) {
        assertThat(new User(name, email, license, photoOwner, link, false, null, true).isValid()).isEqualTo(expected);
    }

    @Test
    public void testJsonDeserialization() throws IOException {
        final var mapper = new ObjectMapper();
        final User user = mapper.readerFor(User.class).readValue("{\"id\":\"1\", \"nickname\":\"@Nick Name\",\"email\":\"nick@example.com\",\"license\":\"CC0 1.0 Universell (CC0 1.0)\",\"photoOwner\":true,\"link\":\"https://example.com\",\"anonymous\":false,\"uploadToken\":\"token\",\"uploadTokenSalt\":\"123456\",\"key\":\"key\", \"admin\":true}");
        assertThat(user.getId()).isEqualTo(0);
        assertThat(user.getName()).isEqualTo("@Nick Name");
        assertThat(user.getDisplayName()).isEqualTo("@Nick Name");
        assertThat(user.getNormalizedName()).isEqualTo("nickname");
        assertThat(user.getEmail()).isEqualTo("nick@example.com");
        assertThat(user.getLicense()).isEqualTo("CC0 1.0 Universell (CC0 1.0)");
        assertThat(user.isOwnPhotos()).isEqualTo(true);
        assertThat(user.getUrl()).isEqualTo("https://example.com");
        assertThat(user.isAnonymous()).isEqualTo(false);
        assertThat(user.getKey()).isNull();
        assertThat(user.isAdmin()).isEqualTo(false);
    }

    @Test
    public void testJsonSerialization() throws IOException {
        final var mapper = new ObjectMapper();
        final var user = new User("@Nick Name", "https://example.com", "CC0 1.0 Universell (CC0 1.0)", 1, "nick@example.com", true, true, "key", true, null, true);
        final var json = mapper.writerFor(User.class).writeValueAsString(user);
        assertThat(json).isEqualTo("{\"nickname\":\"@Nick Name\",\"email\":\"nick@example.com\",\"license\":\"CC0 1.0 Universell (CC0 1.0)\",\"photoOwner\":true,\"link\":\"https://example.com\",\"anonymous\":true,\"sendNotifications\":true,\"admin\":true,\"emailVerified\":false}");
    }

    @Test
    public void testRoleUser() {
        final var user = new User("@Nick Name", "https://example.com", "CC0 1.0 Universell (CC0 1.0)", 1, "nick@example.com", true, true, "key", false, null, true);
        final var roles = user.getRoles();
        assertThat(roles.size()).isEqualTo(1);
        assertThat(roles.contains(User.ROLE_USER)).isEqualTo(true);
        assertThat(roles.contains(User.ROLE_ADMIN)).isEqualTo(false);
    }

    @Test
    public void testRolesAdmin() {
        final var admin = new User("@Nick Name", "https://example.com", "CC0 1.0 Universell (CC0 1.0)", 1, "nick@example.com", true, true, "key", true, null, true);
        final var roles = admin.getRoles();
        assertThat(roles.size()).isEqualTo(2);
        assertThat(roles.contains(User.ROLE_USER)).isEqualTo(true);
        assertThat(roles.contains(User.ROLE_ADMIN)).isEqualTo(true);
    }

}
