package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * AdminInboxCommandResponseDto
 */

@JsonTypeName("AdminInboxCommandResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-23T18:41:39.638497575+02:00[Europe/Berlin]")
public class AdminInboxCommandResponseDto {

    private Integer status;

    private String message;

    /**
     * Default constructor
     *
     * @deprecated Use {@link AdminInboxCommandResponseDto#AdminInboxCommandResponseDto(Integer, String)}
     */
    @Deprecated
    public AdminInboxCommandResponseDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public AdminInboxCommandResponseDto(Integer status, String message) {
        this.status = status;
        this.message = message;
    }

    public AdminInboxCommandResponseDto status(Integer status) {
        this.status = status;
        return this;
    }

    /**
     * Get status
     *
     * @return status
     */
    @NotNull
    @JsonProperty("status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public AdminInboxCommandResponseDto message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Get message
     *
     * @return message
     */
    @NotNull
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AdminInboxCommandResponseDto adminInboxCommandResponse = (AdminInboxCommandResponseDto) o;
        return Objects.equals(this.status, adminInboxCommandResponse.status) &&
                Objects.equals(this.message, adminInboxCommandResponse.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AdminInboxCommandResponseDto {\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

