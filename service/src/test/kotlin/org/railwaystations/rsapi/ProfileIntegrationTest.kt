package org.railwaystations.rsapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
import com.ninjasquad.springmockk.MockkBean
import jakarta.validation.constraints.NotNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.railwaystations.rsapi.adapter.db.AbstractPostgreSqlTest
import org.railwaystations.rsapi.adapter.db.UserDao
import org.railwaystations.rsapi.core.model.License
import org.railwaystations.rsapi.core.model.User
import org.railwaystations.rsapi.core.ports.outbound.MailerPort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import java.util.regex.Pattern
import kotlin.Throws

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["server.error.include-message=always"]
)
@ActiveProfiles("test")
internal class ProfileIntegrationTest : AbstractPostgreSqlTest() {
    @Autowired
    private lateinit var mapper: ObjectMapper

    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var userDao: UserDao

    @MockkBean(relaxed = true)
    private lateinit var mailerPort: MailerPort

    @Test
    fun getProfileForbidden() {
        val response = restTemplate.withBasicAuth("nickname", "wrong")
            .getForEntity<String>("http://localhost:$port/myProfile")

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun getMyProfileWithBasicAuthPassword() {
        val response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
            .getForEntity<String>("http://localhost:$port/myProfile")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertProfile(response, "@user27", "https://www.example.com/user27", false, null)
    }

    @Test
    fun getMyProfileWithOAuthAndClientSecret() {
        val headers = HttpHeaders()
        var response = restTemplate.exchange<String>(
            "http://localhost:$port/oauth2/authorize?client_id=testClient&scope=all&response_type=code&redirect_uri=http://127.0.0.1:8000/authorized",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        val pattern = Pattern.compile("<input name=\"_csrf\" type=\"hidden\" value=\"(.*)\"")
        val matcher = response.body?.let { pattern.matcher(it) }
        assertThat(matcher?.find()).isTrue()
        val csrfToken = matcher?.group(1)

        val map = LinkedMultiValueMap<String, String>()
        map.add("username", "@user27")
        map.add("password", "y89zFqkL6hro")
        map.add("_csrf", csrfToken)

        headers.add(HttpHeaders.COOKIE, response.headers[HttpHeaders.SET_COOKIE]!![0])

        response = restTemplate.exchange<String>(
            "http://localhost:$port/login",
            HttpMethod.POST,
            HttpEntity(map, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.FOUND)

        headers.clear()
        headers.add(HttpHeaders.COOKIE, response.headers[HttpHeaders.SET_COOKIE]!![0])

        val nonRedirectingRestTemplate = nonRedirectingRestTemplate()
        response = nonRedirectingRestTemplate.exchange<String>(
            "http://localhost:$port/oauth2/authorize?client_id=testClient&scope=all&response_type=code&continue&redirect_uri=http://127.0.0.1:8000/authorized",
            HttpMethod.GET,
            HttpEntity(map, headers)
        )
        val location = response.headers[HttpHeaders.LOCATION]!![0]
        assertThat(location).startsWith("http://127.0.0.1:8000/authorized?code=")
        val code = location.substring(38)

        // request myProfile with first token
        var tokenResponse = requestToken(
            code = code,
            grantType = "authorization_code",
            expectedHttpStatus = HttpStatus.OK,
            testRestTemplate = restTemplateWithBacisAuthTestClient(),
            codeVerifier = null,
            clientId = "testClientId"
        )
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.OK)

        // revoke access_token
        revokeToken(
            restTemplateWithBacisAuthTestClient(),
            tokenResponse!!.accessToken.tokenValue,
            "access_token",
            HttpHeaders()
        )
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.UNAUTHORIZED)

        // request myProfile with refreshed token
        tokenResponse = requestToken(
            tokenResponse.refreshToken!!.tokenValue,
            "refresh_token",
            HttpStatus.OK,
            restTemplateWithBacisAuthTestClient(),
            null,
            "testClientId"
        )
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.OK)

        // revoke refresh_token
        revokeToken(
            restTemplateWithBacisAuthTestClient(),
            tokenResponse!!.refreshToken!!.tokenValue,
            "refresh_token",
            HttpHeaders()
        )
        tokenResponse = requestToken(
            tokenResponse.refreshToken!!.tokenValue,
            "refresh_token",
            HttpStatus.BAD_REQUEST,
            restTemplateWithBacisAuthTestClient(),
            null,
            "testClientId"
        )
        assertThat(tokenResponse).isNull()
    }

