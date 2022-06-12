package org.railwaystations.rsapi.core.services;

import org.apache.commons.lang3.StringUtils;
import org.railwaystations.rsapi.adapter.out.db.StationDao;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.in.FindPhotoStationsUseCase;
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

    public PhotoStationsService(StationDao stationDao) {
        super();
        this.stationDao = stationDao;
    }

    public Map<Station.Key, Station> getStationsByCountry(Set<String> countryCodes) {
        Set<Station> stations;
        if (countryCodes == null || countryCodes.isEmpty()) {
            stations = stationDao.all();
        } else {
            stations = stationDao.findByCountryCodes(countryCodes);
        }
        return stations.stream().collect(toMap(Station::getKey, Function.identity()));
    }

    public Optional<Station> findByCountryAndId(String country, String stationId) {
        if (StringUtils.isBlank(stationId)) {
            return Optional.empty();
        }
        if (StringUtils.isNotBlank(country)) {
            return findByKey(new Station.Key(country, stationId));
        }
        Set<Station> stations = stationDao.findById(stationId);
        if (stations.size() > 1) {
            return Optional.empty(); // id is not unique
        }
        return stations.stream().findFirst();
    }

    public Optional<Station> findByKey(Station.Key key) {
        return stationDao.findByKey(key.getCountry(), key.getId()).stream().findFirst();
    }

    public List<Station> findRecentImports(Instant since) {
        return stationDao.findRecentImports(since);
    }

    public List<Station> findStationsBy(Set<String> countries, Boolean hasPhoto, String photographer,
                                        Integer maxDistance, Double lat, Double lon, Boolean active) {
        // TODO: can we search this on the DB?
        return getStationsByCountry(countries)
                .values().stream().filter(station -> station.appliesTo(hasPhoto, photographer, maxDistance, lat, lon, active)).collect(Collectors.toList());

    }

}
