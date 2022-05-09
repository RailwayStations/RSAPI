package org.railwaystations.rsapi.app;

import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.railwaystations.rsapi.adapter.in.web.model.StationDto;
import org.railwaystations.rsapi.adapter.out.monitoring.LoggingMonitor;
import org.railwaystations.rsapi.adapter.out.photostorage.WorkDir;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.out.Monitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.servlet.Filter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"server.error.include-message=always"})
@ActiveProfiles("test")
class RsapiIntegrationTests {

	@Autowired
	private ObjectMapper mapper;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private WorkDir workDir;

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
		final var stations = assertLoadStationsOk("/stations");
		assertThat(stations.length).isEqualTo(954);
		assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
		assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNotNull();
	}

	@Test
	public void stationById() {
		final var station = loadStationDe6932();
		assertThat(station.getIdStr()).isEqualTo("6932");
		assertThat(station.getTitle()).isEqualTo( "Wuppertal-Ronsdorf");
		assertThat(station.getPhotoUrl()).isEqualTo("https://api.railway-stations.org/photos/de/6932.jpg");
		assertThat(station.getPhotographer()).isEqualTo("@user10");
		assertThat(station.getLicense()).isEqualTo("CC0 1.0 Universell (CC0 1.0)");
		assertThat(station.getActive()).isTrue();
		assertThat(station.getOutdated()).isFalse();
	}

	@Test
	public void outdatedStationById() {
		final var station = loadStationByKey("de", "7051");
		assertThat(station.getCountry()).isEqualTo("de");
		assertThat(station.getIdStr()).isEqualTo("7051");
		assertThat(station.getOutdated()).isTrue();
	}

	@Test
	public void stationByIdNotFound() {
		loadRaw("/de/stations/11111111111", 404, String.class);
	}

	@Test
	public void stationsDe() {
		final var stations = assertLoadStationsOk("/de/stations");
		assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
		assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNull();
	}

	@Test
	public void stationsDeQueryParam() {
		final var stations = assertLoadStationsOk(String.format("/%s?country=de", "stations"));
		assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
		assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNull();
	}

	@Test
	public void stationsDeChQueryParam() {
		final var stations = assertLoadStationsOk(String.format("/%s?country=de&country=ch", "stations"));
		assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
		assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNotNull();
	}

	@Test
	public void stationsDePhotograph() {
		final var stations = assertLoadStationsOk(String.format("/de/%s?photographer=@user10", "stations"));
		assertThat(findByKey(stations, new Station.Key("de", "6966"))).isNotNull();
	}

	@Test
	public void stationsCh() {
		final var stations = assertLoadStationsOk(String.format("/ch/%s", "stations"));
		assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNotNull();
		assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNull();
	}

	@Test
	public void stationsUnknownCountry() {
		final var stations = assertLoadStationsOk("/jp/stations");
		assertThat(stations.length).isEqualTo(0);
	}

	@Test
	public void stationsDeFromAnonym() {
		final var stations = assertLoadStationsOk("/de/stations?photographer=Anonym");
		assertThat(stations.length).isEqualTo(9);
	}

	@Test
	public void stationsDeFromDgerkrathWithinMax5km() {
		final var stations = assertLoadStationsOk("/de/stations?maxDistance=5&lat=49.0065325041363&lon=13.2770955562592&photographer=@user27");
		assertThat(stations.length).isEqualTo(2);
	}

	@Test
	public void stationsJson() throws IOException {
		final var response = loadRaw("/de/stations.json", 200, String.class);
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode).isNotNull();
		assertThat(jsonNode.isArray()).isTrue();
		assertThat(jsonNode.size()).isEqualTo(729);
	}

	@Test
	public void stationsGpx() throws IOException, ParserConfigurationException, SAXException {
		final var response = loadRaw(String.format("/ch/%s.gpx?hasPhoto=true", "stations"), 200, String.class);
		final var factory = DocumentBuilderFactory.newInstance();
		final var builder = factory.newDocumentBuilder();
		final var content = readSaveStringEntity(response);
		final var doc = builder.parse(new InputSource(new StringReader(content)));
		final var gpx = doc.getDocumentElement();
		assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/gpx+xml");
		assertThat(gpx.getTagName()).isEqualTo("gpx");
		assertThat(gpx.getAttribute("xmlns")).isEqualTo("http://www.topografix.com/GPX/1/1");
		assertThat(gpx.getAttribute("version")).isEqualTo("1.1");
		final var wpts = gpx.getElementsByTagName("wpt");
		assertThat(wpts.getLength()).isEqualTo(7);
	}

	private String readSaveStringEntity(final ResponseEntity<String> response) {
		return response.getBody();
	}

	private StationDto[] assertLoadStationsOk(final String path) {
		final var response = loadRaw(path, 200, StationDto[].class);

		if (response.getStatusCodeValue() != 200) {
			return new StationDto[0];
		}
		return response.getBody();
	}

	private <T> ResponseEntity<T>  loadRaw(final String path, final int expectedStatus, final Class<T> responseType) {
		final var response = restTemplate.getForEntity(String.format("http://localhost:%d%s", port, path),
				responseType);

		assertThat(response.getStatusCodeValue()).isEqualTo(expectedStatus);
		return response;
	}

	private StationDto findByKey(final StationDto[] stations, final Station.Key key) {
		return Arrays.stream(stations).filter(station -> station.getCountry().equals(key.getCountry()) && station.getIdStr().equals(key.getId())).findAny().orElse(null);
	}

	@Test
	public void photographersDeJson() throws IOException {
		final var response = loadRaw("/de/photographers.json", 200, String.class);
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode).isNotNull();
		assertThat(jsonNode.isObject()).isTrue();
		assertThat(jsonNode.size()).isEqualTo(4);
		assertThat(jsonNode.get("@user27").asInt()).isEqualTo(31);
		assertThat(jsonNode.get("@user8").asInt()).isEqualTo(29);
		assertThat(jsonNode.get("@user10").asInt()).isEqualTo(15);
		assertThat(jsonNode.get("@user0").asInt()).isEqualTo(9);
	}

	@Test
	public void photographersDeTxt() {
		final var response = loadRaw("/de/photographers.txt", 200, String.class);
		assertThat(response.getBody()).isEqualTo("""
				count	photographer
				31	@user27
				29	@user8
				15	@user10
				9	@user0
				""");
	}

	private StationDto loadStationDe6932() {
		return loadStationByKey("de", "6932");
	}

	private StationDto loadStationByKey(final String countryCode, final String id) {
		return loadRaw("/" + countryCode + "/stations/" + id, 200, StationDto.class).getBody();
	}

	@Test
	public void statisticDeJson() throws IOException {
		final var response = loadRaw("/de/stats.json", 200, String.class);
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode).isNotNull();
		assertThat(jsonNode.get("total").asInt()).isEqualTo(729);
		assertThat(jsonNode.get("withPhoto").asInt()).isEqualTo(84);
		assertThat(jsonNode.get("withoutPhoto").asInt()).isEqualTo(645);
		assertThat(jsonNode.get("photographers").asInt()).isEqualTo(4);
		assertThat(jsonNode.get("countryCode").asText()).isEqualTo("de");
	}

	@Test
	public void photoUploadForbidden() {
		final var headers = new HttpHeaders();
		headers.add("Upload-Token", "edbfc44727a6fd4f5b029aff21861a667a6b4195");
		headers.add("Nickname", "nickname");
		headers.add("Email", "nickname@example.com");
		headers.add("Station-Id", "4711");
		headers.add("Country", "de");
		headers.setContentType(MediaType.IMAGE_JPEG);
		final var request = new HttpEntity<>(IMAGE, headers);
		final var response = restTemplate.postForEntity(
				String.format("http://localhost:%d%s", port, "/photoUpload"), request, String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(401);
	}

	private final byte[] IMAGE = Base64.getDecoder().decode("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=");

	@Test
	public void photoUploadUnknownStationThenDeletePhotoThenDeleteStation() throws IOException {
		final var headers = new HttpHeaders();
		headers.add("Station-Title", URLEncoder.encode("Hintertupfingen", StandardCharsets.UTF_8.toString()));
		headers.add("Latitude", "50.123");
		headers.add("Longitude", "9.123");
		headers.add("Comment", "Missing Station");
		headers.setContentType(MediaType.IMAGE_JPEG);
		final var request = new HttpEntity<>(IMAGE, headers);
		final var uploadResponse = restTemplateWithBasicAuthUser10().postForEntity(
				String.format("http://localhost:%d%s", port, "/photoUpload"), request, String.class);

		assertThat(uploadResponse.getStatusCodeValue()).isEqualTo(202);
		final var inboxResponse = mapper.readTree(uploadResponse.getBody());
		final var uploadId = inboxResponse.get("id").asInt();
		final var filename = inboxResponse.get("filename").asText();
		assertThat(filename).isNotBlank();
		assertThat(inboxResponse.get("crc32").asLong()).isEqualTo(312729961L);

		// download uploaded photo from inbox
		final var photoResponse = restTemplate.getForEntity(
				String.format("http://localhost:%d%s%s", port, "/inbox/", filename), byte[].class);
		final var inputImage = ImageIO.read(new ByteArrayInputStream(Objects.requireNonNull(photoResponse.getBody())));
		assertThat(inputImage).isNotNull();
		// we cannot binary compare the result anymore, the photos are re-encoded
		// assertThat(IOUtils.readFully((InputStream)photoResponse.getEntity(), IMAGE.length)).isEqualTo(IMAGE));

		// simulate VsionAI
		Files.move(workDir.getInboxToProcessDir().resolve(filename), workDir.getInboxProcessedDir().resolve(filename));

		// get nextZ value for stationId
		final var nextZResponse = restTemplateWithBasicAuthUser10().getForEntity(
				String.format("http://localhost:%d%s", port, "/nextZ"), String.class);
		final var nextZResponseJson = mapper.readTree(nextZResponse.getBody());
		final var stationId = nextZResponseJson.get("nextZ").asText();



		// send import command
		sendInboxCommand("{\"id\": " + uploadId + ", \"stationId\": \"" + stationId + "\", \"countryCode\": \"de\", \"command\": \"IMPORT\", \"createStation\": true}");

		// assert station is imported
		final var newStation = loadStationByKey("de", stationId);
		assertThat(newStation.getTitle()).isEqualTo("Hintertupfingen");
		assertThat(newStation.getLat()).isEqualTo(50.123);
		assertThat(newStation.getLon()).isEqualTo(9.123);
		assertThat(newStation.getPhotographer()).isEqualTo("@user10");
		assertThat(newStation.getPhotoUrl()).isNotNull();

		// send problem report wrong photo
		final var problemReportWrongPhotoJson = """
				{
					"countryCode": "de",
					"stationId": "%s",
					"type": "WRONG_PHOTO",
					"comment": "This photo is clearly wrong"
				}""".formatted(stationId);
		final int idWrongPhoto = sendProblemReport(problemReportWrongPhotoJson);
		sendInboxCommand("{\"id\": " + idWrongPhoto + ", \"command\": \"DELETE_PHOTO\"}");

		// assert station has no photo anymore
		final var deletedPhotoStation = loadStationByKey("de", stationId);
		assertThat(deletedPhotoStation.getPhotoUrl()).isNull();

		// send problem report station not existing
		final var problemReportStationNonExistentJson = """
				{
					"countryCode": "de",
					"stationId": "%s",
					"type": "STATION_NONEXISTENT",
					"comment": "This photo is clearly wrong"
				}""".formatted(stationId);
		final int idStationNonExistent = sendProblemReport(problemReportStationNonExistentJson);
		sendInboxCommand("{\"id\": " + idStationNonExistent + ", \"command\": \"DELETE_STATION\"}");

		// assert station doesn't exist anymore
		loadRaw("/de/stations/" + stationId, 404, StationDto.class);
	}

	@Test
	public void getInboxWithBasicAuthPasswordFail() {
		final var response = restTemplate.withBasicAuth("@user27", "blahblubb")
				.getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(401);
	}

	@Test
	public void problemReportWithWrongLocation() throws JsonProcessingException {
		assertCoordinatesOfStation6815(51.5764217543438, 11.281417285);

		final var problemReportJson = """
				{
					"countryCode": "de",
					"stationId": "6815",
					"type": "WRONG_LOCATION",
					"comment": "Station at wrong location",
					"lat": 51.123,
					"lon": 11.123
				}""";
		final int id = sendProblemReport(problemReportJson);
		sendInboxCommand("{\"id\": " + id + ", \"command\": \"UPDATE_LOCATION\"}");

		assertCoordinatesOfStation6815(51.123, 11.123);
	}

	private int sendProblemReport(final String problemReportJson) throws JsonProcessingException {
		final var responsePostProblem = restTemplateWithBasicAuthUser10()
				.postForEntity(String.format("http://localhost:%d%s", port, "/reportProblem"), new HttpEntity<>(problemReportJson, createJsonHeaders()), String.class);
		assertThat(responsePostProblem.getStatusCodeValue()).isEqualTo(202);
		final var jsonNodePostProblemReponse = mapper.readTree(responsePostProblem.getBody());
		assertThat(jsonNodePostProblemReponse).isNotNull();
		assertThat(jsonNodePostProblemReponse.get("state").asText()).isEqualTo("REVIEW");
		return jsonNodePostProblemReponse.get("id").asInt();
	}

	private void assertCoordinatesOfStation6815(final double lat, final double lon) {
		final var station = loadStationByKey("de", "6815");
		assertThat(station).isNotNull();
		assertThat(station.getLat()).isEqualTo(lat);
		assertThat(station.getLon()).isEqualTo(lon);
	}

	@Test
	public void problemReportWithOutdatedPhoto() throws JsonProcessingException {
		assertOutdatedPhotoOfStation7065(false);

		final var problemReportJson = """
				{
					"countryCode": "de",
					"stationId": "7065",
					"type": "PHOTO_OUTDATED",
					"comment": "Photo is outdated"
				}""";
		final var id = sendProblemReport(problemReportJson);
		sendInboxCommand("{\"id\": " + id + ", \"command\": \"PHOTO_OUTDATED\"}");

		assertOutdatedPhotoOfStation7065(true);
	}

	@NotNull
	private HttpHeaders createJsonHeaders() {
		final var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
		return headers;
	}

	private void sendInboxCommand(final String inboxCommand) throws JsonProcessingException {
		final var responseInboxCommand = restTemplateWithBasicAuthUser10()
				.postForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), new HttpEntity<>(inboxCommand, createJsonHeaders()), String.class);
		assertThat(responseInboxCommand.getStatusCodeValue()).isEqualTo(200);
		final var jsonNodeInboxCommandReponse = mapper.readTree(responseInboxCommand.getBody());
		assertThat(jsonNodeInboxCommandReponse).isNotNull();
		assertThat(jsonNodeInboxCommandReponse.get("status").asInt()).isEqualTo(200);
		assertThat(jsonNodeInboxCommandReponse.get("message").asText()).isEqualTo("ok");
	}

	private void assertOutdatedPhotoOfStation7065(final boolean outdated) {
		final var station = loadStationByKey("de", "7065");
		assertThat(station).isNotNull();
		assertThat(station.getOutdated()).isEqualTo(outdated);
	}


	private TestRestTemplate restTemplateWithBasicAuthUser10() {
		return restTemplate.withBasicAuth("@user10", "uON60I7XWTIN");
	}

	@Test
	public void getInboxWithBasicAuthNotAuthorized() {
		final var response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
				.getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(403);
	}

	@Test
	public void getInboxWithBasicAuth() throws JsonProcessingException {
		final var response = restTemplateWithBasicAuthUser10()
				.getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode).isNotNull();
		assertThat(jsonNode.isArray()).isTrue();
	}

	@Test
	public void postAdminInboxCommandWithUnknownInboxExntry() throws JsonProcessingException {
		final HttpHeaders headers = createJsonHeaders();
		final var response = restTemplateWithBasicAuthUser10()
				.postForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), new HttpEntity<>("{\"id\": -1, \"command\": \"IMPORT\"}", headers), String.class);

		assertThat(response.getStatusCodeValue()).isEqualTo(400);
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode.get("status").asInt()).isEqualTo(400);
		assertThat(jsonNode.get("message").asText()).isEqualTo("No pending inbox entry found");
	}

	@ParameterizedTest
	@ValueSource(strings = {"/countries", "/countries.json"})
	public void countries(final String path) throws IOException {
		final var response = loadRaw(path, 200, String.class);
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode).isNotNull();
		assertThat(jsonNode.isArray()).isTrue();
		assertThat(jsonNode.size()).isEqualTo(2);

		final var foundCountries = new AtomicInteger();
		jsonNode.forEach(node->{
			switch (node.get("code").asText()) {
				case "de" -> {
					assertThat(node.get("name").asText()).isEqualTo("Deutschland");
					assertThat(node.get("twitterTags").asText()).isEqualTo("@Bahnhofsoma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn");
					assertThat(node.get("providerApps").size()).isEqualTo(3);
					assertProviderApp(node, 0, "android", "DB Navigator", "https://play.google.com/store/apps/details?id=de.hafas.android.db");
					assertProviderApp(node, 1, "android", "FlixTrain", "https://play.google.com/store/apps/details?id=de.meinfernbus");
					assertProviderApp(node, 2, "ios", "DB Navigator", "https://apps.apple.com/app/db-navigator/id343555245");
					foundCountries.getAndIncrement();
				}
				case "ch" -> {
					assertThat(node.get("name").asText()).isEqualTo("Schweiz");
					assertThat(node.get("twitterTags").asText()).isEqualTo("@BahnhoefeCH, @Bahnhofsoma, #BahnhofsfotoCH");
					assertThat(node.get("providerApps").size()).isEqualTo(2);
					assertProviderApp(node, 0, "android", "SBB Mobile", "https://play.google.com/store/apps/details?id=ch.sbb.mobile.android.b2c");
					assertProviderApp(node, 1, "ios", "SBB Mobile", "https://apps.apple.com/app/sbb-mobile/id294855237");
					foundCountries.getAndIncrement();
				}
			}
		});

		assertThat(foundCountries.get()).isEqualTo(2);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/countries", "/countries.json"})
	public void countriesAll(final String path) throws IOException {
		final var response = loadRaw(path + "?onlyActive=false", 200, String.class);
		final var jsonNode = mapper.readTree(response.getBody());
		assertThat(jsonNode).isNotNull();
		assertThat(jsonNode.isArray()).isTrue();
		assertThat(jsonNode.size()).isEqualTo(4);
	}

	private void assertProviderApp(final JsonNode countryNode, final int i, final String type, final String name, final String url) {
		final var app = countryNode.get("providerApps").get(i);
		assertThat(app.get("type").asText()).isEqualTo(type);
		assertThat(app.get("name").asText()).isEqualTo(name);
		assertThat(app.get("url").asText()).isEqualTo(url);
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

		@Bean
		public Filter openApiValidationFilter() {
			return new OpenApiValidationFilter(true, true);
		}

		@Bean
		public WebMvcConfigurer addOpenApiValidationInterceptor(@Value("classpath:static/openapi.yaml") final Resource apiSpecification) throws IOException {
			final EncodedResource specResource = new EncodedResource(apiSpecification, StandardCharsets.UTF_8);
			final OpenApiValidationInterceptor openApiValidationInterceptor = new OpenApiValidationInterceptor(specResource);
			return new WebMvcConfigurer() {
				@Override
				public void addInterceptors(final @NotNull InterceptorRegistry registry) {
					registry.addInterceptor(openApiValidationInterceptor);
				}
			};
		}
	}

}
