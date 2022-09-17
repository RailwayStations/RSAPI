package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * The creator of a photo
 */

@Schema(name = "Photographer", description = "The creator of a photo")
@JsonTypeName("Photographer")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-09-17T10:27:28.459965650+02:00[Europe/Berlin]")
public class PhotographerDto {

  @JsonProperty("name")
  private String name;

  @JsonProperty("url")
  private String url;

  public PhotographerDto name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Username of the photographer
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "Username of the photographer", required = true)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PhotographerDto url(String url) {
    this.url = url;
    return this;
  }

  /**
   * Link to the photographers social media account or homepage
   * @return url
  */
  
  @Schema(name = "url", description = "Link to the photographers social media account or homepage", required = false)
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
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

