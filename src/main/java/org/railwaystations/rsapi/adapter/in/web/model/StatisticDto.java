package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Statistic of number of stations with and without photos
 */

@JsonTypeName("Statistic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-05T18:08:36.030268796+01:00[Europe/Berlin]")
public class StatisticDto {

    @JsonProperty("total")
    private Long total;

    @JsonProperty("withPhoto")
    private Long withPhoto;

    @JsonProperty("withoutPhoto")
    private Long withoutPhoto;

    @JsonProperty("photographers")
    private Long photographers;

    @JsonProperty("countryCode")
    private String countryCode = null;

    public StatisticDto total(Long total) {
        this.total = total;
        return this;
    }

    /**
     * Get total
     *
     * @return total
     */
    @NotNull
    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public StatisticDto withPhoto(Long withPhoto) {
        this.withPhoto = withPhoto;
        return this;
    }

    /**
     * Get withPhoto
     *
     * @return withPhoto
     */
    @NotNull
    public Long getWithPhoto() {
        return withPhoto;
    }

    public void setWithPhoto(Long withPhoto) {
        this.withPhoto = withPhoto;
    }

    public StatisticDto withoutPhoto(Long withoutPhoto) {
        this.withoutPhoto = withoutPhoto;
        return this;
    }

    /**
     * Get withoutPhoto
     *
     * @return withoutPhoto
     */
    @NotNull
    public Long getWithoutPhoto() {
        return withoutPhoto;
    }

    public void setWithoutPhoto(Long withoutPhoto) {
        this.withoutPhoto = withoutPhoto;
    }

    public StatisticDto photographers(Long photographers) {
        this.photographers = photographers;
        return this;
    }

    /**
     * Get photographers
     *
     * @return photographers
     */
    @NotNull
    public Long getPhotographers() {
        return photographers;
    }

    public void setPhotographers(Long photographers) {
        this.photographers = photographers;
    }

    public StatisticDto countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    /**
     * Get countryCode
     *
     * @return countryCode
     */

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatisticDto statistic = (StatisticDto) o;
        return Objects.equals(this.total, statistic.total) &&
                Objects.equals(this.withPhoto, statistic.withPhoto) &&
                Objects.equals(this.withoutPhoto, statistic.withoutPhoto) &&
                Objects.equals(this.photographers, statistic.photographers) &&
                Objects.equals(this.countryCode, statistic.countryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, withPhoto, withoutPhoto, photographers, countryCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class StatisticDto {\n");
        sb.append("    total: ").append(toIndentedString(total)).append("\n");
        sb.append("    withPhoto: ").append(toIndentedString(withPhoto)).append("\n");
        sb.append("    withoutPhoto: ").append(toIndentedString(withoutPhoto)).append("\n");
        sb.append("    photographers: ").append(toIndentedString(photographers)).append("\n");
        sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
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

