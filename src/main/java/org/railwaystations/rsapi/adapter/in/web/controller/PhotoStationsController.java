package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.in.web.api.PhotoStationsApi;
import org.railwaystations.rsapi.adapter.in.web.model.PhotoDto;
import org.railwaystations.rsapi.adapter.in.web.model.PhotoLicenseDto;
import org.railwaystations.rsapi.adapter.in.web.model.PhotoStationDto;
import org.railwaystations.rsapi.adapter.in.web.model.PhotoStationsDto;
import org.railwaystations.rsapi.adapter.in.web.model.PhotographerDto;
import org.railwaystations.rsapi.core.model.License;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.FindPhotoStationsUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RestController
@Validated
public class PhotoStationsController implements PhotoStationsApi {

    private final FindPhotoStationsUseCase findPhotoStationsUseCase;

    private final String photoBaseUrl;

    public PhotoStationsController(FindPhotoStationsUseCase findPhotoStationsUseCase, @Value("${photoBaseUrl}") String photoBaseUrl) {
        this.findPhotoStationsUseCase = findPhotoStationsUseCase;
        this.photoBaseUrl = photoBaseUrl;
    }

    private PhotoStationsDto mapPhotoStations(Set<Station> stations) {
        return new PhotoStationsDto(photoBaseUrl, mapLicenses(stations), mapPhotographers(stations), mapStations(stations));
    }

    private List<PhotoStationDto> mapStations(Set<Station> stations) {
        return stations.stream()
                .map(station -> new PhotoStationDto(
                        station.getKey().getCountry(),
                        station.getKey().getId(),
                        station.getTitle(),
                        station.getCoordinates().getLat(),
                        station.getCoordinates().getLon(),
                        mapPhotos(station))
                        .shortCode(station.getDs100())
                        .inactive(station.isActive() ? null : Boolean.TRUE)
                ).toList();
    }

    private List<PhotoDto> mapPhotos(Station station) {
        return station.getPhotos().stream()
                .sorted()
                .map(photo -> new PhotoDto(
                        photo.getId(),
                        photo.getPhotographer().getDisplayName(),
                        photo.getUrlPath(),
                        photo.getCreatedAt().toEpochMilli(),
                        photo.getLicense().name())
                        .outdated(photo.isOutdated() ? Boolean.TRUE : null))
                .toList();
    }

    private List<PhotographerDto> mapPhotographers(Set<Station> stations) {
        return stations.stream()
                .flatMap(station -> station.getPhotos().stream())
                .map(Photo::getPhotographer)
                .collect(toMap(User::getDisplayName, Function.identity(), (user1, user2) -> user1))
                .values().stream()
                .map(user -> new PhotographerDto(user.getDisplayName())
                        .url(URI.create(user.getDisplayUrl())))
                .toList();
    }

    private List<PhotoLicenseDto> mapLicenses(Set<Station> stations) {
        return stations.stream()
                .flatMap(station -> station.getPhotos().stream())
                .map(Photo::getLicense)
                .collect(toMap(License::name, Function.identity(), (license1, license2) -> license1))
                .values().stream()
                .map(license -> new PhotoLicenseDto(license.name(), license.getDisplayName(), URI.create(license.getUrl())))
                .toList();
    }

    @Override
    public ResponseEntity<PhotoStationsDto> photoStationByIdCountryIdGet(String country, String id) {
        var stations = Set.of(findPhotoStationsUseCase.findByCountryAndId(country, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return ResponseEntity.ok(mapPhotoStations(stations));
    }

    @Override
    public ResponseEntity<PhotoStationsDto> photoStationsByCountryCountryGet(String country, Boolean hasPhoto, Boolean isActive) {
        var stations = findPhotoStationsUseCase.findByCountry(Set.of(country), hasPhoto, isActive);
        return ResponseEntity.ok(mapPhotoStations(stations));
    }

    @Override
    public ResponseEntity<PhotoStationsDto> photoStationsByPhotographerPhotographerGet(String photographer, String country) {
        var stations = findPhotoStationsUseCase.findByPhotographer(photographer, country);
        return ResponseEntity.ok(mapPhotoStations(stations));
    }

    @Override
    public ResponseEntity<PhotoStationsDto> photoStationsByRecentPhotoImportsGet(Integer sinceHours) {
        var stations = findPhotoStationsUseCase.findRecentImports(sinceHours);
        return ResponseEntity.ok(mapPhotoStations(stations));
    }

}
