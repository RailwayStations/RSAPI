package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Generated;
import java.util.Objects;

/**
 * command to import or reject an inbox entry
 */

@Schema(name = "InboxCommand", description = "command to import or reject an inbox entry")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-04-25T21:59:47.380653632+02:00[Europe/Berlin]")
public class InboxCommandDto   {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("countryCode")
  private String countryCode;

  @JsonProperty("stationId")
  private String stationId;

  @JsonProperty("rejectReason")
  private String rejectReason;

  @JsonProperty("DS100")
  private String DS100;

  @JsonProperty("active")
  private Boolean active;

  @JsonProperty("ignoreConflict")
  private Boolean ignoreConflict;

  @JsonProperty("createStation")
  private Boolean createStation;

  /**
   * Gets or Sets command
   */
  public enum CommandEnum {
    IMPORT("IMPORT"),
    
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
  
  @Schema(name = "id", required = false)
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

  public InboxCommandDto ignoreConflict(Boolean ignoreConflict) {
    this.ignoreConflict = ignoreConflict;
    return this;
  }

  /**
   * ignore a conflict
   * @return ignoreConflict
  */
  
  @Schema(name = "ignoreConflict", description = "ignore a conflict", required = false)
  public Boolean getIgnoreConflict() {
    return ignoreConflict;
  }

  public void setIgnoreConflict(Boolean ignoreConflict) {
    this.ignoreConflict = ignoreConflict;
  }

  public InboxCommandDto createStation(Boolean createStation) {
    this.createStation = createStation;
    return this;
  }

  /**
   * create the station if it doesn't exist
   * @return createStation
  */
  
  @Schema(name = "createStation", description = "create the station if it doesn't exist", required = false)
  public Boolean getCreateStation() {
    return createStation;
  }

  public void setCreateStation(Boolean createStation) {
    this.createStation = createStation;
  }

  public InboxCommandDto command(CommandEnum command) {
    this.command = command;
    return this;
  }

  /**
   * Get command
   * @return command
  */
  
  @Schema(name = "command", required = false)
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
        Objects.equals(this.rejectReason, inboxCommand.rejectReason) &&
        Objects.equals(this.DS100, inboxCommand.DS100) &&
        Objects.equals(this.active, inboxCommand.active) &&
        Objects.equals(this.ignoreConflict, inboxCommand.ignoreConflict) &&
        Objects.equals(this.createStation, inboxCommand.createStation) &&
        Objects.equals(this.command, inboxCommand.command);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, countryCode, stationId, rejectReason, DS100, active, ignoreConflict, createStation, command);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InboxCommandDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
    sb.append("    stationId: ").append(toIndentedString(stationId)).append("\n");
    sb.append("    rejectReason: ").append(toIndentedString(rejectReason)).append("\n");
    sb.append("    DS100: ").append(toIndentedString(DS100)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    ignoreConflict: ").append(toIndentedString(ignoreConflict)).append("\n");
    sb.append("    createStation: ").append(toIndentedString(createStation)).append("\n");
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

