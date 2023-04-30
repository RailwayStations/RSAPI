package org.railwaystations.rsapi.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.rsapi.adapter.out.db.UserDao;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.error.include-message=always"})
@ActiveProfiles("test")
class ProfileIntegrationTests extends AbstractMariaDBBaseTest {

    @Autowired
    private ObjectMapper mapper;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserDao userDao;

    @MockBean
    private Mailer mailer;

    @Test
    void register() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        var response = restTemplate.postForEntity(
                String.format("http://localhost:%d%s", port, "/registration"), new HttpEntity<>("""
                        {
                        	"nickname": "nickname ",
                        	"email": "nick.name@example.com",
                        	"license": "CC0",
                        	"photoOwner": true,
                        	"link": ""
                        }
                        """, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        verify(mailer, Mockito.times(1))
                .send(eq("nick.name@example.com"),
                        eq("Railway-Stations.org new password"),
                        matches("""
                                Hello,

                                your new password is: .*

                                Cheers
                                Your Railway-Stations-Team

                                ---
                                Hallo,

                                Dein neues Passwort lautet: .*

                                Viele Grüße
                                Dein Bahnhofsfoto-Team"""));
    }

    @Test
    void registerDifferentEmail() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = restTemplate.postForEntity(
                String.format("http://localhost:%d%s", port, "/registration"), new HttpEntity<>("""
                        {
                        	"nickname": "user14",
                        	"email": "other@example.com",
                        	"license": "CC0",
                        	"photoOwner": true,
                        	"link": "link"
                        }""", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getProfileForbidden() {
        var headers = new HttpHeaders();
        headers.add("Nickname", "nickname");
        headers.add("Email", "nickname@example.com");
        headers.add("Upload-Token", "wrong");
        var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyProfileWithEmailAndUploadToken() throws IOException {
        var headers = new HttpHeaders();
        headers.add("Upload-Token", "uON60I7XWTIN");
        headers.add("Email", "user10@example.com");
        var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
    }

    @Test
    void getMyProfileWithNameAndUploadToken() throws IOException {
        var headers = new HttpHeaders();
        headers.add("Upload-Token", "uON60I7XWTIN");
        headers.add("Email", "@user10");
        var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
    }

    @Test
    void getMyProfileWithBasicAuthUploadToken() throws IOException {
        var response = restTemplate.withBasicAuth("@user10", "uON60I7XWTIN")
                .getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
    }

    @Test
    void getMyProfileWithBasicAuthPassword() throws IOException {
        var response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
                .getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertProfile(response, "@user27", "https://www.example.com/user27", false, null);
    }

    @Test
    void getMyProfileWithOAuthAndClientSecret() throws IOException {
        var headers = new HttpHeaders();
        var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/oauth2/authorize?client_id=testClient&scope=all&response_type=code&redirect_uri=http://127.0.0.1:8000/authorized"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        var pattern = Pattern.compile("<input name=\"_csrf\" type=\"hidden\" value=\"(.*)\"");
        var matcher = pattern.matcher(response.getBody());
        assertThat(matcher.find()).isTrue();
        var csrfToken = matcher.group(1);

        var map = new LinkedMultiValueMap<String, String>();
        map.add("username", "@user27");
        map.add("password", "y89zFqkL6hro");
        map.add("_csrf", csrfToken);

        headers.add("Cookie", response.getHeaders().get("Set-Cookie").get(0));

        response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/login"), HttpMethod.POST, new HttpEntity<>(map, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        headers.clear();
        headers.add("Cookie", response.getHeaders().get("Set-Cookie").get(0));

        var nonRedirectingRestTemplate = nonRedirectingRestTemplate();
        response = nonRedirectingRestTemplate.exchange(String.format("http://localhost:%d%s", port, "/oauth2/authorize?client_id=testClient&scope=all&response_type=code&continue&redirect_uri=http://127.0.0.1:8000/authorized"), HttpMethod.GET, new HttpEntity<>(map, headers), String.class);
        var location = response.getHeaders().get("Location").get(0);
        assertThat(location).startsWith("http://127.0.0.1:8000/authorized?code=");
        var code = location.substring(38);

        // request myProfile with first token
        var tokenResponse = requestToken(code, "authorization_code", HttpStatus.OK, restTemplateWithBacisAuthTestClient(), null, "testClientId");
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.OK);

        // revoke access_token
        revokeToken(restTemplateWithBacisAuthTestClient(), tokenResponse.getAccessToken().getTokenValue(), "access_token", new HttpHeaders());
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.UNAUTHORIZED);

        // request myProfile with refreshed token
        tokenResponse = requestToken(tokenResponse.getRefreshToken().getTokenValue(), "refresh_token", HttpStatus.OK, restTemplateWithBacisAuthTestClient(), null, "testClientId");
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.OK);

        // revoke refresh_token
        revokeToken(restTemplateWithBacisAuthTestClient(), tokenResponse.getRefreshToken().getTokenValue(), "refresh_token", new HttpHeaders());
        tokenResponse = requestToken(tokenResponse.getRefreshToken().getTokenValue(), "refresh_token", HttpStatus.BAD_REQUEST, restTemplateWithBacisAuthTestClient(), null, "testClientId");
        assertThat(tokenResponse).isNull();
    }

    @Test
    void getMyProfileWithOAuthAndPKCE() throws IOException, NoSuchAlgorithmException {
        var codeVerifier = generateCodeVerifier();
        var codeChallenge = generateCodeChallenge(codeVerifier);

        var headers = new HttpHeaders();
        var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/oauth2/authorize?client_id=publicTestClient&scope=all&response_type=code&code_challenge=" + codeChallenge + "&code_challenge_method=S256&redirect_uri=http://127.0.0.1:8000/authorized"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        var pattern = Pattern.compile("<input name=\"_csrf\" type=\"hidden\" value=\"(.*)\"");
        var matcher = pattern.matcher(response.getBody());
        assertThat(matcher.find()).isTrue();
        var csrfToken = matcher.group(1);

        var map = new LinkedMultiValueMap<String, String>();
        map.add("username", "@user27");
        map.add("password", "y89zFqkL6hro");
        map.add("_csrf", csrfToken);

        headers.add("Cookie", response.getHeaders().get("Set-Cookie").get(0));

        response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/login"), HttpMethod.POST, new HttpEntity<>(map, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        headers.clear();
        headers.add("Cookie", response.getHeaders().get("Set-Cookie").get(0));

        var nonRedirectingRestTemplate = nonRedirectingRestTemplate();
        response = nonRedirectingRestTemplate.exchange(String.format("http://localhost:%d%s", port, "/oauth2/authorize?client_id=publicTestClient&scope=all&response_type=code&code_challenge=" + codeChallenge + "&code_challenge_method=S256&continue&redirect_uri=http://127.0.0.1:8000/authorized"), HttpMethod.GET, new HttpEntity<>(map, headers), String.class);
        var location = response.getHeaders().get("Location").get(0);
        assertThat(location).startsWith("http://127.0.0.1:8000/authorized?code=");
        var code = location.substring(38);

        // request myProfile with first token
        var tokenResponse = requestToken(code, "authorization_code", HttpStatus.OK, restTemplate, codeVerifier, "publicTestClient");
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.OK);

        // we don't get a refresh token for public clients, so we can't test the refresh token flow
        assertThat(tokenResponse.getRefreshToken()).isNull();

        // revoke access_token not possible without client authentication
        //headers.clear();
        //headers.add("Authorization", "Bearer " + tokenResponse.getAccessToken().getTokenValue());
        //revokeToken(restTemplate, tokenResponse.getAccessToken().getTokenValue(), "access_token", headers);
        //assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.UNAUTHORIZED);
    }

