package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Inbox state query
 */

@Schema(name = "InboxStateQueryResponse", description = "Inbox state query")
@JsonTypeName("InboxStateQueryResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-06-27T19:01:27.797025753+02:00[Europe/Berlin]")
public class InboxStateQueryResponseDto {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("countryCode")
  private String countryCode;

  @JsonProperty("stationId")
  private String stationId;

  @JsonProperty("lat")
  private Double lat;

  @JsonProperty("lon")
  private Double lon;

  @JsonProperty("rejectedReason")
  private String rejectedReason;

  @JsonProperty("filename")
  private String filename;

  @JsonProperty("inboxUrl")
  private String inboxUrl;

  @JsonProperty("crc32")
  private Long crc32;

  /**
   * Gets or Sets state
   */
  public enum StateEnum {
    UNKNOWN("UNKNOWN"),
    
    REVIEW("REVIEW"),
    
    CONFLICT("CONFLICT"),
    
    ACCEPTED("ACCEPTED"),
    
    REJECTED("REJECTED");

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

  public InboxStateQueryResponseDto id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  @NotNull 
  @Schema(name = "id", required = true)
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public InboxStateQueryResponseDto countryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  /**
   * Get countryCode
   * @return countryCode
  */
  
  @Schema(name = "countryCode", required = false)
  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public InboxStateQueryResponseDto stationId(String stationId) {
    this.stationId = stationId;
    return this;
  }

  /**
   * Get stationId
   * @return stationId
  */
  
  @Schema(name = "stationId", required = false)
  public String getStationId() {
    return stationId;
  }

  public void setStationId(String stationId) {
    this.stationId = stationId;
  }

  public InboxStateQueryResponseDto lat(Double lat) {
    this.lat = lat;
    return this;
  }

  /**
   * Get lat
   * @return lat
  */
  
  @Schema(name = "lat", required = false)
  public Double getLat() {
    return lat;
  }

  public void setLat(Double lat) {
    this.lat = lat;
  }

  public InboxStateQueryResponseDto lon(Double lon) {
    this.lon = lon;
    return this;
  }

  /**
   * Get lon
   * @return lon
  */
  
  @Schema(name = "lon", required = false)
  public Double getLon() {
    return lon;
  }

  public void setLon(Double lon) {
    this.lon = lon;
  }

  public InboxStateQueryResponseDto rejectedReason(String rejectedReason) {
    this.rejectedReason = rejectedReason;
    return this;
  }

  /**
   * Get rejectedReason
   * @return rejectedReason
  */
  
  @Schema(name = "rejectedReason", required = false)
  public String getRejectedReason() {
    return rejectedReason;
  }

  public void setRejectedReason(String rejectedReason) {
    this.rejectedReason = rejectedReason;
  }

  public InboxStateQueryResponseDto filename(String filename) {
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

  public InboxStateQueryResponseDto inboxUrl(String inboxUrl) {
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

  public InboxStateQueryResponseDto crc32(Long crc32) {
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

  public InboxStateQueryResponseDto state(StateEnum state) {
    this.state = state;
    return this;
  }

  /**
   * Get state
   * @return state
  */
  @NotNull 
  @Schema(name = "state", required = true)
  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InboxStateQueryResponseDto inboxStateQueryResponse = (InboxStateQueryResponseDto) o;
    return Objects.equals(this.id, inboxStateQueryResponse.id) &&
        Objects.equals(this.countryCode, inboxStateQueryResponse.countryCode) &&
        Objects.equals(this.stationId, inboxStateQueryResponse.stationId) &&
        Objects.equals(this.lat, inboxStateQueryResponse.lat) &&
        Objects.equals(this.lon, inboxStateQueryResponse.lon) &&
        Objects.equals(this.rejectedReason, inboxStateQueryResponse.rejectedReason) &&
        Objects.equals(this.filename, inboxStateQueryResponse.filename) &&
        Objects.equals(this.inboxUrl, inboxStateQueryResponse.inboxUrl) &&
        Objects.equals(this.crc32, inboxStateQueryResponse.crc32) &&
        Objects.equals(this.state, inboxStateQueryResponse.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, countryCode, stationId, lat, lon, rejectedReason, filename, inboxUrl, crc32, state);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InboxStateQueryResponseDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
    sb.append("    stationId: ").append(toIndentedString(stationId)).append("\n");
    sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
    sb.append("    lon: ").append(toIndentedString(lon)).append("\n");
    sb.append("    rejectedReason: ").append(toIndentedString(rejectedReason)).append("\n");
    sb.append("    filename: ").append(toIndentedString(filename)).append("\n");
    sb.append("    inboxUrl: ").append(toIndentedString(inboxUrl)).append("\n");
    sb.append("    crc32: ").append(toIndentedString(crc32)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
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

