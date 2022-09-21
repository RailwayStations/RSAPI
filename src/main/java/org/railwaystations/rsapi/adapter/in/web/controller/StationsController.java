package org.railwaystations.rsapi.adapter.in.web.controller;

import org.railwaystations.rsapi.adapter.in.web.model.StationDto;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.ports.in.FindPhotoStationsUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Set;

@RestController
public class StationsController {

    private static final String COUNTRY = "country";
    private static final String PHOTOGRAPHER = "photographer";
    private static final String HAS_PHOTO = "hasPhoto";
    private static final String ID = "id";
    private static final String ACTIVE = "active";
    private static final String SINCE_HOURS = "sinceHours";

    @Autowired
    private FindPhotoStationsUseCase findPhotoStationsUseCase;

    @Value("${photoBaseUrl}")
    private String photoBaseUrl;

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/stations")
    public List<StationDto> get(@RequestParam(value = COUNTRY, required = false) Set<String> countries,
                                @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return toDto(findPhotoStationsUseCase.findByCountry(countries, hasPhoto, photographer, active));
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/stations.json")
    public List<StationDto> getAsJson(@RequestParam(value = COUNTRY, required = false) Set<String> countries,
                                      @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                      @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                      @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return get(countries, hasPhoto, photographer, active);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/{country}/stations")
    public List<StationDto> getWithCountry(@PathVariable(COUNTRY) String country,
                                           @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                           @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                           @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return get(Set.of(country), hasPhoto, photographer, active);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/{country}/stations.json")
    public List<StationDto> getWithCountryAsJson(@PathVariable(COUNTRY) String country,
                                                 @RequestParam(value = HAS_PHOTO, required = false) Boolean hasPhoto,
                                                 @RequestParam(value = PHOTOGRAPHER, required = false) String photographer,
                                                 @RequestParam(value = ACTIVE, required = false) Boolean active) {
        return getWithCountry(country, hasPhoto, photographer, active);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", value = "/{country}/stations/{id}")
    public StationDto getById(@PathVariable(COUNTRY) String country,
                              @PathVariable(ID) String id) {
        return toDto(findPhotoStationsUseCase.findByCountryAndId(country, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", value = "/recentPhotoImports")
    public List<StationDto> recentPhotoImports(@RequestParam(value = SINCE_HOURS, required = false, defaultValue = "10") @Min(1) @Max(800) long sinceHours) {
        return toDto(findPhotoStationsUseCase.findRecentImports(sinceHours));
    }

    private List<StationDto> toDto(Set<Station> stations) {
        return stations.stream().map(this::toDto).toList();
    }

    private StationDto toDto(Station station) {
        var stationDto = new StationDto()
                .country(station.getKey().getCountry())
                .idStr(station.getKey().getId())
                .id(legacyStationId(station.getKey().getId()))
                .title(station.getTitle())
                .DS100(station.getDs100())
                .active(station.isActive())
                .lat(station.getCoordinates().getLat())
                .lon(station.getCoordinates().getLon());

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

}
