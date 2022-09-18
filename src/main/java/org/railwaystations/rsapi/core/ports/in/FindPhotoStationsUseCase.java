package org.railwaystations.rsapi.core.ports.in;

import org.railwaystations.rsapi.core.model.Station;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface FindPhotoStationsUseCase {

    Set<Station> findStationsBy(Set<String> countries, Boolean hasPhoto, String photographer, Boolean active);

    Set<Station> findStationsBy(Set<String> countries, Boolean hasPhoto, Boolean active);

    Optional<Station> findByCountryAndId(String country, String id);

    Set<Station> findRecentImports(Instant minus);

}
