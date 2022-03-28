package org.railwaystations.rsapi.core.services;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.model.Coordinates;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.in.FindPhotoStationsUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
public class PhotoStationsService implements FindPhotoStationsUseCase {

    private final StationDao stationDao;
    private final String photoBaseUrl;

    public PhotoStationsService(final StationDao stationDao, @Value("${photoBaseUrl}") final String photoBaseUrl) {
        super();
        this.stationDao = stationDao;
        this.photoBaseUrl = photoBaseUrl;
    }

    public Map<Station.Key, Station> getStationsByCountry(final Set<String> countryCodes) {
        final Set<Station> stations;
        if (countryCodes == null || countryCodes.isEmpty()) {
            stations = stationDao.all();
        } else {
            stations = stationDao.findByCountryCodes(countryCodes);
        }
        return stations.stream().map(this::injectPhotoBaseUrl).collect(toMap(Station::getKey, Function.identity()));
    }

    private Station injectPhotoBaseUrl(final Station station) {
        station.prependPhotoBaseUrl(photoBaseUrl);
        return station;
    }

    public Optional<Station> findByCountryAndId(final String country, final String stationId) {
        if (StringUtils.isBlank(stationId)) {
            return Optional.empty();
        }
        if (StringUtils.isNotBlank(country)) {
            return findByKey(new Station.Key(country, stationId));
        }
        final Set<Station> stations = stationDao.findById(stationId);
        if (stations.size() > 1) {
            return Optional.empty(); // id is not unique
        }
        return stations.stream().findFirst().map(this::injectPhotoBaseUrl);
    }

    public Optional<Station> findByKey(final Station.Key key) {
        return stationDao.findByKey(key.getCountry(), key.getId()).stream().findFirst().map(this::injectPhotoBaseUrl);
    }

    public void insert(final Station station) {
        stationDao.insert(station);
    }

    public void delete(final Station station) {
        stationDao.delete(station);
    }

    public void updateActive(final Station station) {
        stationDao.updateActive(station);
    }

    public List<Station> findRecentImports(final Instant since) {
        return stationDao.findRecentImports(since.toEpochMilli()).stream().map(this::injectPhotoBaseUrl).toList();
    }

    public int countNearbyCoordinates(final Coordinates coordinates) {
        return stationDao.countNearbyCoordinates(coordinates);
    }

    public String getNextZ() {
        return "Z" + (stationDao.getMaxZ() + 1);
    }

    public void changeStationTitle(final Station station, final String newTitle) {
        stationDao.changeStationTitle(station, newTitle);
    }

    public void updateLocation(final Station station, final Coordinates coordinates) {
        stationDao.updateLocation(station, coordinates);
    }

    public List<Station> findStationsBy(final Set<String> countries, final Boolean hasPhoto, final String photographer,
                                        final Integer maxDistance, final Double lat, final Double lon, final Boolean active) {
        // TODO: can we search this on the DB?
        return getStationsByCountry(countries)
                .values().stream().filter(station -> station.appliesTo(hasPhoto, photographer, maxDistance, lat, lon, active)).collect(Collectors.toList());

    }

}
