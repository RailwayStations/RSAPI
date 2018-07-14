package org.railwaystations.api.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.ElasticBackend;
import org.railwaystations.api.model.*;
import org.railwaystations.api.model.elastic.Bahnhofsfoto;
import org.railwaystations.api.monitoring.Monitor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BaseStationLoader implements StationLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SOURCE_ELEMENT = "_source";

    private final String stationsUrl;
    private final String photosUrl;
    private final Country country;
    private final Monitor monitor;

    private final ElasticBackend elasticBackend;

    BaseStationLoader(final Country country, final String photosUrl, final String stationsUrl, final Monitor monitor, final ElasticBackend elasticBackend) {
        super();
        this.country = country;
        this.photosUrl = photosUrl;
        this.stationsUrl = stationsUrl;
        this.elasticBackend = elasticBackend;
        this.monitor = monitor;
    }

    public Country getCountry() {
        return country;
    }

    @Override
    public final Map<Station.Key, Station> loadStations(final Map<String, Photographer> photographers, final String photoBaseUrl) {
        try {
            return fetchStations(fetchPhotos(new HashMap<>(), photographers, photoBaseUrl));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Station.Key, Photo> fetchPhotos(final Map<Station.Key, Photo> photos, final Map<String, Photographer> photographers, final String photoBaseUrl) throws Exception {
        elasticBackend.fetchAll(photosUrl, 0, hits -> fetchPhotos(photos, photographers, photoBaseUrl, hits));
        return photos;
    }

    private Void fetchPhotos(final Map<Station.Key, Photo> photos, final Map<String, Photographer> photographers, final String photoBaseUrl, final JsonNode hits) {
        for (int i = 0; i < hits.size(); i++) {
            final Photo photo = createPhoto(hits.get(i).get(BaseStationLoader.SOURCE_ELEMENT), photographers, photoBaseUrl);
            if (photos.get(photo.getStationKey()) != null) {
                monitor.sendMessage("Station " + photo.getStationKey() + " has duplicate photos");
            }
            photos.put(photo.getStationKey(), photo);
        }
        return null;
    }

    private Photo createPhoto(final JsonNode photoJson, final Map<String, Photographer> photographers, final String photoBaseUrl) {
        final Bahnhofsfoto bahnhofsfoto;
        try {
            bahnhofsfoto = MAPPER.treeToValue(photoJson, Bahnhofsfoto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        final String statUser = "1".equals(photoJson.get("flag").asText()) ? "@RecumbentTravel" : bahnhofsfoto.getPhotographer();
        return new Photo(new Station.Key(bahnhofsfoto.getCountryCode().toLowerCase(Locale.ENGLISH), bahnhofsfoto.getId()), photoBaseUrl + bahnhofsfoto.getUrl(),
                bahnhofsfoto.getPhotographer(), getPhotographerUrl(bahnhofsfoto.getPhotographer(), photographers),
                bahnhofsfoto.getCreatedAt(), StringUtils.trimToEmpty(bahnhofsfoto.getLicense()), statUser);
    }

    private String getPhotographerUrl(final String nickname, final Map<String, Photographer> photographers) {
        final Photographer photographer = photographers.get(nickname);
        return photographer != null ? photographer.getUrl() : null;
    }

    private Map<Station.Key, Station> fetchStations(final Map<Station.Key, Photo> photos) throws Exception {
        final Map<Station.Key, Station> stations = new HashMap<>();
        elasticBackend.fetchAll(stationsUrl, 0, hits -> fetchStations(photos, stations, hits));
        return stations;
    }

    private Void fetchStations(final Map<Station.Key,Photo> photos, final Map<Station.Key, Station> stations, final JsonNode hits) {
        for (int i = 0; i < hits.size(); i++) {
            final Station station = createStationFromElastic(photos, hits.get(i).get(BaseStationLoader.SOURCE_ELEMENT));
            stations.put(station.getKey(), station);
        }
        return null;
    }

    protected Station createStationFromElastic(final Map<Station.Key, Photo> photos, final JsonNode sourceJson) {
        final JsonNode propertiesJson = sourceJson.get("properties");
        final String id = propertiesJson.get("UICIBNR").asText();
        final JsonNode abkuerzung = propertiesJson.get("abkuerzung");
        final Station.Key key = new Station.Key(getCountry().getCode(), id);
        return new Station(key,
                propertiesJson.get("name").asText(),
                readCoordinates(sourceJson),
                abkuerzung != null ? abkuerzung.asText() : "",
                photos.get(key));
    }

    Coordinates readCoordinates(final JsonNode json) {
        final JsonNode coordinates = json.get("geometry").get("coordinates");
        return new Coordinates(coordinates.get(1).asDouble(), coordinates.get(0).asDouble());
    }
}