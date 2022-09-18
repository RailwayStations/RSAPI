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
import org.railwaystations.rsapi.adapter.in.web.model.PhotoLicenseDto;
import org.railwaystations.rsapi.adapter.in.web.model.PhotoStationDto;
import org.railwaystations.rsapi.adapter.in.web.model.PhotoStationsDto;
import org.railwaystations.rsapi.adapter.in.web.model.PhotographerDto;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.imageio.ImageIO;
import javax.servlet.Filter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.error.include-message=always"})
@ActiveProfiles("test")
class RsapiIntegrationTests extends AbstractMariaDBBaseTest {

    @Autowired
    private ObjectMapper mapper;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WorkDir workDir;

    @Test
    void stationsAllCountries() {
        var stations = assertLoadStationsOk("/stations");
        assertThat(stations.length).isEqualTo(954);
        assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
        assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNotNull();
    }

    @Test
    void stationById() {
        var station = loadStationDe6932();
        assertThat(station.getIdStr()).isEqualTo("6932");
        assertThat(station.getTitle()).isEqualTo("Wuppertal-Ronsdorf");
        assertThat(station.getPhotoUrl()).isEqualTo("https://api.railway-stations.org/photos/de/6932.jpg");
        assertThat(station.getPhotographer()).isEqualTo("@user10");
        assertThat(station.getLicense()).isEqualTo("CC0 1.0 Universell (CC0 1.0)");
        assertThat(station.getActive()).isTrue();
        assertThat(station.getOutdated()).isFalse();
    }

