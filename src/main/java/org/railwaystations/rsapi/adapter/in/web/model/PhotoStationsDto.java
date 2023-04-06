package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stations with photos
 */

@JsonTypeName("PhotoStations")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-06T21:00:36.711673187+02:00[Europe/Berlin]")
public class PhotoStationsDto {

    private String photoBaseUrl;

    @Valid
    private List<@Valid PhotoLicenseDto> licenses = new ArrayList<>();

    @Valid
    private List<@Valid PhotographerDto> photographers = new ArrayList<>();

    @Valid
    private List<@Valid PhotoStationDto> stations = new ArrayList<>();

    /**
     * Default constructor
     *
     * @deprecated Use {@link PhotoStationsDto#PhotoStationsDto(String, List<@Valid PhotoLicenseDto>, List<@Valid PhotographerDto>, List<@Valid PhotoStationDto>)}
     */
    @Deprecated
    public PhotoStationsDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public PhotoStationsDto(String photoBaseUrl, List<@Valid PhotoLicenseDto> licenses, List<@Valid PhotographerDto> photographers, List<@Valid PhotoStationDto> stations) {
        this.photoBaseUrl = photoBaseUrl;
        this.licenses = licenses;
        this.photographers = photographers;
        this.stations = stations;
    }

    public PhotoStationsDto photoBaseUrl(String photoBaseUrl) {
        this.photoBaseUrl = photoBaseUrl;
        return this;
    }

    /**
     * Base URL of all photos
     *
     * @return photoBaseUrl
     */
    @NotNull
    @JsonProperty("photoBaseUrl")
    public String getPhotoBaseUrl() {
        return photoBaseUrl;
    }

    public void setPhotoBaseUrl(String photoBaseUrl) {
        this.photoBaseUrl = photoBaseUrl;
    }

    public PhotoStationsDto licenses(List<@Valid PhotoLicenseDto> licenses) {
        this.licenses = licenses;
        return this;
    }

    public PhotoStationsDto addLicensesItem(PhotoLicenseDto licensesItem) {
        if (this.licenses == null) {
            this.licenses = new ArrayList<>();
        }
        this.licenses.add(licensesItem);
        return this;
    }

    /**
     * List of used licenses, might be empty if no photos available
     *
     * @return licenses
     */
    @NotNull
    @Valid
    @JsonProperty("licenses")
    public List<@Valid PhotoLicenseDto> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<@Valid PhotoLicenseDto> licenses) {
        this.licenses = licenses;
    }

    public PhotoStationsDto photographers(List<@Valid PhotographerDto> photographers) {
        this.photographers = photographers;
        return this;
    }

    public PhotoStationsDto addPhotographersItem(PhotographerDto photographersItem) {
        if (this.photographers == null) {
            this.photographers = new ArrayList<>();
        }
        this.photographers.add(photographersItem);
        return this;
    }

    /**
     * List of all photographers, might be empty if no photos available
     *
     * @return photographers
     */
    @NotNull
    @Valid
    @JsonProperty("photographers")
    public List<@Valid PhotographerDto> getPhotographers() {
        return photographers;
    }

    public void setPhotographers(List<@Valid PhotographerDto> photographers) {
        this.photographers = photographers;
    }

    public PhotoStationsDto stations(List<@Valid PhotoStationDto> stations) {
        this.stations = stations;
        return this;
    }

    public PhotoStationsDto addStationsItem(PhotoStationDto stationsItem) {
        if (this.stations == null) {
            this.stations = new ArrayList<>();
        }
        this.stations.add(stationsItem);
        return this;
    }

    /**
     * List of the stations
     *
     * @return stations
     */
    @NotNull
    @Valid
    @JsonProperty("stations")
    public List<@Valid PhotoStationDto> getStations() {
        return stations;
    }

    public void setStations(List<@Valid PhotoStationDto> stations) {
        this.stations = stations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhotoStationsDto photoStations = (PhotoStationsDto) o;
        return Objects.equals(this.photoBaseUrl, photoStations.photoBaseUrl) &&
                Objects.equals(this.licenses, photoStations.licenses) &&
                Objects.equals(this.photographers, photoStations.photographers) &&
                Objects.equals(this.stations, photoStations.stations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(photoBaseUrl, licenses, photographers, stations);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PhotoStationsDto {\n");
        sb.append("    photoBaseUrl: ").append(toIndentedString(photoBaseUrl)).append("\n");
        sb.append("    licenses: ").append(toIndentedString(licenses)).append("\n");
        sb.append("    photographers: ").append(toIndentedString(photographers)).append("\n");
        sb.append("    stations: ").append(toIndentedString(stations)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

