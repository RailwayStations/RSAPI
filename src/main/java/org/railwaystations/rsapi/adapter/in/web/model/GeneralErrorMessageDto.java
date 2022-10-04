package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * GeneralErrorMessageDto
 */

@JsonTypeName("GeneralErrorMessage")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-04T21:43:45.819674334+02:00[Europe/Berlin]")
public class GeneralErrorMessageDto {

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("path")
    private String path;

    public GeneralErrorMessageDto timestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Get timestamp
     *
     * @return timestamp
     */

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public GeneralErrorMessageDto status(Integer status) {
        this.status = status;
        return this;
    }

    /**
     * Get status
     *
     * @return status
     */
    @NotNull
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public GeneralErrorMessageDto error(String error) {
        this.error = error;
        return this;
    }

    /**
     * Get error
     *
     * @return error
     */

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public GeneralErrorMessageDto message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Get message
     *
     * @return message
     */
    @NotNull
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public GeneralErrorMessageDto path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Get path
     *
     * @return path
     */

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeneralErrorMessageDto generalErrorMessage = (GeneralErrorMessageDto) o;
        return Objects.equals(this.timestamp, generalErrorMessage.timestamp) &&
                Objects.equals(this.status, generalErrorMessage.status) &&
                Objects.equals(this.error, generalErrorMessage.error) &&
                Objects.equals(this.message, generalErrorMessage.message) &&
                Objects.equals(this.path, generalErrorMessage.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, status, error, message, path);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class GeneralErrorMessageDto {\n");
        sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    error: ").append(toIndentedString(error)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
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

