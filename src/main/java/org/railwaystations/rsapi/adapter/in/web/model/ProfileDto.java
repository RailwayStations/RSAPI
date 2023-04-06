package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.util.Objects;

/**
 * User profile information
 */

@JsonTypeName("Profile")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-06T21:00:36.711673187+02:00[Europe/Berlin]")
public class ProfileDto {

    private String nickname;

    private String email;

    private LicenseDto license;

    private Boolean photoOwner;

    private URI link;

    private Boolean anonymous;

    private Boolean admin;

    private Boolean emailVerified;

    private Boolean sendNotifications;

    /**
     * Default constructor
     *
     * @deprecated Use {@link ProfileDto#ProfileDto(String, LicenseDto, Boolean)}
     */
    @Deprecated
    public ProfileDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public ProfileDto(String nickname, LicenseDto license, Boolean photoOwner) {
        this.nickname = nickname;
        this.license = license;
        this.photoOwner = photoOwner;
    }

    public ProfileDto nickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    /**
     * Get nickname
     *
     * @return nickname
     */
    @NotNull
    @JsonProperty("nickname")
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public ProfileDto email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Get email
     *
     * @return email
     */
    @Email
    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ProfileDto license(LicenseDto license) {
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

    public ProfileDto photoOwner(Boolean photoOwner) {
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

    public ProfileDto link(URI link) {
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

    public ProfileDto anonymous(Boolean anonymous) {
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

    public ProfileDto admin(Boolean admin) {
        this.admin = admin;
        return this;
    }

    /**
     * Get admin
     *
     * @return admin
     */

    @JsonProperty("admin")
    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public ProfileDto emailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
        return this;
    }

    /**
     * Get emailVerified
     *
     * @return emailVerified
     */

    @JsonProperty("emailVerified")
    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public ProfileDto sendNotifications(Boolean sendNotifications) {
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
        ProfileDto profile = (ProfileDto) o;
        return Objects.equals(this.nickname, profile.nickname) &&
                Objects.equals(this.email, profile.email) &&
                Objects.equals(this.license, profile.license) &&
                Objects.equals(this.photoOwner, profile.photoOwner) &&
                Objects.equals(this.link, profile.link) &&
                Objects.equals(this.anonymous, profile.anonymous) &&
                Objects.equals(this.admin, profile.admin) &&
                Objects.equals(this.emailVerified, profile.emailVerified) &&
                Objects.equals(this.sendNotifications, profile.sendNotifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname, email, license, photoOwner, link, anonymous, admin, emailVerified, sendNotifications);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProfileDto {\n");
        sb.append("    nickname: ").append(toIndentedString(nickname)).append("\n");
        sb.append("    email: ").append(toIndentedString(email)).append("\n");
        sb.append("    license: ").append(toIndentedString(license)).append("\n");
        sb.append("    photoOwner: ").append(toIndentedString(photoOwner)).append("\n");
        sb.append("    link: ").append(toIndentedString(link)).append("\n");
        sb.append("    anonymous: ").append(toIndentedString(anonymous)).append("\n");
        sb.append("    admin: ").append(toIndentedString(admin)).append("\n");
        sb.append("    emailVerified: ").append(toIndentedString(emailVerified)).append("\n");
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