    @Test
    fun getMyProfileWithOAuthAndPKCE() {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        val headers = HttpHeaders()
        var response = restTemplate.exchange<String>(
            "http://localhost:$port/oauth2/authorize?client_id=publicTestClient&scope=all&response_type=code&code_challenge=$codeChallenge&code_challenge_method=S256&redirect_uri=http://127.0.0.1:8000/authorized",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull()
        val pattern = Pattern.compile("<input name=\"_csrf\" type=\"hidden\" value=\"(.*)\"")
        val matcher = response.body?.let { pattern.matcher(it) }
        assertThat(matcher?.find()).isTrue()
        val csrfToken = matcher?.group(1)

        val map = LinkedMultiValueMap<String, String>()
        map.add("username", "@user27")
        map.add("password", "y89zFqkL6hro")
        map.add("_csrf", csrfToken)

        headers.add(HttpHeaders.COOKIE, response.headers[HttpHeaders.SET_COOKIE]!![0])

        response = restTemplate.exchange<String>(
            "http://localhost:$port/login",
            HttpMethod.POST,
            HttpEntity(map, headers),
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.FOUND)

        headers.clear()
        headers.add(HttpHeaders.COOKIE, response.headers[HttpHeaders.SET_COOKIE]!![0])

        val nonRedirectingRestTemplate = nonRedirectingRestTemplate()
        response = nonRedirectingRestTemplate.exchange<String>(
            "http://localhost:$port/oauth2/authorize?client_id=publicTestClient&scope=all&response_type=code&code_challenge=$codeChallenge&code_challenge_method=S256&continue&redirect_uri=http://127.0.0.1:8000/authorized",
            HttpMethod.GET,
            HttpEntity(map, headers)
        )
        val location = response.headers["Location"]!![0]
        assertThat(location).startsWith("http://127.0.0.1:8000/authorized?code=")
        val code = location.substring(38)

        // request myProfile with first token
        val tokenResponse =
            requestToken(code, "authorization_code", HttpStatus.OK, restTemplate, codeVerifier, "publicTestClient")
        assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.OK)

        // we don't get a refresh token for public clients, so we can't test the refresh token flow
        assertThat(tokenResponse!!.refreshToken).isNull()

        // revoke access_token not possible without client authentication
        //headers.clear();
        //headers.add("Authorization", "Bearer " + tokenResponse.getAccessToken().getTokenValue());
        //revokeToken(restTemplate, tokenResponse.getAccessToken().getTokenValue(), "access_token", headers);
        //assertMyProfileRequestWithOAuthToken(tokenResponse, HttpStatus.UNAUTHORIZED);
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32)
        secureRandom.nextBytes(codeVerifier)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charset.defaultCharset())
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun assertMyProfileRequestWithOAuthToken(
        tokenResponse: OAuth2AccessTokenResponse?,
        expectedHttpStatus: HttpStatus
    ) {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse!!.accessToken.tokenValue)
        val response = restTemplate.exchange<String>(
            "http://localhost:$port/myProfile",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
        )
        assertThat(response.statusCode).isEqualTo(expectedHttpStatus)
        if (expectedHttpStatus == HttpStatus.OK) {
            assertProfile(response, "@user27", "https://www.example.com/user27", false, null)
        }
    }

    private fun revokeToken(
        testRestTemplate: TestRestTemplate,
        token: String,
        tokenType: String,
        headers: HttpHeaders
    ) {
        val map = LinkedMultiValueMap<String, String>()
        map.add("token", token)
        map.add("token_type_hint", tokenType)
        val response = testRestTemplate
            .exchange<String>(
                "http://localhost:$port/oauth2/revoke",
                HttpMethod.POST,
                HttpEntity(map, headers),
            )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    private fun restTemplateWithBacisAuthTestClient(): TestRestTemplate {
        return restTemplate.withBasicAuth("testClient", "secret")
    }

    private fun requestToken(
        code: String,
        grantType: String,
        expectedHttpStatus: HttpStatus,
        testRestTemplate: TestRestTemplate,
        codeVerifier: String?,
        clientId: String
    ): OAuth2AccessTokenResponse? {
        val map = LinkedMultiValueMap<String, String>()
        map.add("grant_type", grantType)
        if ("refresh_token" == grantType) {
            map.add(grantType, code)
        } else {
            map.add("code", code)
            map.add("client_id", clientId)
            map.add("redirect_uri", "http://127.0.0.1:8000/authorized")
        }
        if (codeVerifier != null) {
            map.add("code_verifier", codeVerifier)
        }
        val response = testRestTemplate.exchange<String>(
            "http://localhost:$port/oauth2/token",
            HttpMethod.POST,
            HttpEntity(map, HttpHeaders()),
        )
        assertThat(response.statusCode).isEqualTo(expectedHttpStatus)
        if (expectedHttpStatus != HttpStatus.OK) {
            return null
        }
        val jsonNode = mapper.readTree(response.body)
        val tokenResponseBuilder = OAuth2AccessTokenResponse.withToken(jsonNode["access_token"].asText())
        val refreshTokenNode = jsonNode["refresh_token"]
        if (refreshTokenNode != null) {
            tokenResponseBuilder.refreshToken(refreshTokenNode.asText())
        }
        tokenResponseBuilder.scopes(setOf(jsonNode["scope"].asText()))
        val tokenType = jsonNode["token_type"].asText()
        assertThat(tokenType).isEqualTo("Bearer")
        tokenResponseBuilder.tokenType(OAuth2AccessToken.TokenType.BEARER)
        tokenResponseBuilder.expiresIn(jsonNode["expires_in"].asInt().toLong())
        val accessTokenResponse = tokenResponseBuilder.build()
        assertThat(accessTokenResponse).isNotNull()
        assertThat(accessTokenResponse.accessToken).isNotNull()
        assertThat(accessTokenResponse.accessToken.scopes).contains("all")
        assertThat(accessTokenResponse.accessToken.tokenType).isEqualTo(OAuth2AccessToken.TokenType.BEARER)
        return accessTokenResponse
    }

    private fun nonRedirectingRestTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        val factory: SimpleClientHttpRequestFactory =
            object : SimpleClientHttpRequestFactory() {
                @Throws(IOException::class)
                public override fun prepareConnection(
                    connection: @NotNull HttpURLConnection,
                    httpMethod: @NotNull String
                ) {
                    super.prepareConnection(connection, httpMethod)
                    connection.instanceFollowRedirects = false
                }
            }

        restTemplate.requestFactory = factory
        return restTemplate
    }

    @Test
    fun getMyProfileWithBasicAuthPasswordFail() {
        val response = restTemplate.withBasicAuth("@user27", "blahblubb")
            .getForEntity<String>("http://localhost:$port/myProfile")

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    private fun assertProfile(
        response: ResponseEntity<String>,
        name: String,
        link: String?,
        anonymous: Boolean,
        email: String?
    ) {
        val jsonNode = mapper.readTree(response.body)
        assertThat(jsonNode["nickname"].asText()).isEqualTo(name)
        if (email != null) {
            assertThat(jsonNode["email"].asText()).isEqualTo(email)
        } else {
            assertThat(jsonNode["email"]).isNull()
        }
        if (link != null) {
            assertThat(jsonNode["link"].asText()).isEqualTo(link)
        } else {
            assertThat(jsonNode["link"]).isNull()
        }
        assertThat(jsonNode["license"].asText()).isEqualTo("CC0 1.0 Universell (CC0 1.0)")
        assertThat(jsonNode["photoOwner"].asBoolean()).isEqualTo(true)
        assertThat(jsonNode["anonymous"].asBoolean()).isEqualTo(anonymous)
        assertThat(jsonNode.has("uploadToken")).isEqualTo(false)
    }

    @Test
    fun testUpdateMyProfileNameTooLong() {
        val headers = HttpHeaders()
        headers.setBasicAuth("@user27", "y89zFqkL6hro")
        headers.contentType = MediaType.APPLICATION_JSON

        val responsePostUpdate = restTemplate.postForEntity<String>(
            "http://localhost:$port/myProfile", HttpEntity(
                """
                        {
                        	"nickname": "A very long name with a lot of extra words to overfill the database column",
                        	"email": "user27@example.com",
                        	"license": "CC0",
                        	"photoOwner": true,
                        	"anonymous": true
                        }
                        """.trimIndent(), headers
            )
        )
        assertThat(responsePostUpdate.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun updateMyProfileAndChangePassword() {
        val firstPassword = "GDAkhaeU2vrK"
        val responseGetBefore = restTemplate.withBasicAuth("user14@example.com", firstPassword)
            .getForEntity<String>("http://localhost:$port/myProfile")
        assertThat(responseGetBefore.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseGetBefore.body).isNotNull()
        assertProfile(responseGetBefore, "@user14", "https://www.example.com/user14", false, "user14@example.com")

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val responsePostUpdate = restTemplate.withBasicAuth("user14@example.com", firstPassword)
            .postForEntity<String>(
                "http://localhost:$port/myProfile", HttpEntity(
                    """
                        {
                        	"nickname": "user14",
                        	"email": "user14@example.com",
                        	"license": "CC0",
                        	"photoOwner": true,
                        	"anonymous": true
                        }
                        """.trimIndent(), headers
                )
            )
        assertThat(responsePostUpdate.statusCode).isEqualTo(HttpStatus.OK)

        val responseGetAfter = restTemplate.withBasicAuth("user14@example.com", firstPassword)
            .getForEntity<String>("http://localhost:$port/myProfile")
        assertThat(responseGetAfter.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseGetAfter.body).isNotNull()
        assertProfile(responseGetAfter, "user14", null, true, "user14@example.com")

        val secondPassword = "!\"$%&/()=?-1234567890"
        changePassword(firstPassword, secondPassword)
        changePassword(secondPassword, "\\=oF`)X77__U}G")
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        val headers = HttpHeaders()
        headers.setBasicAuth("user14@example.com", oldPassword)
        headers.contentType = MediaType.APPLICATION_JSON

        val changePassword = mapper.createObjectNode()
        changePassword.set<JsonNode>("newPassword", TextNode(newPassword))
        val changePasswordRequest = HttpEntity(changePassword, headers)

        val responseChangePassword = restTemplate.postForEntity<String>(
            "http://localhost:$port/changePassword", changePasswordRequest
        )
        assertThat(responseChangePassword.statusCode).isEqualTo(HttpStatus.OK)

        val responseAfterChangedPassword = restTemplate
            .withBasicAuth("user14@example.com", newPassword)
            .getForEntity<String>("http://localhost:$port/myProfile")
        assertThat(responseAfterChangedPassword.statusCode).isEqualTo(HttpStatus.OK)

        val responseWithOldPassword = restTemplate.exchange<String>(
            "http://localhost:$port/myProfile",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
        )
        assertThat(responseWithOldPassword.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun deleteMyProfileWithBasicAuth() {
        // given
        val responseGet1 = restTemplate.withBasicAuth("@user21", "uON60I7XWTIN")
            .getForEntity<String>("http://localhost:$port/myProfile")
        assertThat(responseGet1.statusCode).isEqualTo(HttpStatus.OK)
        assertProfile(responseGet1, "@user21", "https://www.example.com/user21", false, "user21@example.com")

        // when
        val responseDelete = restTemplate.withBasicAuth("@user21", "uON60I7XWTIN")
            .exchange<Void>(
                "http://localhost:$port/myProfile",
                HttpMethod.DELETE,
            )

        // then
        assertThat(responseDelete.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        val user = userDao.findById(22)
        assertThat(user).usingRecursiveComparison().isEqualTo(
            User(
                id = 22,
                name = "deleteduser22",
                license = License.UNKNOWN,
                ownPhotos = false,
                anonymous = true,
            )
        )
        val responseGet2 = restTemplate.withBasicAuth("@user21", "uON60I7XWTIN")
            .getForEntity<String>("http://localhost:$port/myProfile")
        assertThat(responseGet2.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
