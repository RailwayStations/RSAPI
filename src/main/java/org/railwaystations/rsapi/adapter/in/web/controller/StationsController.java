package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.in.web.api.StationsApi;
import org.railwaystations.rsapi.adapter.in.web.model.StationDto;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.in.FindPhotoStationsUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class StationsController implements StationsApi {

    private final FindPhotoStationsUseCase findPhotoStationsUseCase;

    private final String photoBaseUrl;

    public StationsController(FindPhotoStationsUseCase findPhotoStationsUseCase, @Value("${photoBaseUrl}") String photoBaseUrl) {
        this.findPhotoStationsUseCase = findPhotoStationsUseCase;
        this.photoBaseUrl = photoBaseUrl;
    }

    private List<StationDto> toDto(Set<Station> stations) {
        return stations.stream().map(this::toDto).toList();
    }

    private StationDto toDto(Station station) {
        var stationDto = new StationDto(
                station.getKey().getId(),
                station.getKey().getCountry(),
                station.getTitle(),
                station.getCoordinates().getLat(),
                station.getCoordinates().getLon(),
                station.isActive())
                .id(legacyStationId(station.getKey().getId()))
                .DS100(station.getDs100());

        station.getPrimaryPhoto()
                .map(photo -> {
                    stationDto.license(photo.getLicense().getDisplayName())
                            .licenseUrl(photo.getLicense().getUrl())
                            .photoUrl(photoBaseUrl + photo.getUrlPath())
                            .photoId(photo.getId())
                            .photographer(photo.getPhotographer().getDisplayName())
                            .photographerUrl(photo.getPhotographer().getDisplayUrl())
                            .createdAt(photo.getCreatedAt() != null ? photo.getCreatedAt().toEpochMilli() : null)
                            .outdated(photo.isOutdated());
                    return null;
                });
        return stationDto;
    }

    public long legacyStationId(String stationId) {
        try {
            return Long.parseLong(stationId);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @Override
    public ResponseEntity<List<StationDto>> countryStationsGet(String country, Boolean hasPhoto, String photographer, Boolean active) {
        return stationsGet(List.of(country), hasPhoto, photographer, active);
    }

    @Override
    public ResponseEntity<StationDto> countryStationsIdGet(String country, String id) {
        return ResponseEntity.ok(toDto(findPhotoStationsUseCase.findByCountryAndId(country, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))));
    }

    @Override
    public ResponseEntity<List<StationDto>> stationsGet(List<String> country, Boolean hasPhoto, String photographer, Boolean active) {
        return ResponseEntity.ok(toDto(findPhotoStationsUseCase.findByCountry(country != null ? country.stream().limit(3).collect(Collectors.toSet()) : Set.of("de"), hasPhoto, photographer, active)));
    }

}
