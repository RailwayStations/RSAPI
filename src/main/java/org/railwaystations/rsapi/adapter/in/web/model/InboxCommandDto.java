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
 * command to import or reject an inbox entry
 */

@Schema(name = "InboxCommand", description = "command to import or reject an inbox entry")
@JsonTypeName("InboxCommand")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-09-17T10:27:28.459965650+02:00[Europe/Berlin]")
public class InboxCommandDto {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("countryCode")
  private String countryCode;

  @JsonProperty("stationId")
  private String stationId;

  @JsonProperty("title")
  private String title;

  @JsonProperty("lat")
  private Double lat;

  @JsonProperty("lon")
  private Double lon;

  @JsonProperty("rejectReason")
  private String rejectReason;

  @JsonProperty("DS100")
  private String DS100;

  @JsonProperty("active")
  private Boolean active;

  /**
   * how to handle conflicts
   */
  public enum ConflictResolutionEnum {
    DO_NOTHING("DO_NOTHING"),
    
    OVERWRITE_EXISTING_PHOTO("OVERWRITE_EXISTING_PHOTO"),
    
    IMPORT_AS_NEW_PRIMARY_PHOTO("IMPORT_AS_NEW_PRIMARY_PHOTO"),
    
    IMPORT_AS_NEW_SECONDARY_PHOTO("IMPORT_AS_NEW_SECONDARY_PHOTO"),
    
    IGNORE_NEARBY_STATION("IGNORE_NEARBY_STATION");

    private String value;

    ConflictResolutionEnum(String value) {
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
    public static ConflictResolutionEnum fromValue(String value) {
      for (ConflictResolutionEnum b : ConflictResolutionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("conflictResolution")
  private ConflictResolutionEnum conflictResolution;

  /**
   * Gets or Sets command
   */
  public enum CommandEnum {
    IMPORT_PHOTO("IMPORT_PHOTO"),
    
    IMPORT_MISSING_STATION("IMPORT_MISSING_STATION"),
    
    ACTIVATE_STATION("ACTIVATE_STATION"),
    
    DEACTIVATE_STATION("DEACTIVATE_STATION"),
    
    DELETE_STATION("DELETE_STATION"),
    
    DELETE_PHOTO("DELETE_PHOTO"),
    
    MARK_SOLVED("MARK_SOLVED"),
    
    REJECT("REJECT"),
    
    CHANGE_NAME("CHANGE_NAME"),
    
    UPDATE_LOCATION("UPDATE_LOCATION"),
    
    PHOTO_OUTDATED("PHOTO_OUTDATED");

    private String value;

    CommandEnum(String value) {
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
    public static CommandEnum fromValue(String value) {
      for (CommandEnum b : CommandEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("command")
  private CommandEnum command;

  public InboxCommandDto id(Long id) {
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

  public InboxCommandDto countryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  /**
   * country of a new station
   * @return countryCode
  */
  
  @Schema(name = "countryCode", description = "country of a new station", required = false)
  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public InboxCommandDto stationId(String stationId) {
    this.stationId = stationId;
    return this;
  }

  /**
   * ID of a new station
   * @return stationId
  */
  
  @Schema(name = "stationId", description = "ID of a new station", required = false)
  public String getStationId() {
    return stationId;
  }

  public void setStationId(String stationId) {
    this.stationId = stationId;
  }

  public InboxCommandDto title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
  */
  
  @Schema(name = "title", required = false)
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public InboxCommandDto lat(Double lat) {
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

  public InboxCommandDto lon(Double lon) {
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

  public InboxCommandDto rejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
    return this;
  }

  /**
   * explanation of a rejection
   * @return rejectReason
  */
  
  @Schema(name = "rejectReason", description = "explanation of a rejection", required = false)
  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }

  public InboxCommandDto DS100(String DS100) {
    this.DS100 = DS100;
    return this;
  }

  /**
   * DS100 attribute of a new station
   * @return DS100
  */
  
  @Schema(name = "DS100", description = "DS100 attribute of a new station", required = false)
  public String getDS100() {
    return DS100;
  }

  public void setDS100(String DS100) {
    this.DS100 = DS100;
  }

  public InboxCommandDto active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * active flag of a new station (default true)
   * @return active
  */
  
  @Schema(name = "active", description = "active flag of a new station (default true)", required = false)
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public InboxCommandDto conflictResolution(ConflictResolutionEnum conflictResolution) {
    this.conflictResolution = conflictResolution;
    return this;
  }

  /**
   * how to handle conflicts
   * @return conflictResolution
  */
  
  @Schema(name = "conflictResolution", description = "how to handle conflicts", required = false)
  public ConflictResolutionEnum getConflictResolution() {
    return conflictResolution;
  }

  public void setConflictResolution(ConflictResolutionEnum conflictResolution) {
    this.conflictResolution = conflictResolution;
  }

  public InboxCommandDto command(CommandEnum command) {
    this.command = command;
    return this;
  }

  /**
   * Get command
   * @return command
  */
  @NotNull 
  @Schema(name = "command", required = true)
  public CommandEnum getCommand() {
    return command;
  }

  public void setCommand(CommandEnum command) {
    this.command = command;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InboxCommandDto inboxCommand = (InboxCommandDto) o;
    return Objects.equals(this.id, inboxCommand.id) &&
        Objects.equals(this.countryCode, inboxCommand.countryCode) &&
        Objects.equals(this.stationId, inboxCommand.stationId) &&
        Objects.equals(this.title, inboxCommand.title) &&
        Objects.equals(this.lat, inboxCommand.lat) &&
        Objects.equals(this.lon, inboxCommand.lon) &&
        Objects.equals(this.rejectReason, inboxCommand.rejectReason) &&
        Objects.equals(this.DS100, inboxCommand.DS100) &&
        Objects.equals(this.active, inboxCommand.active) &&
        Objects.equals(this.conflictResolution, inboxCommand.conflictResolution) &&
        Objects.equals(this.command, inboxCommand.command);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, countryCode, stationId, title, lat, lon, rejectReason, DS100, active, conflictResolution, command);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InboxCommandDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
    sb.append("    stationId: ").append(toIndentedString(stationId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
    sb.append("    lon: ").append(toIndentedString(lon)).append("\n");
    sb.append("    rejectReason: ").append(toIndentedString(rejectReason)).append("\n");
    sb.append("    DS100: ").append(toIndentedString(DS100)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    conflictResolution: ").append(toIndentedString(conflictResolution)).append("\n");
    sb.append("    command: ").append(toIndentedString(command)).append("\n");
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

