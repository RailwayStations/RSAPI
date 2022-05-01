package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Generated;
import java.util.Objects;

/**
 * Represents an uploaded photo with processing state
 */

@Schema(name = "InboxEntry", description = "Represents an uploaded photo with processing state")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-05-01T17:38:12.901376066+02:00[Europe/Berlin]")
public class InboxEntryDto   {

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

  @JsonProperty("photographerNickname")
  private String photographerNickname;

  @JsonProperty("photographerEmail")
  private String photographerEmail;

  @JsonProperty("comment")
  private String comment;

  @JsonProperty("createdAt")
  private Long createdAt;

  @JsonProperty("done")
  private Boolean done;

  @JsonProperty("filename")
  private String filename;

  @JsonProperty("inboxUrl")
  private String inboxUrl;

  @JsonProperty("hasPhoto")
  private Boolean hasPhoto;

  @JsonProperty("hasConflict")
  private Boolean hasConflict;

  /**
   * Gets or Sets problemReportType
   */
  public enum ProblemReportTypeEnum {
    WRONG_LOCATION("WRONG_LOCATION"),
    
    STATION_INACTIVE("STATION_INACTIVE"),
    
    STATION_ACTIVE("STATION_ACTIVE"),
    
    STATION_NONEXISTENT("STATION_NONEXISTENT"),
    
    WRONG_NAME("WRONG_NAME"),
    
    WRONG_PHOTO("WRONG_PHOTO"),
    
    PHOTO_OUTDATED("PHOTO_OUTDATED"),
    
    OTHER("OTHER");

    private String value;

