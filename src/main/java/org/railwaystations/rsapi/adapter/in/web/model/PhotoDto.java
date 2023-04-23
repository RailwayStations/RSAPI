package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * A photo of a station
 */

@JsonTypeName("Photo")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-23T18:41:39.638497575+02:00[Europe/Berlin]")
public class PhotoDto {

    private Long id;

    private String photographer;

    private String path;

    private Long createdAt;

    private String license;

    private Boolean outdated = false;

    /**
     * Default constructor
     *
     * @deprecated Use {@link PhotoDto#PhotoDto(Long, String, String, Long, String)}
     */
    @Deprecated
    public PhotoDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public PhotoDto(Long id, String photographer, String path, Long createdAt, String license) {
        this.id = id;
        this.photographer = photographer;
        this.path = path;
        this.createdAt = createdAt;
        this.license = license;
    }

    public PhotoDto id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Unique id of a photo
     *
     * @return id
     */
    @NotNull
    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PhotoDto photographer(String photographer) {
        this.photographer = photographer;
        return this;
    }

    /**
     * Name of the photographer
     *
     * @return photographer
     */
    @NotNull
    @JsonProperty("photographer")
    public String getPhotographer() {
        return photographer;
    }

    public void setPhotographer(String photographer) {
        this.photographer = photographer;
    }

    public PhotoDto path(String path) {
        this.path = path;
        return this;
    }

    /**
     * URL path to the photo, to be used together with the photoBaseUrl
     *
     * @return path
     */
    @NotNull
    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public PhotoDto createdAt(Long createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /**
     * Timestamp when the photo was created in the railway-stations database (Epoche milliseconds since 1.1.1970)
     *
     * @return createdAt
     */
    @NotNull
    @JsonProperty("createdAt")
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public PhotoDto license(String license) {
        this.license = license;
        return this;
    }

    /**
     * Id of the license used for this photo
     *
     * @return license
     */
    @NotNull
    @JsonProperty("license")
    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public PhotoDto outdated(Boolean outdated) {
        this.outdated = outdated;
        return this;
    }

    /**
     * Indicates if this photo is outdated
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
        PhotoDto photo = (PhotoDto) o;
        return Objects.equals(this.id, photo.id) &&
                Objects.equals(this.photographer, photo.photographer) &&
                Objects.equals(this.path, photo.path) &&
                Objects.equals(this.createdAt, photo.createdAt) &&
                Objects.equals(this.license, photo.license) &&
                Objects.equals(this.outdated, photo.outdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, photographer, path, createdAt, license, outdated);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PhotoDto {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    photographer: ").append(toIndentedString(photographer)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
        sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
        sb.append("    license: ").append(toIndentedString(license)).append("\n");
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

