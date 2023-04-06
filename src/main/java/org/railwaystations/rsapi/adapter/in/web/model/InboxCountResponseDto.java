package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * counts the pending inbox entries
 */

@JsonTypeName("InboxCountResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-06T21:00:36.711673187+02:00[Europe/Berlin]")
public class InboxCountResponseDto {

    private Long pendingInboxEntries;

    /**
     * Default constructor
     *
     * @deprecated Use {@link InboxCountResponseDto#InboxCountResponseDto(Long)}
     */
    @Deprecated
    public InboxCountResponseDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public InboxCountResponseDto(Long pendingInboxEntries) {
        this.pendingInboxEntries = pendingInboxEntries;
    }

    public InboxCountResponseDto pendingInboxEntries(Long pendingInboxEntries) {
        this.pendingInboxEntries = pendingInboxEntries;
        return this;
    }

    /**
     * Get pendingInboxEntries
     *
     * @return pendingInboxEntries
     */
    @NotNull
    @JsonProperty("pendingInboxEntries")
    public Long getPendingInboxEntries() {
        return pendingInboxEntries;
    }

    public void setPendingInboxEntries(Long pendingInboxEntries) {
        this.pendingInboxEntries = pendingInboxEntries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InboxCountResponseDto inboxCountResponse = (InboxCountResponseDto) o;
        return Objects.equals(this.pendingInboxEntries, inboxCountResponse.pendingInboxEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pendingInboxEntries);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class InboxCountResponseDto {\n");
        sb.append("    pendingInboxEntries: ").append(toIndentedString(pendingInboxEntries)).append("\n");
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

