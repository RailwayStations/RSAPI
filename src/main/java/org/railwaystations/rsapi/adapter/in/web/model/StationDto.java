package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * A railway-station object with location and optional photo
 */

@JsonTypeName("Station")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-23T18:41:39.638497575+02:00[Europe/Berlin]")
public class StationDto {

    private String idStr;

    private Long id;

    private String country;

    private String title;

    private String photographer;

    private String photographerUrl;

    private String photoUrl;

    private Long photoId;

    private String license;

    private String licenseUrl;

    private Double lat;

    private Double lon;

    private Long createdAt;

    private String DS100;

    private Boolean active;

    private Boolean outdated;

    /**
     * Default constructor
     *
     * @deprecated Use {@link StationDto#StationDto(String, String, String, Double, Double, Boolean)}
     */
    @Deprecated
    public StationDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public StationDto(String idStr, String country, String title, Double lat, Double lon, Boolean active) {
        this.idStr = idStr;
        this.country = country;
        this.title = title;
        this.lat = lat;
        this.lon = lon;
        this.active = active;
    }

    public StationDto idStr(String idStr) {
        this.idStr = idStr;
        return this;
    }

    /**
     * Unique ID of the station per country
     *
     * @return idStr
     */
    @NotNull
    @JsonProperty("idStr")
    public String getIdStr() {
        return idStr;
    }

    public void setIdStr(String idStr) {
        this.idStr = idStr;
    }

    public StationDto id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Unique (numeric) ID of the station per country
     *
     * @return id
     */

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StationDto country(String country) {
        this.country = country;
        return this;
    }

    /**
     * Two character country code
     *
     * @return country
     */
    @NotNull
    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public StationDto title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Name of the station
     *
     * @return title
     */
    @NotNull
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public StationDto photographer(String photographer) {
        this.photographer = photographer;
        return this;
    }

    /**
     * Nickname of the photographer
     *
     * @return photographer
     */

    @JsonProperty("photographer")
    public String getPhotographer() {
        return photographer;
    }

    public void setPhotographer(String photographer) {
        this.photographer = photographer;
    }

    public StationDto photographerUrl(String photographerUrl) {
        this.photographerUrl = photographerUrl;
        return this;
    }

    /**
     * Link to the photographer
     *
     * @return photographerUrl
     */

    @JsonProperty("photographerUrl")
    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public void setPhotographerUrl(String photographerUrl) {
        this.photographerUrl = photographerUrl;
    }

    public StationDto photoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
        return this;
    }

    /**
     * URL of the photo
     *
     * @return photoUrl
     */

    @JsonProperty("photoUrl")
    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public StationDto photoId(Long photoId) {
        this.photoId = photoId;
        return this;
    }

    /**
     * ID of the photo
     *
     * @return photoId
     */

    @JsonProperty("photoId")
    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
    }

    public StationDto license(String license) {
        this.license = license;
        return this;
    }

    /**
     * License of the photo, clients need to link to an online version of the license text
     *
     * @return license
     */

    @JsonProperty("license")
    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public StationDto licenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
        return this;
    }

    /**
     * The URL of the license text. Clients should use this to link to the online version of the license text
     *
     * @return licenseUrl
     */

    @JsonProperty("licenseUrl")
    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public StationDto lat(Double lat) {
        this.lat = lat;
        return this;
    }

    /**
     * Latitude of the station
     *
     * @return lat
     */
    @NotNull
    @JsonProperty("lat")
    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public StationDto lon(Double lon) {
        this.lon = lon;
        return this;
    }

    /**
     * Longitute of the station
     *
     * @return lon
     */
    @NotNull
    @JsonProperty("lon")
    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public StationDto createdAt(Long createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Timestamp when the photo was created in the railway-stations database (Epoche milliseconds since 1.1.1970)
     *
     * @return createdAt
     */

    @JsonProperty("createdAt")
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public StationDto DS100(String DS100) {
        this.DS100 = DS100;
        return this;
    }

    /**
     * A short code of the station, depending on the country
     *
     * @return DS100
     */

    @JsonProperty("DS100")
    public String getDS100() {
        return DS100;
    }

    public void setDS100(String DS100) {
        this.DS100 = DS100;
    }

    public StationDto active(Boolean active) {
        this.active = active;
        return this;
    }

    /**
     * Get active
     *
     * @return active
     */
    @NotNull
    @JsonProperty("active")
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public StationDto outdated(Boolean outdated) {
        this.outdated = outdated;
        return this;
    }

    /**
     * Get outdated
     *
     * @return outdated
     */

    @JsonProperty("outdated")
    public Boolean getOutdated() {
        return outdated;
    }

    public void setOutdated(Boolean outdated) {
        this.outdated = outdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StationDto station = (StationDto) o;
        return Objects.equals(this.idStr, station.idStr) &&
                Objects.equals(this.id, station.id) &&
                Objects.equals(this.country, station.country) &&
                Objects.equals(this.title, station.title) &&
                Objects.equals(this.photographer, station.photographer) &&
                Objects.equals(this.photographerUrl, station.photographerUrl) &&
                Objects.equals(this.photoUrl, station.photoUrl) &&
                Objects.equals(this.photoId, station.photoId) &&
                Objects.equals(this.license, station.license) &&
                Objects.equals(this.licenseUrl, station.licenseUrl) &&
                Objects.equals(this.lat, station.lat) &&
                Objects.equals(this.lon, station.lon) &&
                Objects.equals(this.createdAt, station.createdAt) &&
                Objects.equals(this.DS100, station.DS100) &&
                Objects.equals(this.active, station.active) &&
                Objects.equals(this.outdated, station.outdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idStr, id, country, title, photographer, photographerUrl, photoUrl, photoId, license, licenseUrl, lat, lon, createdAt, DS100, active, outdated);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StationDto {\n");
        sb.append("    idStr: ").append(toIndentedString(idStr)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    country: ").append(toIndentedString(country)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    photographer: ").append(toIndentedString(photographer)).append("\n");
        sb.append("    photographerUrl: ").append(toIndentedString(photographerUrl)).append("\n");
        sb.append("    photoUrl: ").append(toIndentedString(photoUrl)).append("\n");
        sb.append("    photoId: ").append(toIndentedString(photoId)).append("\n");
        sb.append("    license: ").append(toIndentedString(license)).append("\n");
        sb.append("    licenseUrl: ").append(toIndentedString(licenseUrl)).append("\n");
        sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
        sb.append("    lon: ").append(toIndentedString(lon)).append("\n");
        sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
        sb.append("    DS100: ").append(toIndentedString(DS100)).append("\n");
        sb.append("    active: ").append(toIndentedString(active)).append("\n");
        sb.append("    outdated: ").append(toIndentedString(outdated)).append("\n");
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

