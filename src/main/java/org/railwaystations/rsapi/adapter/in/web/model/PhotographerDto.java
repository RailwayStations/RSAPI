package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.util.Objects;

/**
 * The creator of a photo
 */

@JsonTypeName("Photographer")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-23T18:41:39.638497575+02:00[Europe/Berlin]")
public class PhotographerDto {

    private String name;

    private URI url;

    /**
     * Default constructor
     *
     * @deprecated Use {@link PhotographerDto#PhotographerDto(String)}
     */
    @Deprecated
    public PhotographerDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public PhotographerDto(String name) {
        this.name = name;
    }

    public PhotographerDto name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Username of the photographer
     *
     * @return name
     */
    @NotNull
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PhotographerDto url(URI url) {
        this.url = url;
        return this;
    }

    /**
     * Link to the photographers social media account or homepage
     *
     * @return url
     */
    @Valid
    @JsonProperty("url")
    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhotographerDto photographer = (PhotographerDto) o;
        return Objects.equals(this.name, photographer.name) &&
                Objects.equals(this.url, photographer.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PhotographerDto {\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
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

