package org.railwaystations.rsapi.adapter.in.web.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RestController
@Validated
public class PhotoStationsController {

    @Autowired
    private FindPhotoStationsUseCase findPhotoStationsUseCase;

    @Value("${photoBaseUrl}")
    private String photoBaseUrl;

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/photoStationById/{country}/{id}")
    public PhotoStationsDto photoStationById(@PathVariable(value = "country") String country,
                                             @PathVariable(value = "id") String id) {
        var stations = Set.of(findPhotoStationsUseCase.findByCountryAndId(country, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return mapPhotoStations(stations);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/photoStationsByCountry/{country}")
    public PhotoStationsDto photoStationsByCountry(@PathVariable(value = "country") String country,
                                                   @RequestParam(value = "hasPhoto", required = false) Boolean hasPhoto,
                                                   @RequestParam(value = "isActive", required = false) Boolean isActive) {
        var stations = findPhotoStationsUseCase.findByCountry(Set.of(country), hasPhoto, isActive);
        return mapPhotoStations(stations);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/photoStationsByPhotographer/{photographer}")
    public PhotoStationsDto photoStationsByPhotographer(@PathVariable(value = "photographer") String photographer,
                                                        @RequestParam(value = "country", required = false) String country) {
        var stations = findPhotoStationsUseCase.findByPhotographer(photographer, country);
        return mapPhotoStations(stations);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/photoStationsByRecentPhotoImports")
    public PhotoStationsDto photoStationsByRecentPhotoImports(@RequestParam(value = "sinceHours", required = false, defaultValue = "10") @Min(1) @Max(800) long sinceHours) {
        var stations = findPhotoStationsUseCase.findRecentImports(sinceHours);
        return mapPhotoStations(stations);
    }

    private PhotoStationsDto mapPhotoStations(Set<Station> stations) {
        return new PhotoStationsDto()
                .photoBaseUrl(photoBaseUrl)
                .licenses(mapLicenses(stations))
                .photographers(mapPhotographers(stations))
                .stations(mapStations(stations));
    }

    private List<PhotoStationDto> mapStations(Set<Station> stations) {
        return stations.stream()
                .map(station -> new PhotoStationDto()
                        .country(station.getKey().getCountry())
                        .id(station.getKey().getId())
                        .title(station.getTitle())
                        .lat(station.getCoordinates().getLat())
                        .lon(station.getCoordinates().getLon())
                        .shortCode(station.getDs100())
                        .inactive(station.isActive() ? null : Boolean.TRUE)
                        .photos(mapPhotos(station))
                ).toList();
    }

    private List<PhotoDto> mapPhotos(Station station) {
        return station.getPhotos().stream()
                .sorted()
                .map(photo -> new PhotoDto()
                        .id(photo.getId())
                        .path(photo.getUrlPath())
                        .photographer(photo.getPhotographer().getDisplayName())
                        .license(photo.getLicense().name())
                        .createdAt(photo.getCreatedAt().toEpochMilli())
                        .outdated(photo.isOutdated() ? Boolean.TRUE : null))
                .toList();
    }

    private List<PhotographerDto> mapPhotographers(Set<Station> stations) {
        return stations.stream()
                .flatMap(station -> station.getPhotos().stream())
                .map(Photo::getPhotographer)
                .collect(toMap(User::getDisplayName, Function.identity(), (user1, user2) -> user1))
                .values().stream()
                .map(user -> new PhotographerDto()
                        .name(user.getDisplayName())
                        .url(URI.create(user.getDisplayUrl())))
                .toList();
    }

    private List<PhotoLicenseDto> mapLicenses(Set<Station> stations) {
        return stations.stream()
                .flatMap(station -> station.getPhotos().stream())
                .map(Photo::getLicense)
                .collect(toMap(License::name, Function.identity(), (license1, license2) -> license1))
                .values().stream()
                .map(license -> new PhotoLicenseDto()
                        .id(license.name())
                        .name(license.getDisplayName())
                        .url(URI.create(license.getUrl())))
                .toList();
    }

}
