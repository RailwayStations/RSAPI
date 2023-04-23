package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.util.Objects;

/**
 * User profile information
 */

@JsonTypeName("UpdateProfile")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-23T18:54:33.312547466+02:00[Europe/Berlin]")
public class UpdateProfileDto {

    private String nickname;

    private String email;

    private LicenseDto license;

    private Boolean photoOwner;

    private URI link;

    private Boolean anonymous;

    private Boolean sendNotifications;

    /**
     * Default constructor
     *
     * @deprecated Use {@link UpdateProfileDto#UpdateProfileDto(String, String, LicenseDto, Boolean)}
     */
    @Deprecated
    public UpdateProfileDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public UpdateProfileDto(String nickname, String email, LicenseDto license, Boolean photoOwner) {
        this.nickname = nickname;
        this.email = email;
        this.license = license;
        this.photoOwner = photoOwner;
    }

    public UpdateProfileDto nickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    /**
     * Get nickname
     *
     * @return nickname
     */
    @NotNull
    @Size(min = 3, max = 50)
    @JsonProperty("nickname")
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public UpdateProfileDto email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Get email
     *
     * @return email
     */
    @NotNull
    @Size(min = 3, max = 100)
    @Email
    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UpdateProfileDto license(LicenseDto license) {
        this.license = license;
        return this;
    }

    /**
     * Get license
     *
     * @return license
     */
    @NotNull
    @Valid
    @JsonProperty("license")
    public LicenseDto getLicense() {
        return license;
    }

    public void setLicense(LicenseDto license) {
        this.license = license;
    }

    public UpdateProfileDto photoOwner(Boolean photoOwner) {
        this.photoOwner = photoOwner;
        return this;
    }

    /**
     * Get photoOwner
     *
     * @return photoOwner
     */
    @NotNull
    @JsonProperty("photoOwner")
    public Boolean getPhotoOwner() {
        return photoOwner;
    }

    public void setPhotoOwner(Boolean photoOwner) {
        this.photoOwner = photoOwner;
    }

    public UpdateProfileDto link(URI link) {
        this.link = link;
        return this;
    }

    /**
     * Get link
     *
     * @return link
     */
    @Valid
    @JsonProperty("link")
    public URI getLink() {
        return link;
    }

    public void setLink(URI link) {
        this.link = link;
    }

    public UpdateProfileDto anonymous(Boolean anonymous) {
        this.anonymous = anonymous;
        return this;
    }

    /**
     * Get anonymous
     *
     * @return anonymous
     */

    @JsonProperty("anonymous")
    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public UpdateProfileDto sendNotifications(Boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
        return this;
    }

    /**
     * Get sendNotifications
     *
     * @return sendNotifications
     */

    @JsonProperty("sendNotifications")
    public Boolean getSendNotifications() {
        return sendNotifications;
    }

    public void setSendNotifications(Boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateProfileDto updateProfile = (UpdateProfileDto) o;
        return Objects.equals(this.nickname, updateProfile.nickname) &&
                Objects.equals(this.email, updateProfile.email) &&
                Objects.equals(this.license, updateProfile.license) &&
                Objects.equals(this.photoOwner, updateProfile.photoOwner) &&
                Objects.equals(this.link, updateProfile.link) &&
                Objects.equals(this.anonymous, updateProfile.anonymous) &&
                Objects.equals(this.sendNotifications, updateProfile.sendNotifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname, email, license, photoOwner, link, anonymous, sendNotifications);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UpdateProfileDto {\n");
        sb.append("    nickname: ").append(toIndentedString(nickname)).append("\n");
        sb.append("    email: ").append(toIndentedString(email)).append("\n");
        sb.append("    license: ").append(toIndentedString(license)).append("\n");
        sb.append("    photoOwner: ").append(toIndentedString(photoOwner)).append("\n");
        sb.append("    link: ").append(toIndentedString(link)).append("\n");
        sb.append("    anonymous: ").append(toIndentedString(anonymous)).append("\n");
        sb.append("    sendNotifications: ").append(toIndentedString(sendNotifications)).append("\n");
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

