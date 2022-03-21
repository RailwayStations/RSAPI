package org.railwaystations.rsapi.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.rsapi.core.ports.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;

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
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		final ResponseEntity<String> response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/registration"), new HttpEntity<>("""
						{
						"nickname": "nickname ",
						"email": "nick.name@example.com",
						"license": "CC0",
						"photoOwner": true,
						"link": ""
						}
						""", headers), String.class);

		assertThat(response.getStatusCodeValue(), is(202));

		Mockito.verify(mailer, Mockito.times(1))
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
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		final ResponseEntity<String> response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/registration"),new HttpEntity<>("""
						{
						\t"nickname": "user14",\s
						\t"email": "other@example.com",\s
						\t"license": "CC0",
						\t"photoOwner": true,\s
						\t"link": "link"
						}""", headers), String.class);

		assertThat(response.getStatusCodeValue(), is(409));
	}

	@Test
	public void getProfileForbidden() {
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Nickname", "nickname");
		headers.add("Email", "nickname@example.com");
		final ResponseEntity<String> response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

		assertThat(response.getStatusCodeValue(), is(401));
	}

	@Test
	public void getMyProfileWithEmail() throws IOException {
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Upload-Token", "uON60I7XWTIN");
		headers.add("Email", "user10@example.com");
		final ResponseEntity<String> response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

		assertThat(response.getStatusCodeValue(), is(200));
		assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
	}

	@Test
	public void getMyProfileWithName() throws IOException {
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Upload-Token", "uON60I7XWTIN");
		headers.add("Email", "@user10");
		final ResponseEntity<String> response = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

		assertThat(response.getStatusCodeValue(), is(200));
		assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
	}

	@Test
	public void getMyProfileWithBasicAuthUploadToken() throws IOException {
		final ResponseEntity<String> response = restTemplate.withBasicAuth("@user10", "uON60I7XWTIN")
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

		assertThat(response.getStatusCodeValue(), is(200));
		assertProfile(response, "@user10", "https://www.example.com/user10", false, "user10@example.com");
	}

	@Test
	public void getMyProfileWithBasicAuthPassword() throws IOException {
		final ResponseEntity<String> response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

		assertThat(response.getStatusCodeValue(), is(200));
		assertProfile(response, "@user27", "https://www.example.com/user27", false, null);
	}

	@Test
	public void getMyProfileWithBasicAuthPasswordFail() {
		final ResponseEntity<String> response = restTemplate.withBasicAuth("@user27", "blahblubb")
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);

		assertThat(response.getStatusCodeValue(), is(401));
	}

	private void assertProfile(final ResponseEntity<String> response, final String name, final String link, final boolean anonymous, final String email) throws IOException {
		final JsonNode jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode.get("nickname").asText(), is(name));
		if (email != null) {
			assertThat(jsonNode.get("email").asText(), is(email));
		} else {
			assertThat(jsonNode.get("email"), nullValue());
		}
		assertThat(jsonNode.get("link").asText(), is(link));
		assertThat(jsonNode.get("license").asText(), is("CC0 1.0 Universell (CC0 1.0)"));
		assertThat(jsonNode.get("photoOwner").asBoolean(), is(true));
		assertThat(jsonNode.get("anonymous").asBoolean(), is(anonymous));
		assertThat(jsonNode.has("uploadToken"), is(false));
	}

	@Test
	public void updateMyProfileAndChangePassword() throws IOException {
		final String firstPassword = "GDAkhaeU2vrK";
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Upload-Token", firstPassword);
		headers.add("Email", "user14@example.com");
		final ResponseEntity<String> responseGetBefore = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseGetBefore.getStatusCodeValue(), is(200));
		assertThat(responseGetBefore.getBody(), notNullValue());
		assertProfile(responseGetBefore, "@user14", "https://www.example.com/user14", false, "user14@example.com");

		headers.setContentType(MediaType.APPLICATION_JSON);
		final ResponseEntity<String> responsePostUpdate = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/myProfile"), new HttpEntity<>("""
						{
						\t"nickname": "user14",\s
						\t"email": "user14@example.com",\s
						\t"license": "CC0",
						\t"photoOwner": true,\s
						\t"anonymous": true
						}""", headers), String.class);
		assertThat(responsePostUpdate.getStatusCodeValue(), is(200));
		assertThat(responsePostUpdate.getBody(), notNullValue());

		final ResponseEntity<String> responseGetAfter = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseGetAfter.getStatusCodeValue(), is(200));
		assertThat(responseGetAfter.getBody(), notNullValue());
		assertProfile(responseGetAfter, "user14", "", true, "user14@example.com");


		final String secondPassword = "!\"$%&/()=?-1234567890";
		changePassword(firstPassword, secondPassword, true, true);
		changePassword(secondPassword, "\\=oF`)X77__U}G", false, false);
	}

	public void changePassword(final String oldPassword, final String newPassword, final boolean authUploadToken, final boolean changePasswordViaHeader) {
		final HttpHeaders headers = new HttpHeaders();
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
			final ObjectNode changePassword = mapper.createObjectNode();
			changePassword.set("newPassword", new TextNode(newPassword));
			changePasswordRequest = new HttpEntity<>(changePassword, headers);
		}

		final ResponseEntity<String> responseChangePassword = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/changePassword"), changePasswordRequest, String.class);
		assertThat(responseChangePassword.getStatusCodeValue(), is(200));

		final ResponseEntity<String> responseAfterChangedPassword = restTemplate
				.withBasicAuth("user14@example.com", newPassword)
				.getForEntity(String.format("http://localhost:%d%s", port, "/myProfile"), String.class);
		assertThat(responseAfterChangedPassword.getStatusCodeValue(), is(200));

		final ResponseEntity<String> responseWithOldPassword = restTemplate.exchange(String.format("http://localhost:%d%s", port, "/myProfile"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseWithOldPassword.getStatusCodeValue(), is(401));
	}

}
