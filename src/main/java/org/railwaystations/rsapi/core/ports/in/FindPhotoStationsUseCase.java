package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.Station;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FindPhotoStationsUseCase {

    List<Station> findStationsBy(Set<String> countries, Boolean hasPhoto, String photographer, Integer maxDistance, Double lat, Double lon, Boolean active);

    Optional<Station> findByCountryAndId(String country, String id);

    List<Station> findRecentImports(Instant minus);

}