    @Test
    void photoStationById() {
        var response = restTemplate.getForEntity(String.format("http://localhost:%d/photoStationById/de/6932", port),
                PhotoStationsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var photoStationsDto = response.getBody();
        assertThat(photoStationsDto).isNotNull();
        assertThat(photoStationsDto.getPhotoBaseUrl()).isEqualTo("https://api.railway-stations.org/photos");

        var licenses = photoStationsDto.getLicenses();
        assertThat(licenses).containsExactly(new PhotoLicenseDto().id("CC0_10").name("CC0 1.0 Universell (CC0 1.0)").url("https://creativecommons.org/publicdomain/zero/1.0/"));

        var photographers = photoStationsDto.getPhotographers();
        assertThat(photographers).containsExactly(new PhotographerDto().name("@user10").url("https://www.example.com/user10"));

        var station = photoStationsDto.getStations().get(0);
        assertThat(station.getCountry()).isEqualTo("de");
        assertThat(station.getId()).isEqualTo("6932");
        assertThat(station.getTitle()).isEqualTo("Wuppertal-Ronsdorf");
        assertThat(station.getShortCode()).isEqualTo("KWRO");
        assertThat(station.getInactive()).isFalse();

        var photo1 = station.getPhotos().get(0);
        assertThat(photo1.getId()).isEqualTo(24);
        assertThat(photo1.getPath()).isEqualTo("/de/6932.jpg");
        assertThat(photo1.getPhotographer()).isEqualTo("@user10");
        assertThat(photo1.getLicense()).isEqualTo("CC0_10");
        assertThat(photo1.getOutdated()).isFalse();
        // assertThat(photo1.getCreatedAt()).isEqualTo(1523037167000L); does fail on GitHub

        var photo2 = station.getPhotos().get(1);
        assertThat(photo2.getId()).isEqualTo(128);
        assertThat(photo2.getPath()).isEqualTo("/de/6932_2.jpg");
        assertThat(photo2.getPhotographer()).isEqualTo("@user10");
        assertThat(photo2.getLicense()).isEqualTo("CC0_10");
        assertThat(photo2.getOutdated()).isTrue();
        // assertThat(photo2.getCreatedAt()).isEqualTo(1659357923000L); does fail on GitHub
    }

    @Test
    void photoStationsByCountryDe() {
        var response = restTemplate.getForEntity(String.format("http://localhost:%d/photoStationsByCountry/de", port),
                PhotoStationsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var photoStationsDto = response.getBody();
        assertThat(photoStationsDto).isNotNull();
        assertThat(photoStationsDto.getStations())
                .anyMatch(photoStationDto -> photoStationDto.getCountry().equals("de") && photoStationDto.getId().equals("6721"));
        assertThat(photoStationsDto.getStations())
                .noneMatch(photoStationDto -> !photoStationDto.getCountry().equals("de"));

        // check if we can find the license and photographer of one stationphoto
        var stationWithPhoto = photoStationsDto.getStations().stream().filter(photoStationDto -> photoStationDto.getPhotos().size() > 0).findAny();
        assertThat(stationWithPhoto).isPresent();
        assertThat(photoStationsDto.getLicenses()).anyMatch(photoLicenseDto -> photoLicenseDto.getId().equals(stationWithPhoto.get().getPhotos().get(0).getLicense()));
        assertThat(photoStationsDto.getPhotographers()).anyMatch(photographerDto -> photographerDto.getName().equals(stationWithPhoto.get().getPhotos().get(0).getPhotographer()));
    }

    @Test
    void photoStationsByCountry_with_unknown_country() {
        var response = restTemplate.getForEntity(String.format("http://localhost:%d/photoStationsByCountry/00", port),
                PhotoStationsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var photoStationsDto = response.getBody();
        assertThat(photoStationsDto).isNotNull();
        assertThat(photoStationsDto.getLicenses()).isEmpty();
        assertThat(photoStationsDto.getPhotographers()).isEmpty();
        assertThat(photoStationsDto.getStations()).isEmpty();
    }

    @Test
    void photoStationsByPhotographerAndCountry() {
        var response = restTemplate.getForEntity(String.format("http://localhost:%d/photoStationsByPhotographer/@user10?country=de", port),
                PhotoStationsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var photoStationsDto = response.getBody();
        assertThat(photoStationsDto).isNotNull();

        var licenses = photoStationsDto.getLicenses();
        assertThat(licenses).containsExactlyInAnyOrder(new PhotoLicenseDto().id("CC0_10").name("CC0 1.0 Universell (CC0 1.0)").url("https://creativecommons.org/publicdomain/zero/1.0/"),
                new PhotoLicenseDto().id("CC_BY_SA_40").name("CC BY-SA 4.0").url("https://creativecommons.org/licenses/by-sa/4.0/"));

        var photographers = photoStationsDto.getPhotographers();
        assertThat(photographers).containsExactly(new PhotographerDto().name("@user10").url("https://www.example.com/user10"));
        assertThat(photographers).noneMatch(photographerDto -> !photographerDto.getName().equals("@user10"));
        assertThat(photoStationsDto.getStations()).noneMatch(stationDto -> !stationDto.getCountry().equals("de"));
        assertThat(photoStationsDto.getStations().stream().flatMap(photoStationDto -> photoStationDto.getPhotos().stream())).noneMatch(photoDto -> !photoDto.getPhotographer().equals("@user10"));

        var station = photoStationsDto.getStations().stream()
                .filter(photoStationDto -> photoStationDto.getCountry().equals("de") && photoStationDto.getId().equals("6932"))
                .findAny()
                .orElseThrow();
        assertThat(station.getCountry()).isEqualTo("de");
        assertThat(station.getId()).isEqualTo("6932");

        var photo1 = station.getPhotos().get(0);
        assertThat(photo1.getId()).isEqualTo(24L);
        assertThat(photo1.getPath()).isEqualTo("/de/6932.jpg");
        assertThat(photo1.getPhotographer()).isEqualTo("@user10");
        assertThat(photo1.getLicense()).isEqualTo("CC0_10");
        assertThat(photo1.getOutdated()).isFalse();

        var photo2 = station.getPhotos().get(1);
        assertThat(photo2.getId()).isEqualTo(128L);
        assertThat(photo2.getPath()).isEqualTo("/de/6932_2.jpg");
        assertThat(photo2.getPhotographer()).isEqualTo("@user10");
        assertThat(photo2.getLicense()).isEqualTo("CC0_10");
        assertThat(photo2.getOutdated()).isTrue();
    }

    @Test
    void photoStationsByPhotographerAnonymInAllCountries() {
        var response = restTemplate.getForEntity(String.format("http://localhost:%d/photoStationsByPhotographer/Anonym", port),
                PhotoStationsDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var photoStationsDto = response.getBody();
        assertThat(photoStationsDto).isNotNull();

        var licenses = photoStationsDto.getLicenses();
        assertThat(licenses).containsExactlyInAnyOrder(new PhotoLicenseDto().id("CC0_10").name("CC0 1.0 Universell (CC0 1.0)").url("https://creativecommons.org/publicdomain/zero/1.0/"));

        var photographers = photoStationsDto.getPhotographers();
        assertThat(photographers).containsExactly(new PhotographerDto().name("Anonym").url("https://railway-stations.org"));
        assertThat(photographers).noneMatch(photographerDto -> !photographerDto.getName().equals("Anonym"));
        assertThat(photoStationsDto.getStations().stream().map(PhotoStationDto::getCountry).collect(Collectors.toSet())).containsExactlyInAnyOrder("de", "ch");
        assertThat(photoStationsDto.getStations().stream().flatMap(photoStationDto -> photoStationDto.getPhotos().stream())).noneMatch(photoDto -> !photoDto.getPhotographer().equals("Anonym"));

        var station = photoStationsDto.getStations().stream()
                .filter(photoStationDto -> photoStationDto.getCountry().equals("de") && photoStationDto.getId().equals("6998"))
                .findAny()
                .orElseThrow();
        assertThat(station.getCountry()).isEqualTo("de");
        assertThat(station.getId()).isEqualTo("6998");

        var photo1 = station.getPhotos().get(0);
        assertThat(photo1.getId()).isEqualTo(54);
        assertThat(photo1.getPath()).isEqualTo("/de/6998_1.jpg");
        assertThat(photo1.getPhotographer()).isEqualTo("Anonym");
        assertThat(photo1.getLicense()).isEqualTo("CC0_10");
        assertThat(photo1.getOutdated()).isFalse();
    }

    @Test
    void outdatedStationById() {
        var station = loadDeStationByStationId("7051");
        assertThat(station.getCountry()).isEqualTo("de");
        assertThat(station.getIdStr()).isEqualTo("7051");
        assertThat(station.getOutdated()).isTrue();
    }

    @Test
    void stationByIdNotFound() {
        loadRaw("/de/stations/11111111111", 404, String.class);
    }

    @Test
    void stationsDe() {
        var stations = assertLoadStationsOk("/de/stations");
        assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
        assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNull();
    }

    @Test
    void stationsDeQueryParam() {
        var stations = assertLoadStationsOk(String.format("/%s?country=de", "stations"));
        assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
        assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNull();
    }

    @Test
    void stationsDeChQueryParam() {
        var stations = assertLoadStationsOk(String.format("/%s?country=de&country=ch", "stations"));
        assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNotNull();
        assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNotNull();
    }

    @Test
    void stationsDePhotograph() {
        var stations = assertLoadStationsOk(String.format("/de/%s?photographer=@user10", "stations"));
        assertThat(findByKey(stations, new Station.Key("de", "6966"))).isNotNull();
    }

    @Test
    void stationsCh() {
        var stations = assertLoadStationsOk(String.format("/ch/%s", "stations"));
        assertThat(findByKey(stations, new Station.Key("ch", "8500126"))).isNotNull();
        assertThat(findByKey(stations, new Station.Key("de", "6721"))).isNull();
    }

    @Test
    void stationsUnknownCountry() {
        var stations = assertLoadStationsOk("/jp/stations");
        assertThat(stations.length).isEqualTo(0);
    }

    @Test
    void stationsDeFromAnonym() {
        var stations = assertLoadStationsOk("/de/stations?photographer=Anonym");
        assertThat(stations.length).isEqualTo(9);
    }

    @Test
    void stationsJson() throws IOException {
        var response = loadRaw("/de/stations.json", 200, String.class);
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.isArray()).isTrue();
        assertThat(jsonNode.size()).isEqualTo(729);
    }

    private StationDto[] assertLoadStationsOk(String path) {
        var response = loadRaw(path, 200, StationDto[].class);

        if (response.getStatusCodeValue() != 200) {
            return new StationDto[0];
        }
        return response.getBody();
    }

    private <T> ResponseEntity<T> loadRaw(String path, int expectedStatus, Class<T> responseType) {
        var response = restTemplate.getForEntity(String.format("http://localhost:%d%s", port, path),
                responseType);

        assertThat(response.getStatusCodeValue()).isEqualTo(expectedStatus);
        return response;
    }

    private StationDto findByKey(StationDto[] stations, Station.Key key) {
        return Arrays.stream(stations).filter(station -> station.getCountry().equals(key.getCountry()) && station.getIdStr().equals(key.getId())).findAny().orElse(null);
    }

    @Test
    void photographersDeJson() throws IOException {
        var response = loadRaw("/de/photographers.json", 200, String.class);
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.isObject()).isTrue();
        assertThat(jsonNode.size()).isEqualTo(4);
        assertThat(jsonNode.get("@user27").asInt()).isEqualTo(31);
        assertThat(jsonNode.get("@user8").asInt()).isEqualTo(29);
        assertThat(jsonNode.get("@user10").asInt()).isEqualTo(15);
        assertThat(jsonNode.get("@user0").asInt()).isEqualTo(9);
    }

    @Test
    void photographersDeTxt() {
        var response = loadRaw("/de/photographers.txt", 200, String.class);
        assertThat(response.getBody()).isEqualTo("""
                count	photographer
                31	@user27
                29	@user8
                15	@user10
                9	@user0
                """);
    }

    private StationDto loadStationDe6932() {
        return loadDeStationByStationId("6932");
    }

    private StationDto loadDeStationByStationId(String stationId) {
        return loadRaw("/de/stations/" + stationId, 200, StationDto.class).getBody();
    }

    @Test
    void statisticDeJson() throws IOException {
        var response = loadRaw("/de/stats.json", 200, String.class);
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.get("total").asInt()).isEqualTo(729);
        assertThat(jsonNode.get("withPhoto").asInt()).isEqualTo(84);
        assertThat(jsonNode.get("withoutPhoto").asInt()).isEqualTo(645);
        assertThat(jsonNode.get("photographers").asInt()).isEqualTo(4);
        assertThat(jsonNode.get("countryCode").asText()).isEqualTo("de");
    }

    @Test
    void photoUploadForbidden() {
        var headers = new HttpHeaders();
        headers.add("Upload-Token", "edbfc44727a6fd4f5b029aff21861a667a6b4195");
        headers.add("Nickname", "nickname");
        headers.add("Email", "nickname@example.com");
        headers.add("Station-Id", "4711");
        headers.add("Country", "de");
        headers.setContentType(MediaType.IMAGE_JPEG);
        var request = new HttpEntity<>(IMAGE, headers);
        var response = restTemplate.postForEntity(
                String.format("http://localhost:%d%s", port, "/photoUpload"), request, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
    }

    private final byte[] IMAGE = Base64.getDecoder().decode("/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=");

    @Test
    void photoUploadUnknownStationThenDeletePhotoThenDeleteStation() throws IOException {
        var headers = new HttpHeaders();
        headers.add("Station-Title", URLEncoder.encode("Hintertupfingen", StandardCharsets.UTF_8.toString()));
        headers.add("Latitude", "50.123");
        headers.add("Longitude", "9.123");
        headers.add("Comment", "Missing Station");
        headers.setContentType(MediaType.IMAGE_JPEG);
        var request = new HttpEntity<>(IMAGE, headers);
        var uploadResponse = restTemplateWithBasicAuthUser10().postForEntity(
                String.format("http://localhost:%d%s", port, "/photoUpload"), request, String.class);

        assertThat(uploadResponse.getStatusCodeValue()).isEqualTo(202);
        var inboxResponse = mapper.readTree(uploadResponse.getBody());
        var uploadId = inboxResponse.get("id").asInt();
        var filename = inboxResponse.get("filename").asText();
        assertThat(filename).isNotBlank();
        assertThat(inboxResponse.get("crc32").asLong()).isEqualTo(312729961L);

        // download uploaded photo from inbox
        var photoResponse = restTemplate.getForEntity(
                String.format("http://localhost:%d%s%s", port, "/inbox/", filename), byte[].class);
        var inputImage = ImageIO.read(new ByteArrayInputStream(Objects.requireNonNull(photoResponse.getBody())));
        assertThat(inputImage).isNotNull();
        // we cannot binary compare the result anymore, the photos are re-encoded
        // assertThat(IOUtils.readFully((InputStream)photoResponse.getEntity(), IMAGE.length)).isEqualTo(IMAGE));

        // simulate VsionAI
        Files.move(workDir.getInboxToProcessDir().resolve(filename), workDir.getInboxProcessedDir().resolve(filename));

        // get nextZ value for stationId
        var nextZResponse = restTemplateWithBasicAuthUser10().getForEntity(
                String.format("http://localhost:%d%s", port, "/nextZ"), String.class);
        var nextZResponseJson = mapper.readTree(nextZResponse.getBody());
        var stationId = nextZResponseJson.get("nextZ").asText();


        // send import command
        sendInboxCommand("""
                {
                	 "id": %s,
                	 "stationId": "%s",
                	 "countryCode": "de",
                	 "title": "Hintertupfingen",
                	 "lat": 50.123,
                	 "lon": 9.123,
                	 "active": true,
                	 "command": "IMPORT_MISSING_STATION"
                }
                """.formatted(uploadId, stationId));

        // assert station is imported
        var newStation = loadDeStationByStationId(stationId);
        assertThat(newStation.getTitle()).isEqualTo("Hintertupfingen");
        assertThat(newStation.getLat()).isEqualTo(50.123);
        assertThat(newStation.getLon()).isEqualTo(9.123);
        assertThat(newStation.getPhotographer()).isEqualTo("@user10");
        assertThat(newStation.getPhotoUrl()).isNotNull();

        // send problem report wrong photo
        var problemReportWrongPhotoJson = """
                {
                	"countryCode": "de",
                	"stationId": "%s",
                	"type": "WRONG_PHOTO",
                	"comment": "This photo is clearly wrong"
                }""".formatted(stationId);
        int idWrongPhoto = sendProblemReport(problemReportWrongPhotoJson);
        sendInboxCommand("{\"id\": " + idWrongPhoto + ", \"command\": \"DELETE_PHOTO\"}");

        // assert station has no photo anymore
        var deletedPhotoStation = loadDeStationByStationId(stationId);
        assertThat(deletedPhotoStation.getPhotoUrl()).isNull();

        // send problem report station not existing
        var problemReportStationNonExistentJson = """
                {
                	"countryCode": "de",
                	"stationId": "%s",
                	"type": "STATION_NONEXISTENT",
                	"comment": "This photo is clearly wrong"
                }""".formatted(stationId);
        int idStationNonExistent = sendProblemReport(problemReportStationNonExistentJson);
        sendInboxCommand("{\"id\": " + idStationNonExistent + ", \"command\": \"DELETE_STATION\"}");

        // assert station doesn't exist anymore
        loadRaw("/de/stations/" + stationId, 404, StationDto.class);
    }

    @Test
    void getInboxWithBasicAuthPasswordFail() {
        var response = restTemplate.withBasicAuth("@user27", "blahblubb")
                .getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
    }

    @Test
    void problemReportWithWrongLocation() throws JsonProcessingException {
        assertCoordinatesOfStation6815(51.5764217543438, 11.281417285);

        var problemReportJson = """
                {
                	"countryCode": "de",
                	"stationId": "6815",
                	"type": "WRONG_LOCATION",
                	"comment": "Station at wrong location",
                	"lat": 51.123,
                	"lon": 11.123
                }""";
        int id = sendProblemReport(problemReportJson);
        sendInboxCommand("{\"id\": " + id + ", \"command\": \"UPDATE_LOCATION\"}");

        assertCoordinatesOfStation6815(51.123, 11.123);
    }

    private int sendProblemReport(String problemReportJson) throws JsonProcessingException {
        var responsePostProblem = restTemplateWithBasicAuthUser10()
                .postForEntity(String.format("http://localhost:%d%s", port, "/reportProblem"), new HttpEntity<>(problemReportJson, createJsonHeaders()), String.class);
        assertThat(responsePostProblem.getStatusCodeValue()).isEqualTo(202);
        var jsonNodePostProblemReponse = mapper.readTree(responsePostProblem.getBody());
        assertThat(jsonNodePostProblemReponse).isNotNull();
        assertThat(jsonNodePostProblemReponse.get("state").asText()).isEqualTo("REVIEW");
        return jsonNodePostProblemReponse.get("id").asInt();
    }

    private void assertCoordinatesOfStation6815(double lat, double lon) {
        var station = loadDeStationByStationId("6815");
        assertThat(station).isNotNull();
        assertThat(station.getLat()).isEqualTo(lat);
        assertThat(station.getLon()).isEqualTo(lon);
    }

    @Test
    void problemReportWithWrongStationName() throws JsonProcessingException {
        var stationBefore = loadDeStationByStationId("6815");
        assertThat(stationBefore).isNotNull();
        assertThat(stationBefore.getTitle()).isEqualTo("Wippra");

        var problemReportJson = """
                {
                	"countryCode": "de",
                	"stationId": "6815",
                	"type": "WRONG_NAME",
                	"comment": "Correct Name is 'New Name'"
                }""";
        var id = sendProblemReport(problemReportJson);
        sendInboxCommand("{\"id\": " + id + ", \"command\": \"CHANGE_NAME\", \"title\": \"Admin New Name\"}");

        var stationAfter = loadDeStationByStationId("6815");
        assertThat(stationAfter).isNotNull();
        assertThat(stationAfter.getTitle()).isEqualTo("Admin New Name");
    }

    @Test
    void problemReportWithOutdatedPhoto() throws JsonProcessingException {
        assertOutdatedPhotoOfStation7065(false);

        var problemReportJson = """
                {
                	"countryCode": "de",
                	"stationId": "7065",
                	"type": "PHOTO_OUTDATED",
                	"comment": "Photo is outdated"
                }""";
        var id = sendProblemReport(problemReportJson);
        sendInboxCommand("{\"id\": " + id + ", \"command\": \"PHOTO_OUTDATED\"}");

        assertOutdatedPhotoOfStation7065(true);
    }

    @NotNull
    private HttpHeaders createJsonHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void sendInboxCommand(String inboxCommand) throws JsonProcessingException {
        var responseInboxCommand = restTemplateWithBasicAuthUser10()
                .postForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), new HttpEntity<>(inboxCommand, createJsonHeaders()), String.class);
        assertThat(responseInboxCommand.getStatusCodeValue()).isEqualTo(200);
        var jsonNodeInboxCommandReponse = mapper.readTree(responseInboxCommand.getBody());
        assertThat(jsonNodeInboxCommandReponse).isNotNull();
        assertThat(jsonNodeInboxCommandReponse.get("status").asInt()).isEqualTo(200);
        assertThat(jsonNodeInboxCommandReponse.get("message").asText()).isEqualTo("ok");
    }

    private void assertOutdatedPhotoOfStation7065(boolean outdated) {
        var station = loadDeStationByStationId("7065");
        assertThat(station).isNotNull();
        assertThat(station.getOutdated()).isEqualTo(outdated);
    }


    private TestRestTemplate restTemplateWithBasicAuthUser10() {
        return restTemplate.withBasicAuth("@user10", "uON60I7XWTIN");
    }

    @Test
    void getInboxWithBasicAuthNotAuthorized() {
        var response = restTemplate.withBasicAuth("@user27", "y89zFqkL6hro")
                .getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(403);
    }

    @Test
    void getInboxWithBasicAuth() throws JsonProcessingException {
        var response = restTemplateWithBasicAuthUser10()
                .getForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.isArray()).isTrue();
    }

