package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Provider App information
 */

@JsonTypeName("ProviderApp")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-06T21:00:36.711673187+02:00[Europe/Berlin]")
public class ProviderAppDto {

    /**
     * Gets or Sets type
     */
    public enum TypeEnum {
        ANDROID("android"),

        IOS("ios"),

        WEB("web");

        private String value;

        TypeEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static TypeEnum fromValue(String value) {
            for (TypeEnum b : TypeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private TypeEnum type;

    private String name;

    private String url;

    /**
     * Default constructor
     *
     * @deprecated Use {@link ProviderAppDto#ProviderAppDto(TypeEnum, String, String)}
     */
    @Deprecated
    public ProviderAppDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public ProviderAppDto(TypeEnum type, String name, String url) {
        this.type = type;
        this.name = name;
        this.url = url;
    }

    public ProviderAppDto type(TypeEnum type) {
        this.type = type;
        return this;
    }

    /**
     * Get type
     *
     * @return type
     */
    @NotNull
    @JsonProperty("type")
    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    public ProviderAppDto name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get name
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

    public ProviderAppDto url(String url) {
        this.url = url;
        return this;
    }

    /**
     * Get url
     *
     * @return url
     */
    @NotNull
    @JsonProperty("url")
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
        ProviderAppDto providerApp = (ProviderAppDto) o;
        return Objects.equals(this.type, providerApp.type) &&
                Objects.equals(this.name, providerApp.name) &&
                Objects.equals(this.url, providerApp.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, url);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProviderAppDto {\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

