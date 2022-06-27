package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Generated;
import java.util.Objects;

/**
 * Response status of photo uploads and problem reports
 */

@Schema(name = "InboxResponse", description = "Response status of photo uploads and problem reports")
@JsonTypeName("InboxResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-06-27T19:01:27.797025753+02:00[Europe/Berlin]")
public class InboxResponseDto {

  /**
   * Gets or Sets state
   */
  public enum StateEnum {
    REVIEW("REVIEW"),
    
    LAT_LON_OUT_OF_RANGE("LAT_LON_OUT_OF_RANGE"),
    
    NOT_ENOUGH_DATA("NOT_ENOUGH_DATA"),
    
    UNSUPPORTED_CONTENT_TYPE("UNSUPPORTED_CONTENT_TYPE"),
    
    PHOTO_TOO_LARGE("PHOTO_TOO_LARGE"),
    
    CONFLICT("CONFLICT"),
    
    UNAUTHORIZED("UNAUTHORIZED"),
    
    ERROR("ERROR");

    private String value;

    StateEnum(String value) {
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
    public static StateEnum fromValue(String value) {
      for (StateEnum b : StateEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("state")
  private StateEnum state;

  @JsonProperty("message")
  private String message;

  @JsonProperty("id")
  private Long id;

  @JsonProperty("filename")
  private String filename;

  @JsonProperty("inboxUrl")
  private String inboxUrl;

  @JsonProperty("crc32")
  private Long crc32;

  public InboxResponseDto state(StateEnum state) {
    this.state = state;
    return this;
  }

  /**
   * Get state
   * @return state
  */
  
  @Schema(name = "state", required = false)
  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }

  public InboxResponseDto message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
  */
  
  @Schema(name = "message", required = false)
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public InboxResponseDto id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  
  @Schema(name = "id", required = false)
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public InboxResponseDto filename(String filename) {
    this.filename = filename;
    return this;
  }

  /**
   * filename in inbox
   * @return filename
  */
  
  @Schema(name = "filename", description = "filename in inbox", required = false)
  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public InboxResponseDto inboxUrl(String inboxUrl) {
    this.inboxUrl = inboxUrl;
    return this;
  }

  /**
   * url of the photo in the inbox
   * @return inboxUrl
  */
  
  @Schema(name = "inboxUrl", description = "url of the photo in the inbox", required = false)
  public String getInboxUrl() {
    return inboxUrl;
  }

  public void setInboxUrl(String inboxUrl) {
    this.inboxUrl = inboxUrl;
  }

  public InboxResponseDto crc32(Long crc32) {
    this.crc32 = crc32;
    return this;
  }

  /**
   * CRC32 checksum of the uploaded photo
   * @return crc32
  */
  
  @Schema(name = "crc32", description = "CRC32 checksum of the uploaded photo", required = false)
  public Long getCrc32() {
    return crc32;
  }

  public void setCrc32(Long crc32) {
    this.crc32 = crc32;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InboxResponseDto inboxResponse = (InboxResponseDto) o;
    return Objects.equals(this.state, inboxResponse.state) &&
        Objects.equals(this.message, inboxResponse.message) &&
        Objects.equals(this.id, inboxResponse.id) &&
        Objects.equals(this.filename, inboxResponse.filename) &&
        Objects.equals(this.inboxUrl, inboxResponse.inboxUrl) &&
        Objects.equals(this.crc32, inboxResponse.crc32);
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, message, id, filename, inboxUrl, crc32);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InboxResponseDto {\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    filename: ").append(toIndentedString(filename)).append("\n");
    sb.append("    inboxUrl: ").append(toIndentedString(inboxUrl)).append("\n");
    sb.append("    crc32: ").append(toIndentedString(crc32)).append("\n");
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

