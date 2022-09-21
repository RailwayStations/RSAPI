package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * User profile information
 */

@JsonTypeName("Profile")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-09-21T21:45:29.210227244+02:00[Europe/Berlin]")
public class ProfileDto {

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("email")
    private String email;

    @JsonProperty("license")
    private LicenseDto license;

    @JsonProperty("photoOwner")
    private Boolean photoOwner;

    @JsonProperty("link")
    private String link;

    @JsonProperty("anonymous")
    private Boolean anonymous;

    @JsonProperty("admin")
    private Boolean admin;

    @JsonProperty("newPassword")
    private String newPassword;

    @JsonProperty("emailVerified")
    private Boolean emailVerified;

    @JsonProperty("sendNotifications")
    private Boolean sendNotifications;

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
    public Boolean getPhotoOwner() {
        return photoOwner;
    }

    public void setPhotoOwner(Boolean photoOwner) {
        this.photoOwner = photoOwner;
    }

    public ProfileDto link(String link) {
        this.link = link;
        return this;
    }

    /**
     * Get link
     *
     * @return link
     */

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
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

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public ProfileDto newPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    /**
     * Get newPassword
     *
     * @return newPassword
     */

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
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
                Objects.equals(this.newPassword, profile.newPassword) &&
                Objects.equals(this.emailVerified, profile.emailVerified) &&
                Objects.equals(this.sendNotifications, profile.sendNotifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname, email, license, photoOwner, link, anonymous, admin, newPassword, emailVerified, sendNotifications);
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
        sb.append("    newPassword: ").append(toIndentedString(newPassword)).append("\n");
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

