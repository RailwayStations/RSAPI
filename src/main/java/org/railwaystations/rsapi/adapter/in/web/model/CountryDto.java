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
 * Supported Country with its configuration
 */

@JsonTypeName("Country")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-11-26T11:22:49.165554890+01:00[Europe/Berlin]")
public class CountryDto {

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("twitterTags")
    private String twitterTags;

    @JsonProperty("timetableUrlTemplate")
    private String timetableUrlTemplate;

    @JsonProperty("overrideLicense")
    private String overrideLicense;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("providerApps")
    @Valid
    private List<ProviderAppDto> providerApps = null;

    public CountryDto code(String code) {
        this.code = code;
        return this;
    }

    /**
     * Two letter country code
     *
     * @return code
     */
    @NotNull
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public CountryDto name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Name of the country
     *
     * @return name
     */
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CountryDto email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Email Address to send photos to
     *
     * @return email
     */

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CountryDto twitterTags(String twitterTags) {
        this.twitterTags = twitterTags;
        return this;
    }

    /**
     * Twitter Tags when sharing the photo on Twitter
     *
     * @return twitterTags
     */

    public String getTwitterTags() {
        return twitterTags;
    }

    public void setTwitterTags(String twitterTags) {
        this.twitterTags = twitterTags;
    }

    public CountryDto timetableUrlTemplate(String timetableUrlTemplate) {
        this.timetableUrlTemplate = timetableUrlTemplate;
        return this;
    }

    /**
     * URL template for the timetable, contains {title}, {id} and {DS100} placeholders which need to be replaced
     *
     * @return timetableUrlTemplate
     */

    public String getTimetableUrlTemplate() {
        return timetableUrlTemplate;
    }

    public void setTimetableUrlTemplate(String timetableUrlTemplate) {
        this.timetableUrlTemplate = timetableUrlTemplate;
    }

    public CountryDto overrideLicense(String overrideLicense) {
        this.overrideLicense = overrideLicense;
        return this;
    }

    /**
     * if a country needs a special license
     *
     * @return overrideLicense
     */

    public String getOverrideLicense() {
        return overrideLicense;
    }

    public void setOverrideLicense(String overrideLicense) {
        this.overrideLicense = overrideLicense;
    }

    public CountryDto active(Boolean active) {
        this.active = active;
        return this;
    }

    /**
     * Is this an active country where we collect photos?
     *
     * @return active
     */
    @NotNull
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public CountryDto providerApps(List<ProviderAppDto> providerApps) {
        this.providerApps = providerApps;
        return this;
    }

    public CountryDto addProviderAppsItem(ProviderAppDto providerAppsItem) {
        if (this.providerApps == null) {
            this.providerApps = new ArrayList<>();
        }
        this.providerApps.add(providerAppsItem);
        return this;
    }

    /**
     * array with links to provider apps
     *
     * @return providerApps
     */
    @Valid
    public List<ProviderAppDto> getProviderApps() {
        return providerApps;
    }

    public void setProviderApps(List<ProviderAppDto> providerApps) {
        this.providerApps = providerApps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CountryDto country = (CountryDto) o;
        return Objects.equals(this.code, country.code) &&
                Objects.equals(this.name, country.name) &&
                Objects.equals(this.email, country.email) &&
                Objects.equals(this.twitterTags, country.twitterTags) &&
                Objects.equals(this.timetableUrlTemplate, country.timetableUrlTemplate) &&
                Objects.equals(this.overrideLicense, country.overrideLicense) &&
                Objects.equals(this.active, country.active) &&
                Objects.equals(this.providerApps, country.providerApps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, email, twitterTags, timetableUrlTemplate, overrideLicense, active, providerApps);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CountryDto {\n");
        sb.append("    code: ").append(toIndentedString(code)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    email: ").append(toIndentedString(email)).append("\n");
        sb.append("    twitterTags: ").append(toIndentedString(twitterTags)).append("\n");
        sb.append("    timetableUrlTemplate: ").append(toIndentedString(timetableUrlTemplate)).append("\n");
        sb.append("    overrideLicense: ").append(toIndentedString(overrideLicense)).append("\n");
        sb.append("    active: ").append(toIndentedString(active)).append("\n");
        sb.append("    providerApps: ").append(toIndentedString(providerApps)).append("\n");
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

