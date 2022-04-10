package org.railwaystations.rsapi.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.rsapi.core.ports.out.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"server.error.include-message=always"})
@ActiveProfiles("test")
class ProfileIntegrationTests {

	@Autowired
	private ObjectMapper mapper;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@MockBean
	private Mailer mailer;

	private static final MariaDBContainer<?> mariadb;

	static {
		mariadb = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.1"));
		mariadb.start();
	}

	@DynamicPropertySource
	static void properties(final DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mariadb::getJdbcUrl);
		registry.add("spring.datasource.username", mariadb::getUsername);
		registry.add("spring.datasource.password", mariadb::getPassword);
	}

	@Test
	void contextLoads() {
	}

	@Test
	public void register() {
		final var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		final var response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/registration"), new HttpEntity<>("""
						{
							"nickname": "nickname ",
							"email": "nick.name@example.com",
							"license": "CC0",
							"photoOwner": true,
							"link": ""
						}
						""", headers), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(202);

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
	public void registerDifferentEmail() {
		final var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		final var response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/registration"),new HttpEntity<>("""
						{
							"nickname": "user14",
							"email": "other@example.com",
							"license": "CC0",
							"photoOwner": true,
							"link": "link"
						}""", headers), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(409);
	}

	@Test
	public void getProfileForbidden() {
		final var headers = new HttpHeaders();
		headers.add("Nickname", "nickname");
		headers.add("Email", "nickname@example.com");
		final var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(401);
	}

	@Test
	public void getMyProfileWithEmail() throws IOException {
		final var headers = new HttpHeaders();
		headers.add("Upload-Token", "uON60I7XWTIN");
		headers.add("Email", "user10@example.com");
		final var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
	}

	@Test
	public void getMyProfileWithName() throws IOException {
		final var headers = new HttpHeaders();
		headers.add("Upload-Token", "uON60I7XWTIN");
		headers.add("Email", "@user10");
		final var response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
	}

	@Test
	public void getMyProfileWithBasicAuthUploadToken() throws IOException {
		final var response = restTemplate.withBasicAuth("@user10", "uON60I7XWTIN")
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
	}

	@Test
	public void getMyProfileWithBasicAuthPassword() throws IOException {
		final var response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertProfile(response, "@user27", "https://www.example.com/user27", false, null);
	}

	@Test
	public void getMyProfileWithBasicAuthPasswordFail() {
		final var response = restTemplate.withBasicAuth("@user27", "blahblubb")
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(401);
	}

	private void assertProfile(final ResponseEntity<String> response, final String name, final String link, final boolean anonymous, final String email) throws IOException {
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode.get("nickname").asText()).isEqualTo(name);
		if (email != null) {
			assertThat(jsonNode.get("email").asText()).isEqualTo(email);
		} else {
			assertThat(jsonNode.get("email")).isNull();
		}
		assertThat(jsonNode.get("link").asText()).isEqualTo(link);
		assertThat(jsonNode.get("license").asText()).isEqualTo("CC0 1.0 Universell (CC0 1.0)");
		assertThat(jsonNode.get("photoOwner").asBoolean()).isEqualTo(true);
		assertThat(jsonNode.get("anonymous").asBoolean()).isEqualTo(anonymous);
		assertThat(jsonNode.has("uploadToken")).isEqualTo(false);
	}

	@Test
	public void updateMyProfileAndChangePassword() throws IOException {
		final var firstPassword = "GDAkhaeU2vrK";
		final var headers = new HttpHeaders();
		headers.add("Upload-Token", firstPassword);
		headers.add("Email", "user14@example.com");
		final var responseGetBefore = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseGetBefore.getStatusCodeValue()).isEqualTo(200);
		assertThat(responseGetBefore.getBody()).isNotNull();
		assertProfile(responseGetBefore, "@user14", "https://www.example.com/user14", false, "user14@example.com");

		headers.setContentType(MediaType.APPLICATION_JSON);
		final var responsePostUpdate = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/myProfile"), new HttpEntity<>("""
						{
							"nickname": "user14",
							"email": "user14@example.com",
							"license": "CC0",
							"photoOwner": true,
							"anonymous": true
						}""", headers), String.class);
		assertThat(responsePostUpdate.getStatusCodeValue()).isEqualTo(200);
		assertThat(responsePostUpdate.getBody()).isNotNull();

		final var responseGetAfter = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseGetAfter.getStatusCodeValue()).isEqualTo(200);
		assertThat(responseGetAfter.getBody()).isNotNull();
		assertProfile(responseGetAfter, "user14", "", true, "user14@example.com");


		final var secondPassword = "!\"$%&/()=?-1234567890";
		changePassword(firstPassword, secondPassword, true, true);
		changePassword(secondPassword, "\\=oF`)X77__U}G", false, false);
	}

	public void changePassword(final String oldPassword, final String newPassword, final boolean authUploadToken, final boolean changePasswordViaHeader) {
		final var headers = new HttpHeaders();
		if (authUploadToken) {
			headers.add("Upload-Token", oldPassword);
			headers.add("Email", "user14@example.com");
		} else {
			headers.setBasicAuth("user14@example.com", oldPassword);
		}

		final HttpEntity<Object> changePasswordRequest;
		if (changePasswordViaHeader) {
			headers.add("New-Password", newPassword);
			changePasswordRequest = new HttpEntity<>(headers);
		} else {
			headers.setContentType(MediaType.APPLICATION_JSON);
			final var changePassword = mapper.createObjectNode();
			changePassword.set("newPassword", new TextNode(newPassword));
			changePasswordRequest = new HttpEntity<>(changePassword, headers);
		}

		final var responseChangePassword = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/changePassword"), changePasswordRequest, String.class);
		assertThat(responseChangePassword.getStatusCodeValue()).isEqualTo(200);

		final var responseAfterChangedPassword = restTemplate
				.withBasicAuth("user14@example.com", newPassword)
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);
		assertThat(responseAfterChangedPassword.getStatusCodeValue()).isEqualTo(200);

		final var responseWithOldPassword = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseWithOldPassword.getStatusCodeValue()).isEqualTo(401);
	}

}
