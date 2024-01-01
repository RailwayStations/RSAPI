package org.railwaystations.rsapi.adapter.in.web.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.railwaystations.rsapi.adapter.in.web.model.CountryDto;
import org.railwaystations.rsapi.adapter.in.web.model.RegisterProfileDto;
import org.railwaystations.rsapi.core.model.Photo;
import org.railwaystations.rsapi.core.model.Station;
import org.railwaystations.rsapi.core.model.User;
import org.railwaystations.rsapi.core.ports.in.FindPhotoStationsUseCase;
import org.railwaystations.rsapi.core.ports.in.ListCountriesUseCase;
import org.railwaystations.rsapi.core.ports.in.ManageProfileUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getRequest;
import static org.railwaystations.rsapi.adapter.in.web.RequestUtil.getUserAgent;

@Validated
@Controller
@Deprecated
public class DeprecatedApiController {

    private final FindPhotoStationsUseCase findPhotoStationsUseCase;

    private final ListCountriesUseCase listCountriesUseCase;

    private final ManageProfileUseCase manageProfileUseCase;

    private final LocaleResolver localeResolver;

    private final String photoBaseUrl;

    public DeprecatedApiController(FindPhotoStationsUseCase findPhotoStationsUseCase, ListCountriesUseCase listCountriesUseCase, ManageProfileUseCase manageProfileUseCase, LocaleResolver localeResolver, @Value("${photoBaseUrl}") String photoBaseUrl) {
        this.findPhotoStationsUseCase = findPhotoStationsUseCase;
        this.listCountriesUseCase = listCountriesUseCase;
        this.manageProfileUseCase = manageProfileUseCase;
        this.localeResolver = localeResolver;
        this.photoBaseUrl = photoBaseUrl;
    }

    private List<StationDto> toDto(Set<Station> stations) {
        return stations.stream().map(this::toDto).toList();
    }

    private StationDto toDto(Station station) {
        var photo = station.getPrimaryPhoto();

        return new StationDto(
                station.getKey().getId(),
                legacyStationId(station.getKey().getId()),
                station.getKey().getCountry(),
                station.getTitle(),
                photo.map(it -> it.getPhotographer().getDisplayName()).orElse(null),
                photo.map(it -> it.getPhotographer().getDisplayUrl()).orElse(null),
                photo.map(it -> photoBaseUrl + it.getUrlPath()).orElse(null),
                photo.map(Photo::getId).orElse(null),
                photo.map(it -> it.getLicense().getDisplayName()).orElse(null),
                photo.map(it -> it.getLicense().getUrl()).orElse(null),
                station.getCoordinates().getLat(),
                station.getCoordinates().getLon(),
                photo.map(it -> it.getCreatedAt() != null ? it.getCreatedAt().toEpochMilli() : null).orElse(null),
                station.getDs100(),
                station.isActive(),
                photo.map(Photo::isOutdated).orElse(null)
        );
    }

    private long legacyStationId(String stationId) {
        try {
            return Long.parseLong(stationId);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @Deprecated
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/{country}/stations",
            produces = {"application/json"}
    )
    public ResponseEntity<List<StationDto>> countryStationsGet(
            @Size(min = 2, max = 2) @PathVariable("country") String country,
            @Valid @RequestParam(value = "hasPhoto", required = false) Boolean hasPhoto,
            @Valid @RequestParam(value = "photographer", required = false) String photographer,
            @Valid @RequestParam(value = "active", required = false) Boolean active
    ) {
        return stationsGet(List.of(country), hasPhoto, photographer, active);
    }

    @Deprecated
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/{country}/stations/{id}",
            produces = {"application/json"}
    )
    public ResponseEntity<StationDto> countryStationsIdGet(
            @Size(min = 2, max = 2) @PathVariable("country") String country,
            @PathVariable("id") String id
    ) {
        return ResponseEntity.ok()
                .headers(createDeprecationHeader())
                .body(toDto(findPhotoStationsUseCase.findByCountryAndId(country, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))));
    }

    private HttpHeaders createDeprecationHeader() {
        var responseHeaders = new HttpHeaders();
        responseHeaders.set("Deprecation", "@1661983200");
        return responseHeaders;
    }

    @Deprecated
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/stations",
            produces = {"application/json"}
    )
    public ResponseEntity<List<StationDto>> stationsGet(
            @Valid @RequestParam(value = "country", required = false) List<@Valid String> country,
            @Valid @RequestParam(value = "hasPhoto", required = false) Boolean hasPhoto,
            @Valid @RequestParam(value = "photographer", required = false) String photographer,
            @Valid @RequestParam(value = "active", required = false) Boolean active
    ) {
        return ResponseEntity.ok()
                .headers(createDeprecationHeader())
                .body(toDto(findPhotoStationsUseCase.findByCountry(country != null ? country.stream().limit(3)
                        .collect(Collectors.toSet()) : Set.of("de"), hasPhoto, photographer, active)));
    }

    @Deprecated
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/countries.json",
            produces = {"application/json"}
    )
    public ResponseEntity<List<CountryDto>> countriesJsonGet(
            @Valid @RequestParam(value = "onlyActive", required = false) Boolean onlyActive) {
        return ResponseEntity.ok()
                .headers(createDeprecationHeader())
                .body(listCountriesUseCase.list(onlyActive).stream().map(CountriesController::toDto).toList());
    }

    @Deprecated
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/registration",
            produces = {"text/plain"},
            consumes = {"application/json"}
    )
    public ResponseEntity<Void> registrationPost(
            @Valid @RequestBody RegisterProfileDto registration) {
        manageProfileUseCase.register(toUser(registration), getUserAgent());
        return ResponseEntity.accepted()
                .headers(createDeprecationHeader())
                .build();
    }

    private User toUser(RegisterProfileDto registerProfileDto) {
        return User.builder()
                .name(registerProfileDto.getNickname())
                .email(registerProfileDto.getEmail())
                .url(registerProfileDto.getLink() != null ? registerProfileDto.getLink().toString() : null)
                .ownPhotos(registerProfileDto.getPhotoOwner())
                .anonymous(registerProfileDto.getAnonymous() != null && registerProfileDto.getAnonymous())
                .license(ProfileController.toLicense(registerProfileDto.getLicense()))
                .sendNotifications(registerProfileDto.getSendNotifications() == null || registerProfileDto.getSendNotifications())
                .newPassword(registerProfileDto.getNewPassword())
                .locale(localeResolver.resolveLocale(getRequest()))
                .build();
    }

    @JsonTypeName("Station")
    @Deprecated
    public record StationDto(

            @JsonProperty("idStr")
            String idStr,

            @JsonProperty("id")
            Long id,

            @JsonProperty("country")
            String country,

            @JsonProperty("title")
            String title,

            @JsonProperty("photographer")
            String photographer,

            @JsonProperty("photographerUrl")
            String photographerUrl,

            @JsonProperty("photoUrl")
            String photoUrl,

            @JsonProperty("photoId")
            Long photoId,

            @JsonProperty("license")
            String license,

            @JsonProperty("licenseUrl")
            String licenseUrl,

            @JsonProperty("lat")
            Double lat,

            @JsonProperty("lon")
            Double lon,

            @JsonProperty("createdAt")
            Long createdAt,

            @JsonProperty("DS100")
            String ds100,

            @JsonProperty("active")
            Boolean active,

            @JsonProperty("outdated")
            Boolean outdated) {
    }

}
