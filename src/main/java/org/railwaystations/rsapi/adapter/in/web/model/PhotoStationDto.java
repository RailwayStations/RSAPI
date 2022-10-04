package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A station with its photos
 */

@JsonTypeName("PhotoStation")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-04T21:43:45.819674334+02:00[Europe/Berlin]")
public class PhotoStationDto {

    @JsonProperty("country")
    private String country;

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("shortCode")
    private String shortCode;

    @JsonProperty("inactive")
    private Boolean inactive = false;

    @JsonProperty("photos")
    @Valid
    private List<PhotoDto> photos = new ArrayList<>();

    public PhotoStationDto country(String country) {
        this.country = country;
        return this;
    }

    /**
     * 2 letter code of the country
     *
     * @return country
     */
    @NotNull
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public PhotoStationDto id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Id of the station within the country
     *
     * @return id
     */
    @NotNull
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PhotoStationDto title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Title of the station
     *
     * @return title
     */
    @NotNull
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PhotoStationDto lat(Double lat) {
        this.lat = lat;
        return this;
    }

    /**
     * Latitude of the station
     *
     * @return lat
     */
    @NotNull
    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public PhotoStationDto lon(Double lon) {
        this.lon = lon;
        return this;
    }

    /**
     * Longitute of the station
     *
     * @return lon
     */
    @NotNull
    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public PhotoStationDto shortCode(String shortCode) {
        this.shortCode = shortCode;
        return this;
    }

    /**
     * Provider specific short code of the station, e.g. RIL100 or DS100 for german stations
     *
     * @return shortCode
     */

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public PhotoStationDto inactive(Boolean inactive) {
        this.inactive = inactive;
        return this;
    }

    /**
     * Indicates if this station is inactive
     *
     * @return inactive
     */

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }

    public PhotoStationDto photos(List<PhotoDto> photos) {
        this.photos = photos;
        return this;
    }

    public PhotoStationDto addPhotosItem(PhotoDto photosItem) {
        if (this.photos == null) {
            this.photos = new ArrayList<>();
        }
        this.photos.add(photosItem);
        return this;
    }

    /**
     * Photos of the station. If more than one photo is given, the first one is the primary photo. List might be empty or only the primary photo provided.
     *
     * @return photos
     */
    @NotNull
    @Valid
    public List<PhotoDto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoDto> photos) {
        this.photos = photos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhotoStationDto photoStation = (PhotoStationDto) o;
        return Objects.equals(this.country, photoStation.country) &&
                Objects.equals(this.id, photoStation.id) &&
                Objects.equals(this.title, photoStation.title) &&
                Objects.equals(this.lat, photoStation.lat) &&
                Objects.equals(this.lon, photoStation.lon) &&
                Objects.equals(this.shortCode, photoStation.shortCode) &&
                Objects.equals(this.inactive, photoStation.inactive) &&
                Objects.equals(this.photos, photoStation.photos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, id, title, lat, lon, shortCode, inactive, photos);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PhotoStationDto {\n");
        sb.append("    country: ").append(toIndentedString(country)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
        sb.append("    lon: ").append(toIndentedString(lon)).append("\n");
        sb.append("    shortCode: ").append(toIndentedString(shortCode)).append("\n");
        sb.append("    inactive: ").append(toIndentedString(inactive)).append("\n");
        sb.append("    photos: ").append(toIndentedString(photos)).append("\n");
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

