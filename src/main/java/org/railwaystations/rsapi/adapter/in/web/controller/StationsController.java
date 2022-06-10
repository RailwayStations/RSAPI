package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.in.web.model.StationDto;
import org.railwaystations.rsapi.adapter.in.web.writer.StationsGpxWriter;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.in.FindPhotoStationsUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
public class StationsController {

    private static final String COUNTRY = "country";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String HAS_PHOTO = "hasPhoto";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String LAT = "lat";
    private static final String LON = "lon";
    private static final String ID = "id";
    private static final String ACTIVE = "active";
    private static final String SINCE_HOURS = "sinceHours";

    @Autowired
    private FindPhotoStationsUseCase findPhotoStationsUseCase;

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", StationsGpxWriter.GPX_MEDIA_TYPE_VALUE}, value = "/stations")
    public List<StationDto> get(@RequestParam(value = COUNTRY, required = false) Set<String> countries,
                                @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                @RequestParam(value = MAX_DISTANCE, required = false) Integer maxDistance,
                                @RequestParam(value = LAT, required = false) Double lat,
                                @RequestParam(value = LON, required = false) Double lon,
                                @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return toDto(findPhotoStationsUseCase.findStationsBy(countries, hasPhoto, photographer, maxDistance, lat, lon, active));
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/stations.json")
    public List<StationDto> getAsJson(@RequestParam(value = COUNTRY, required = false) Set<String> countries,
                                  @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                  @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                  @RequestParam(value = MAX_DISTANCE, required = false) Integer maxDistance,
                                  @RequestParam(value = LAT, required = false) Double lat,
                                  @RequestParam(value = LON, required = false) Double lon,
                                  @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return get(countries, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {StationsGpxWriter.GPX_MEDIA_TYPE_VALUE}, value = "/stations.gpx")
    public List<StationDto> getAsGpx(@RequestParam(value = COUNTRY, required = false) Set<String> countries,
                             @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                             @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                             @RequestParam(value = MAX_DISTANCE, required = false) Integer maxDistance,
                             @RequestParam(value = LAT, required = false) Double lat,
                             @RequestParam(value = LON, required = false) Double lon,
                             @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return get(countries, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", StationsGpxWriter.GPX_MEDIA_TYPE_VALUE}, value = "/{country}/stations")
    public List<StationDto> getWithCountry(@PathVariable(COUNTRY) String country,
                                        @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                        @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                        @RequestParam(value = MAX_DISTANCE, required = false) Integer maxDistance,
                                        @RequestParam(value = LAT, required = false) Double lat,
                                        @RequestParam(value = LON, required = false) Double lon,
                                        @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return get(Collections.singleton(country), hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/{country}/stations.json")
    public List<StationDto> getWithCountryAsJson(@PathVariable(COUNTRY) String country,
                                              @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                              @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                              @RequestParam(value = MAX_DISTANCE, required = false) Integer maxDistance,
                                              @RequestParam(value = LAT, required = false) Double lat,
                                              @RequestParam(value = LON, required = false) Double lon,
                                              @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return getWithCountry(country, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = {StationsGpxWriter.GPX_MEDIA_TYPE_VALUE}, value = "/{country}/stations.gpx")
    public List<StationDto> getWithCountryAsGpx(@PathVariable(COUNTRY) String country,
                                              @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                              @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                              @RequestParam(value = MAX_DISTANCE, required = false) Integer maxDistance,
                                              @RequestParam(value = LAT, required = false) Double lat,
                                              @RequestParam(value = LON, required = false) Double lon,
                                              @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return getWithCountry(country, hasPhoto, photographer, maxDistance, lat, lon, active);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", value = "/{country}/stations/{id}")
    public StationDto getById(@PathVariable(COUNTRY) String country,
                           @PathVariable(ID) String id) {
        return toDto(findPhotoStationsUseCase.findByCountryAndId(country, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", value = "/recentPhotoImports")
    public List<StationDto> recentPhotoImports(@RequestParam(value = SINCE_HOURS, required = false, defaultValue = "10") long sinceHours) {
        return toDto(findPhotoStationsUseCase.findRecentImports(Instant.now().minus(sinceHours, ChronoUnit.HOURS)));
    }

    private List<StationDto> toDto(List<Station> stations) {
        return stations.stream().map(this::toDto).toList();
    }

    private StationDto toDto(Station station) {
        return new StationDto()
                .country(station.getKey().getCountry())
                .idStr(station.getKey().getId())
                .id(legacyStationId(station.getKey().getId()))
                .title(station.getTitle())
                .DS100(station.getDS100())
                .license(station.getLicense())
                .active(station.isActive())
                .lat(station.getCoordinates().getLat())
                .lon(station.getCoordinates().getLon())
                .photoUrl(station.getPhotoUrl())
                .photographer(station.getPhotographer())
                .photographerUrl(station.getPhotographerUrl())
                .createdAt(station.getCreatedAt() != null ? station.getCreatedAt().toEpochMilli() : null)
                .outdated(station.getOutdated())
                .licenseUrl(station.getLicenseUrl());
    }

    public long legacyStationId(String stationId) {
        try {
            return Long.parseLong(stationId);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

}