    ProblemReportTypeEnum(String value) {
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
    public static ProblemReportTypeEnum fromValue(String value) {
      for (ProblemReportTypeEnum b : ProblemReportTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("problemReportType")
  private ProblemReportTypeEnum problemReportType;

  @JsonProperty("isProcessed")
  private Boolean isProcessed;

  @JsonProperty("active")
  private Boolean active;

  public InboxEntryDto id(Long id) {
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

  public InboxEntryDto countryCode(String countryCode) {
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

  public InboxEntryDto stationId(String stationId) {
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

  public InboxEntryDto title(String title) {
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

  public InboxEntryDto lat(Double lat) {
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

  public InboxEntryDto lon(Double lon) {
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

  public InboxEntryDto photographerNickname(String photographerNickname) {
    this.photographerNickname = photographerNickname;
    return this;
  }

  /**
   * Get photographerNickname
   * @return photographerNickname
  */
  
  @Schema(name = "photographerNickname", required = false)
  public String getPhotographerNickname() {
    return photographerNickname;
  }

  public void setPhotographerNickname(String photographerNickname) {
    this.photographerNickname = photographerNickname;
  }

  public InboxEntryDto photographerEmail(String photographerEmail) {
    this.photographerEmail = photographerEmail;
    return this;
  }

  /**
   * Get photographerEmail
   * @return photographerEmail
  */
  
  @Schema(name = "photographerEmail", required = false)
  public String getPhotographerEmail() {
    return photographerEmail;
  }

  public void setPhotographerEmail(String photographerEmail) {
    this.photographerEmail = photographerEmail;
  }

  public InboxEntryDto comment(String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Get comment
   * @return comment
  */
  
  @Schema(name = "comment", required = false)
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public InboxEntryDto createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
  */
  
  @Schema(name = "createdAt", required = false)
  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public InboxEntryDto done(Boolean done) {
    this.done = done;
    return this;
  }

  /**
   * true if this photo was already imported or rejected
   * @return done
  */
  
  @Schema(name = "done", description = "true if this photo was already imported or rejected", required = false)
  public Boolean getDone() {
    return done;
  }

  public void setDone(Boolean done) {
    this.done = done;
  }

  public InboxEntryDto filename(String filename) {
    this.filename = filename;
    return this;
  }

  /**
   * name of the file in inbox
   * @return filename
  */
  
  @Schema(name = "filename", description = "name of the file in inbox", required = false)
  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public InboxEntryDto inboxUrl(String inboxUrl) {
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

  public InboxEntryDto hasPhoto(Boolean hasPhoto) {
    this.hasPhoto = hasPhoto;
    return this;
  }

  /**
   * this station has already a photo (conflict)
   * @return hasPhoto
  */
  
  @Schema(name = "hasPhoto", description = "this station has already a photo (conflict)", required = false)
  public Boolean getHasPhoto() {
    return hasPhoto;
  }

  public void setHasPhoto(Boolean hasPhoto) {
    this.hasPhoto = hasPhoto;
  }

  public InboxEntryDto hasConflict(Boolean hasConflict) {
    this.hasConflict = hasConflict;
    return this;
  }

  /**
   * conflict with another upload or existing photo
   * @return hasConflict
  */
  
  @Schema(name = "hasConflict", description = "conflict with another upload or existing photo", required = false)
  public Boolean getHasConflict() {
    return hasConflict;
  }

  public void setHasConflict(Boolean hasConflict) {
    this.hasConflict = hasConflict;
  }

  public InboxEntryDto problemReportType(ProblemReportTypeEnum problemReportType) {
    this.problemReportType = problemReportType;
    return this;
  }

  /**
   * Get problemReportType
   * @return problemReportType
  */
  
  @Schema(name = "problemReportType", required = false)
  public ProblemReportTypeEnum getProblemReportType() {
    return problemReportType;
  }

  public void setProblemReportType(ProblemReportTypeEnum problemReportType) {
    this.problemReportType = problemReportType;
  }

  public InboxEntryDto isProcessed(Boolean isProcessed) {
    this.isProcessed = isProcessed;
    return this;
  }

  /**
   * was this image process (e.g. pixelated)
   * @return isProcessed
  */
  
  @Schema(name = "isProcessed", description = "was this image process (e.g. pixelated)", required = false)
  public Boolean getIsProcessed() {
    return isProcessed;
  }

  public void setIsProcessed(Boolean isProcessed) {
    this.isProcessed = isProcessed;
  }

  public InboxEntryDto active(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * active flag provided by the user
   * @return active
  */
  
  @Schema(name = "active", description = "active flag provided by the user", required = false)
  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InboxEntryDto inboxEntry = (InboxEntryDto) o;
    return Objects.equals(this.id, inboxEntry.id) &&
        Objects.equals(this.countryCode, inboxEntry.countryCode) &&
        Objects.equals(this.stationId, inboxEntry.stationId) &&
        Objects.equals(this.title, inboxEntry.title) &&
        Objects.equals(this.lat, inboxEntry.lat) &&
        Objects.equals(this.lon, inboxEntry.lon) &&
        Objects.equals(this.photographerNickname, inboxEntry.photographerNickname) &&
        Objects.equals(this.photographerEmail, inboxEntry.photographerEmail) &&
        Objects.equals(this.comment, inboxEntry.comment) &&
        Objects.equals(this.createdAt, inboxEntry.createdAt) &&
        Objects.equals(this.done, inboxEntry.done) &&
        Objects.equals(this.filename, inboxEntry.filename) &&
        Objects.equals(this.inboxUrl, inboxEntry.inboxUrl) &&
        Objects.equals(this.hasPhoto, inboxEntry.hasPhoto) &&
        Objects.equals(this.hasConflict, inboxEntry.hasConflict) &&
        Objects.equals(this.problemReportType, inboxEntry.problemReportType) &&
        Objects.equals(this.isProcessed, inboxEntry.isProcessed) &&
        Objects.equals(this.active, inboxEntry.active);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, countryCode, stationId, title, lat, lon, photographerNickname, photographerEmail, comment, createdAt, done, filename, inboxUrl, hasPhoto, hasConflict, problemReportType, isProcessed, active);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InboxEntryDto {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
    sb.append("    stationId: ").append(toIndentedString(stationId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    lat: ").append(toIndentedString(lat)).append("\n");
    sb.append("    lon: ").append(toIndentedString(lon)).append("\n");
    sb.append("    photographerNickname: ").append(toIndentedString(photographerNickname)).append("\n");
    sb.append("    photographerEmail: ").append(toIndentedString(photographerEmail)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    done: ").append(toIndentedString(done)).append("\n");
    sb.append("    filename: ").append(toIndentedString(filename)).append("\n");
    sb.append("    inboxUrl: ").append(toIndentedString(inboxUrl)).append("\n");
    sb.append("    hasPhoto: ").append(toIndentedString(hasPhoto)).append("\n");
    sb.append("    hasConflict: ").append(toIndentedString(hasConflict)).append("\n");
    sb.append("    problemReportType: ").append(toIndentedString(problemReportType)).append("\n");
    sb.append("    isProcessed: ").append(toIndentedString(isProcessed)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
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

