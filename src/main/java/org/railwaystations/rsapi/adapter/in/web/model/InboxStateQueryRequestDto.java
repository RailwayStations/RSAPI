package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * InboxStateQueryRequestDto
 */

@JsonTypeName("InboxStateQueryRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-11-26T11:22:49.165554890+01:00[Europe/Berlin]")
public class InboxStateQueryRequestDto {

    @JsonProperty("id")
    private Long id;

    public InboxStateQueryRequestDto id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Get id
     *
     * @return id
     */
    @NotNull
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InboxStateQueryRequestDto inboxStateQueryRequest = (InboxStateQueryRequestDto) o;
        return Objects.equals(this.id, inboxStateQueryRequest.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class InboxStateQueryRequestDto {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

