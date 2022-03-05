package org.railwaystations.rsapi.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.railwaystations.rsapi.adapter.monitoring.LoggingMonitor;
import org.railwaystations.rsapi.adapter.photostorage.WorkDir;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.Mailer;
import org.railwaystations.rsapi.core.ports.Monitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"server.error.include-message=always"})
@ActiveProfiles("test")
class RsapiApplicationTests {

	private static final ObjectMapper MAPPER = new ObjectMapper();

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
	public void stationsAllCountries() {
		final Station[] stations = assertLoadStationsOk("/stations");
		assertThat(stations.length, is(954));
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), notNullValue());
	}

	@Test
	public void stationById() {
		final Station station = getStationDe6932();
		assertThat(station.getKey().getId(), is("6932"));
		assertThat(station.getTitle(), is( "Wuppertal-Ronsdorf"));
		assertThat(station.getPhotoUrl(), is("https://api.railway-stations.org/photos/de/6932.jpg"));
		assertThat(station.getPhotographer(), is("@user10"));
		assertThat(station.getLicense(), is("CC0 1.0 Universell (CC0 1.0)"));
		assertThat(station.isActive(), is(true));
	}

	@Test
	public void stationByIdNotFound() {
		loadRaw("/de/stations/11111111111", 404, String.class);
	}

	@Test
	public void stationsDe() {
		final Station[] stations = assertLoadStationsOk(String.format("/de/%s", "stations"));
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), nullValue());
	}

	@Test
	public void stationsDeQueryParam() {
		final Station[] stations = assertLoadStationsOk(String.format("/%s?country=de", "stations"));
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), nullValue());
	}

	@Test
	public void stationsDeChQueryParam() {
		final Station[] stations = assertLoadStationsOk(String.format("/%s?country=de&country=ch", "stations"));
		assertThat(findByKey(stations, new Station.Key("de", "6721")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), notNullValue());
	}

	@Test
	public void stationsDePhotograph() {
		final Station[] stations = assertLoadStationsOk(String.format("/de/%s?photographer=@user10", "stations"));
		assertThat(findByKey(stations, new Station.Key("de", "6966")), notNullValue());
	}

	@Test
	public void stationsCh() {
		final Station[] stations = assertLoadStationsOk(String.format("/ch/%s", "stations"));
		assertThat(findByKey(stations, new Station.Key("ch", "8500126")), notNullValue());
		assertThat(findByKey(stations, new Station.Key("de", "6721")), nullValue());
	}

	@Test
	public void stationsUnknownCountry() {
		final Station[] stations = assertLoadStationsOk("/jp/stations");
		assertThat(stations.length, is(0));
	}

	@Test
	public void stationsDeFromAnonym() {
		final Station[] stations = assertLoadStationsOk("/de/stations?photographer=Anonym");
		assertThat(stations.length, is(9));
	}

	@Test
	public void stationsDeFromDgerkrathWithinMax5km() {
		final Station[] stations = assertLoadStationsOk("/de/stations?maxDistance=5&lat=49.0065325041363&lon=13.2770955562592&photographer=@user27");
		assertThat(stations.length, is(2));
	}

	@Test
	public void stationsJson() throws IOException {
		final ResponseEntity<String> response = loadRaw("/de/stations.json", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isArray(), is(true));
		assertThat(jsonNode.size(), is(729));
	}

	@Test
	public void stationsGpx() throws IOException, ParserConfigurationException, SAXException {
		final ResponseEntity<String> response = loadRaw(String.format("/ch/%s.gpx?hasPhoto=true", "stations"), 200, String.class);
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final String content = readSaveStringEntity(response);
		final Document doc = builder.parse(new InputSource(new StringReader(content)));
		final Element gpx = doc.getDocumentElement();
		assertThat(response.getHeaders().getFirst("Content-Type"), is("application/gpx+xml"));
		assertThat(gpx.getTagName(), is("gpx"));
		assertThat(gpx.getAttribute("xmlns"), is("http://www.topografix.com/GPX/1/1"));
		assertThat(gpx.getAttribute("version"), is("1.1"));
		final NodeList wpts = gpx.getElementsByTagName("wpt");
		assertThat(wpts.getLength(), is(7));
	}

	private String readSaveStringEntity(final ResponseEntity<String> response) {
		return response.getBody();
	}

	private Station[] assertLoadStationsOk(final String path) {
		final ResponseEntity<Station[]> response = loadRaw(path, 200, Station[].class);

		if (response.getStatusCodeValue() != 200) {
			return new Station[0];
		}
		return response.getBody();
	}

	private <T> ResponseEntity<T>  loadRaw(final String path, final int expectedStatus, final Class<T> responseType) {
		final ResponseEntity<T> response = restTemplate.getForEntity(String.format("http://localhost:%d%s", port, path),
				responseType);

		assertThat(response.getStatusCodeValue(), is(expectedStatus));
		return response;
	}

	private Station findByKey(final Station[] stations, final Station.Key key) {
		return Arrays.stream(stations).filter(station -> station.getKey().equals(key)).findAny().orElse(null);
	}

	@Test
	public void photographersDeJson() throws IOException {
		final ResponseEntity<String> response = loadRaw(String.format("/de/%s.json", "photographers"), 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isObject(), is(true));
		assertThat(jsonNode.size(), is(4));
		assertThat(jsonNode.get("@user27").asInt(), is(31));
		assertThat(jsonNode.get("@user8").asInt(), is(29));
		assertThat(jsonNode.get("@user10").asInt(), is(15));
		assertThat(jsonNode.get("@user0").asInt(), is(9));
	}

	@Test
	public void photographersAllJson() throws IOException {
		final ResponseEntity<String> response = loadRaw("/photographers.json", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.size(), is(6));
		assertThat(jsonNode.get("@user27").asInt(), is(31));
		assertThat(jsonNode.get("@user8").asInt(), is(29));
		assertThat(jsonNode.get("@user10").asInt(), is(15));
		assertThat(jsonNode.get("@user0").asInt(), is(9));
		assertThat(jsonNode.get("@user2").asInt(), is(6));
		assertThat(jsonNode.get("@user4").asInt(), is(1));
	}

	@Test
	public void photographersTxt() {
		final ResponseEntity<String> response = loadRaw("/de/photographers.txt", 200, String.class);
		assertThat(response.getBody(), is("""
					count	photographer
					31	@user27
					29	@user8
					15	@user10
					9	@user0
					"""));
	}

	private Station getStationDe6932() {
		return loadRaw("/de/stations/6932", 200, Station.class).getBody();
	}

	@Test
	public void statisticAllJson() throws IOException {
		final ResponseEntity<String> response = loadRaw("/stats.json", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.get("total").asInt(), is(954));
		assertThat(jsonNode.get("withPhoto").asInt(), is(91));
		assertThat(jsonNode.get("withoutPhoto").asInt(), is(863));
		assertThat(jsonNode.get("photographers").asInt(), is(6));
		assertThat(jsonNode.get("countryCode").isNull(), is(true));
	}

	@Test
	public void statisticDeJson() throws IOException {
		final ResponseEntity<String> response = loadRaw("/de/stats.json", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.get("total").asInt(), is(729));
		assertThat(jsonNode.get("withPhoto").asInt(), is(84));
		assertThat(jsonNode.get("withoutPhoto").asInt(), is(645));
		assertThat(jsonNode.get("photographers").asInt(), is(4));
		assertThat(jsonNode.get("countryCode").asText(), is("de"));
	}

	@Test
	public void statisticDeTxt() {
		final ResponseEntity<String> response = loadRaw("/de/stats.txt", 200, String.class);
		assertThat(response.getBody(), is(
      				"""
						name	value
						total	729
						withPhoto	84
						withoutPhoto	645
						photographers	4
						countryCode	de
							"""));
	}

	@Test
	public void register() {
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		final ResponseEntity<String> response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/registration"), new HttpEntity<>("""
						{
						\t"nickname": "nickname ",\s
						\t"email": "nick.name@example.com",\s
						\t"license": "CC0",
						\t"photoOwner": true,\s
						\t"link": ""
						}""", headers), String.class);

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
	public void photoUploadForbidden() {
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Upload-Token", "edbfc44727a6fd4f5b029aff21861a667a6b4195");
		headers.add("Nickname", "nickname");
		headers.add("Email", "nickname@example.com");
		headers.add("Station-Id", "4711");
		headers.add("Country", "de");
		headers.setContentType(MediaType.IMAGE_JPEG);
		final HttpEntity<String> request = new HttpEntity<>("", headers);
		final ResponseEntity<String> response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/photoUpload"), request, String.class);

		assertThat(response.getStatusCodeValue(), is(401));
	}

	private final byte[] IMAGE = Base64.getDecoder().decode("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=");

	@Test
	public void photoUploadUnknownStation() throws IOException {
		final HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth("@user10", "uON60I7XWTIN");
		headers.add("Station-Title", URLEncoder.encode("Achères-Grand-Cormier", StandardCharsets.UTF_8.toString()));
		headers.add("Latitude", "50.123");
		headers.add("Longitude", "10.123");
		headers.add("Comment", "Missing Station");
		headers.setContentType(MediaType.IMAGE_JPEG);
		final HttpEntity<byte[]> request = new HttpEntity<>(IMAGE, headers);
		final ResponseEntity<String> response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/photoUpload"), request, String.class);

		assertThat(response.getStatusCodeValue(), is(202));
		final JsonNode inboxResponse = MAPPER.readTree(response.getBody());
		assertThat(inboxResponse.get("id"), notNullValue());
		assertThat(inboxResponse.get("filename"), notNullValue());
		assertThat(inboxResponse.get("crc32").asLong(), is(312729961L));

		// download uploaded photo from inbox
		final ResponseEntity<byte[]> photoResponse = restTemplate.getForEntity(
				String.format("http://localhost:%d%s%s", port, "/inbox/", inboxResponse.get("filename").asText()), byte[].class);
		final BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(Objects.requireNonNull(photoResponse.getBody())));
		assertThat(inputImage, notNullValue());
		// we cannot binary compare the result anymore, the photos are re-encoded
		// assertThat(IOUtils.readFully((InputStream)photoResponse.getEntity(), IMAGE.length), is(IMAGE));
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

	@Test
	public void getInboxWithBasicAuthPasswordFail() {
		final ResponseEntity<String> response = restTemplate.withBasicAuth("@user27", "blahblubb")
				.getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

		assertThat(response.getStatusCodeValue(), is(401));
	}

	@Test
	public void getInboxWithBasicAuthNotAuthorized() {
		final ResponseEntity<String> response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
				.getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

		assertThat(response.getStatusCodeValue(), is(403));
	}

	@Test
	public void getInboxWithBasicAuth() throws JsonProcessingException {
		final ResponseEntity<String> response = restTemplate.withBasicAuth("@user10", "uON60I7XWTIN")
				.getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

		assertThat(response.getStatusCodeValue(), is(200));
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isArray(), is(true));
	}

	@Test
	public void postAdminInboxCommandWithUnknownInboxExntry() throws JsonProcessingException {
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		final ResponseEntity<String> response = restTemplate.withBasicAuth("@user10", "uON60I7XWTIN")
				.postForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), new HttpEntity<>("{\"id\": -1, \"command\": \"IMPORT\"}", headers), String.class);

		assertThat(response.getStatusCodeValue(), is(400));
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode.get("status").asInt(), is(400));
		assertThat(jsonNode.get("message").asText(), is("No pending inbox entry found"));
	}

	private void assertProfile(final ResponseEntity<String> response, final String name, final String link, final boolean anonymous, final String email) throws IOException {
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
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
			final ObjectNode changePassword = MAPPER.createObjectNode();
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

	@ParameterizedTest
	@ValueSource(strings = {"/countries", "/countries.json"})
	public void countries(final String path) throws IOException {
		final ResponseEntity<String> response = loadRaw(path, 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isArray(), is(true));
		assertThat(jsonNode.size(), is(2));

		final AtomicInteger foundCountries = new AtomicInteger();
		jsonNode.forEach(node->{
			final String country = node.get("code").asText();
			switch (country) {
				case "de" -> {
					assertThat(node.get("code").asText(), is("de"));
					assertThat(node.get("name").asText(), is("Deutschland"));
					assertThat(node.get("providerApps").size(), is(3));
					assertProviderApp(node, 0, "android", "DB Navigator", "https://play.google.com/store/apps/details?id=de.hafas.android.db");
					assertProviderApp(node, 1, "android", "FlixTrain", "https://play.google.com/store/apps/details?id=de.meinfernbus");
					assertProviderApp(node, 2, "ios", "DB Navigator", "https://apps.apple.com/app/db-navigator/id343555245");
					foundCountries.getAndIncrement();
				}
				case "ch" -> {
					assertThat(node.get("name").asText(), is("Schweiz"));
					assertThat(node.get("providerApps").size(), is(2));
					assertProviderApp(node, 0, "android", "SBB Mobile", "https://play.google.com/store/apps/details?id=ch.sbb.mobile.android.b2c");
					assertProviderApp(node, 1, "ios", "SBB Mobile", "https://apps.apple.com/app/sbb-mobile/id294855237");
					foundCountries.getAndIncrement();
				}
			}
		});

		assertThat(foundCountries.get(), is(2));
	}

	@ParameterizedTest
	@ValueSource(strings = {"/countries", "/countries.json"})
	public void countriesAll(final String path) throws IOException {
		final ResponseEntity<String> response = loadRaw(path + "?onlyActive=false", 200, String.class);
		final JsonNode jsonNode = MAPPER.readTree(response.getBody());
		assertThat(jsonNode, notNullValue());
		assertThat(jsonNode.isArray(), is(true));
		assertThat(jsonNode.size(), is(4));
	}

	private void assertProviderApp(final JsonNode countryNode, final int i, final String type, final String name, final String url) {
		final JsonNode app = countryNode.get("providerApps").get(i);
		assertThat(app.get("type").asText(), is(type));
		assertThat(app.get("name").asText(), is(name));
		assertThat(app.get("url").asText(), is(url));
	}

	@TestConfiguration
	static class SpringConfig {
		private final String TMP_WORK_DIR = createTempWorkDir();

		@Bean
		public WorkDir workDir() {
            return new WorkDir(TMP_WORK_DIR, null);
		}

		@Bean
		public Monitor monitor() {
			return new LoggingMonitor();
		}

		private String createTempWorkDir() {
			try {
				return Files.createTempDirectory("workDir-" + System.currentTimeMillis()).toFile().getAbsolutePath();
			} catch (final IOException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