    @Test
    void postAdminInboxCommandWithUnknownInboxExntry() throws JsonProcessingException {
        HttpHeaders headers = createJsonHeaders();
        var response = restTemplateWithBasicAuthUser10()
                .postForEntity(String.format("http://localhost:%d%s", port, "/adminInbox"), new HttpEntity<>("{\"id\": -1, \"command\": \"IMPORT_PHOTO\"}", headers), String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode.get("status").asInt()).isEqualTo(400);
        assertThat(jsonNode.get("message").asText()).isEqualTo("No pending inbox entry found");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/countries", "/countries.json"})
    void countries(String path) throws IOException {
        var response = loadRaw(path, 200, String.class);
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.isArray()).isTrue();
        assertThat(jsonNode.size()).isEqualTo(2);

        var foundCountries = new AtomicInteger();
        jsonNode.forEach(node -> {
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
    void countriesAll(String path) throws IOException {
        var response = loadRaw(path + "?onlyActive=false", 200, String.class);
        var jsonNode = mapper.readTree(response.getBody());
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.isArray()).isTrue();
        assertThat(jsonNode.size()).isEqualTo(4);
    }

    private void assertProviderApp(JsonNode countryNode, int i, String type, String name, String url) {
        var app = countryNode.get("providerApps").get(i);
        assertThat(app.get("type").asText()).isEqualTo(type);
        assertThat(app.get("name").asText()).isEqualTo(name);
        assertThat(app.get("url").asText()).isEqualTo(url);
    }

    @TestConfiguration
    static class SpringConfig {

        @Bean
        public WorkDir workDir() {
            return new WorkDir(createTempWorkDir(), null);
        }

        @Bean
        public Monitor monitor() {
            return new LoggingMonitor();
        }

        private String createTempWorkDir() {
            try {
                return Files.createTempDirectory("workDir-" + System.currentTimeMillis()).toFile().getAbsolutePath();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Bean
        public Filter openApiValidationFilter() {
            return new OpenApiValidationFilter(true, true);
        }

        @Bean
        public WebMvcConfigurer addOpenApiValidationInterceptor(@Value("classpath:static/openapi.yaml") Resource apiSpecification) throws IOException {
            var specResource = new EncodedResource(apiSpecification, StandardCharsets.UTF_8);
            var openApiValidationInterceptor = new OpenApiValidationInterceptor(specResource);
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(@NotNull InterceptorRegistry registry) {
                    registry.addInterceptor(openApiValidationInterceptor);
                }
            };
        }
    }

}
