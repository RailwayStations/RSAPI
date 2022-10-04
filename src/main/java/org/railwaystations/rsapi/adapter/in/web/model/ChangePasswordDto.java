package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * ChangePasswordDto
 */

@JsonTypeName("ChangePassword")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-04T21:43:45.819674334+02:00[Europe/Berlin]")
public class ChangePasswordDto {

    @JsonProperty("newPassword")
    private String newPassword;

    public ChangePasswordDto newPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    /**
     * Get newPassword
     *
     * @return newPassword
     */
    @NotNull
    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChangePasswordDto changePassword = (ChangePasswordDto) o;
        return Objects.equals(this.newPassword, changePassword.newPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newPassword);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ChangePasswordDto {\n");
        sb.append("    newPassword: ").append(toIndentedString(newPassword)).append("\n");
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

