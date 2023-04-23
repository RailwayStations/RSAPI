package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Represents an uploaded photo under review
 */

@JsonTypeName("PublicInboxEntry")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-23T18:41:39.638497575+02:00[Europe/Berlin]")
public class PublicInboxEntryDto {

    private String countryCode;

    private String stationId;

    private String title;

    private Double lat;

    private Double lon;

    /**
     * Default constructor
     *
     * @deprecated Use {@link PublicInboxEntryDto#PublicInboxEntryDto(String, String, String, Double, Double)}
     */
    @Deprecated
    public PublicInboxEntryDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public PublicInboxEntryDto(String countryCode, String stationId, String title, Double lat, Double lon) {
        this.countryCode = countryCode;
        this.stationId = stationId;
        this.title = title;
        this.lat = lat;
        this.lon = lon;
    }

    public PublicInboxEntryDto countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    /**
     * Get countryCode
     *
     * @return countryCode
     */
    @NotNull
    @JsonProperty("countryCode")
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public PublicInboxEntryDto stationId(String stationId) {
        this.stationId = stationId;
        return this;
    }

    /**
     * Get stationId
     *
     * @return stationId
     */
    @NotNull
    @JsonProperty("stationId")
    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public PublicInboxEntryDto title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Get title
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

    public PublicInboxEntryDto lat(Double lat) {
        this.lat = lat;
        return this;
    }

    /**
     * Get lat
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

    public PublicInboxEntryDto lon(Double lon) {
        this.lon = lon;
        return this;
    }

    /**
     * Get lon
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublicInboxEntryDto publicInboxEntry = (PublicInboxEntryDto) o;
        return Objects.equals(this.countryCode, publicInboxEntry.countryCode) &&
                Objects.equals(this.stationId, publicInboxEntry.stationId) &&
                Objects.equals(this.title, publicInboxEntry.title) &&
                Objects.equals(this.lat, publicInboxEntry.lat) &&
                Objects.equals(this.lon, publicInboxEntry.lon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, stationId, title, lat, lon);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PublicInboxEntryDto {\n");
        sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
        sb.append("    stationId: ").append(toIndentedString(stationId)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
        sb.append("    lon: ").append(toIndentedString(lon)).append("\n");
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