    private String generateCodeVerifier() {
        var secureRandom = new SecureRandom();
        var codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        var bytes = codeVerifier.getBytes(Charset.defaultCharset());
        var messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        var digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private void assertMyProfileRequestWithOAuthToken(OAuth2AccessTokenResponse tokenResponse, HttpStatus expectedHttpStatus) throws IOException {
        var headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + tokenResponse.getAccessToken().getTokenValue());
        var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(expectedHttpStatus);
        if (expectedHttpStatus == HttpStatus.OK) {
            assertProfile(response, "@user27", "https://www.example.com/user27", false, null);
        }
    }

    private void revokeToken(TestRestTemplate testRestTemplate, String token, String tokenType, HttpHeaders headers) {
        var map = new LinkedMultiValueMap<String, String>();
        map.add("token", token);
        map.add("token_type_hint", tokenType);
        var response = testRestTemplate
                .exchange(String.format("http://localhost:%d%s", port, "/oauth2/revoke"), HttpMethod.POST, new HttpEntity<>(map, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private TestRestTemplate restTemplateWithBacisAuthTestClient() {
        return restTemplate.withBasicAuth("testClient", "secret");
    }

    private OAuth2AccessTokenResponse requestToken(String code, String grantType, HttpStatus expectedHttpStatus, TestRestTemplate testRestTemplate, String codeVerifier, String clientId) throws JsonProcessingException {
        var map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", grantType);
        if ("refresh_token".equals(grantType)) {
            map.add(grantType, code);
        } else {
            map.add("code", code);
            map.add("client_id", clientId);
            map.add("redirect_uri", "http://127.0.0.1:8000/authorized");
        }
        if (codeVerifier != null) {
            map.add("code_verifier", codeVerifier);
        }
        var response = testRestTemplate
                .exchange(String.format("http://localhost:%d%s", port, "/oauth2/token"), HttpMethod.POST, new HttpEntity<>(map, new HttpHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(expectedHttpStatus);
        if (expectedHttpStatus != HttpStatus.OK) {
            return null;
        }
        var jsonNode = mapper.readTree(response.getBody());
        var tokenResponseBuilder = OAuth2AccessTokenResponse.withToken(jsonNode.get("access_token").asText());
        var refreshTokenNode = jsonNode.get("refresh_token");
        if (refreshTokenNode != null) {
            tokenResponseBuilder.refreshToken(refreshTokenNode.asText());
        }
        tokenResponseBuilder.scopes(Set.of(jsonNode.get("scope").asText()));
        var tokenType = jsonNode.get("token_type").asText();
        assertThat(tokenType).isEqualTo("Bearer");
        tokenResponseBuilder.tokenType(OAuth2AccessToken.TokenType.BEARER);
        tokenResponseBuilder.expiresIn(jsonNode.get("expires_in").asInt());
        var accessTokenResponse = tokenResponseBuilder.build();
        assertThat(accessTokenResponse).isNotNull();
        assertThat(accessTokenResponse.getAccessToken()).isNotNull();
        assertThat(accessTokenResponse.getAccessToken().getScopes()).contains("all");
        assertThat(accessTokenResponse.getAccessToken().getTokenType()).isEqualTo(OAuth2AccessToken.TokenType.BEARER);
        return accessTokenResponse;
    }

    private RestTemplate nonRedirectingRestTemplate() {
        var restTemplate = new RestTemplate();
        var factory =
                new SimpleClientHttpRequestFactory() {
                    public void prepareConnection(@NotNull HttpURLConnection connection, @NotNull String httpMethod) throws IOException {
                        super.prepareConnection(connection, httpMethod);
                        connection.setInstanceFollowRedirects(false);
                    }
                };

        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }

    @Test
    void getMyProfileWithBasicAuthPasswordFail() {
        var response = restTemplate.withBasicAuth("@user27", "blahblubb")
                .getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void assertProfile(ResponseEntity<String> response, String name, String link, boolean anonymous, String email) throws IOException {
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode.get("nickname").asText()).isEqualTo(name);
        if (email != null) {
            assertThat(jsonNode.get("email").asText()).isEqualTo(email);
        } else {
            assertThat(jsonNode.get("email")).isNull();
        }
        if (link != null) {
            assertThat(jsonNode.get("link").asText()).isEqualTo(link);
        } else {
            assertThat(jsonNode.get("link")).isNull();
        }
        assertThat(jsonNode.get("license").asText()).isEqualTo("CC0 1.0 Universell (CC0 1.0)");
        assertThat(jsonNode.get("photoOwner").asBoolean()).isEqualTo(true);
        assertThat(jsonNode.get("anonymous").asBoolean()).isEqualTo(anonymous);
        assertThat(jsonNode.has("uploadToken")).isEqualTo(false);
    }

    @Test
    void testUpdateMyProfileNameTooLong() {
        var headers = new HttpHeaders();
        headers.setBasicAuth("@user27", "y89zFqkL6hro");
        headers.setContentType(MediaType.APPLICATION_JSON);

        var responsePostUpdate = restTemplate.postForEntity(
                String.format("http://localhost:%d%s", port, "/myProfile"), new HttpEntity<>("""
                        {
                        	"nickname": "A very long name with a lot of extra words to overfill the database column",
                        	"email": "user27@example.com",
                        	"license": "CC0",
                        	"photoOwner": true,
                        	"anonymous": true
                        }""", headers), String.class);
        assertThat(responsePostUpdate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateMyProfileAndChangePassword() throws IOException {
        var firstPassword = "GDAkhaeU2vrK";
        var headers = new HttpHeaders();
        headers.add("Upload-Token", firstPassword);
        headers.add("Email", "user14@example.com");
        var responseGetBefore = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(responseGetBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseGetBefore.getBody()).isNotNull();
        assertProfile(responseGetBefore, "@user14", "https://www.example.com/user14", false, "user14@example.com");

        headers.setContentType(MediaType.APPLICATION_JSON);
        var responsePostUpdate = restTemplate.postForEntity(
                String.format("http://localhost:%d%s", port, "/myProfile"), new HttpEntity<>("""
                        {
                        	"nickname": "user14",
                        	"email": "user14@example.com",
                        	"license": "CC0",
                        	"photoOwner": true,
                        	"anonymous": true
                        }""", headers), String.class);
        assertThat(responsePostUpdate.getStatusCode()).isEqualTo(HttpStatus.OK);

        var responseGetAfter = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(responseGetAfter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseGetAfter.getBody()).isNotNull();
        assertProfile(responseGetAfter, "user14", null, true, "user14@example.com");

        var secondPassword = "!\"$%&/()=?-1234567890";
        changePassword(firstPassword, secondPassword, true);
        changePassword(secondPassword, "\\=oF`)X77__U}G", false);
    }

    private void changePassword(String oldPassword, String newPassword, boolean changePasswordViaHeader) {
        var headers = new HttpHeaders();
        headers.setBasicAuth("user14@example.com", oldPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> changePasswordRequest;
        if (changePasswordViaHeader) {
            headers.add("New-Password", newPassword);
            changePasswordRequest = new HttpEntity<>(headers);
        } else {
            var changePassword = mapper.createObjectNode();
            changePassword.set("newPassword", new TextNode(newPassword));
            changePasswordRequest = new HttpEntity<>(changePassword, headers);
        }

        var responseChangePassword = restTemplate.postForEntity(
                String.format("http://localhost:%d%s", port, "/changePassword"), changePasswordRequest, String.class);
        assertThat(responseChangePassword.getStatusCode()).isEqualTo(HttpStatus.OK);

        var responseAfterChangedPassword = restTemplate
                .withBasicAuth("user14@example.com", newPassword)
                .getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);
        assertThat(responseAfterChangedPassword.getStatusCode()).isEqualTo(HttpStatus.OK);

        var responseWithOldPassword = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(responseWithOldPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteMyProfileWithBasicAuth() throws IOException {
        // given
        var responseGet1 = restTemplate.withBasicAuth("@user21", "uON60I7XWTIN")
                .getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);
        assertThat(responseGet1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertProfile(responseGet1, "@user21", "https://www.example.com/user21", false, "user21@example.com");

        // when
        var responseDelete = restTemplate.withBasicAuth("@user21", "uON60I7XWTIN")
                .exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.DELETE, null, Void.class);

        // then
        assertThat(responseDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        var user = userDao.findById(22).get();
        assertThat(user).usingRecursiveComparison().isEqualTo(User.builder()
                .id(22)
                .name("deleteduser22")
                .anonymous(true)
                .ownPhotos(false)
                .license(null)
                .url(null)
                .email(null)
                .key(null)
                .sendNotifications(false)
                .admin(false)
                .build());
        var responseGet2 = restTemplate.withBasicAuth("@user21", "uON60I7XWTIN")
                .getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);
        assertThat(responseGet2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

}
