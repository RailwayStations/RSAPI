package org.railwaystations.rsapi.adapter.in.web.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * OAuth2 token response
 */

@JsonTypeName("OAuthTokenResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-06T21:00:36.711673187+02:00[Europe/Berlin]")
public class OAuthTokenResponseDto {

    private String accessToken;

    private String refreshToken;

    private String scope;

    /**
     * Gets or Sets tokenType
     */
    public enum TokenTypeEnum {
        BEARER("Bearer");

        private String value;

        TokenTypeEnum(String value) {
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
        public static TokenTypeEnum fromValue(String value) {
            for (TokenTypeEnum b : TokenTypeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private TokenTypeEnum tokenType;

    private Long expiresIn;

    /**
     * Default constructor
     *
     * @deprecated Use {@link OAuthTokenResponseDto#OAuthTokenResponseDto(String, String, TokenTypeEnum)}
     */
    @Deprecated
    public OAuthTokenResponseDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public OAuthTokenResponseDto(String accessToken, String scope, TokenTypeEnum tokenType) {
        this.accessToken = accessToken;
        this.scope = scope;
        this.tokenType = tokenType;
    }

    public OAuthTokenResponseDto accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    /**
     * Get accessToken
     *
     * @return accessToken
     */
    @NotNull
    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public OAuthTokenResponseDto refreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    /**
     * Get refreshToken
     *
     * @return refreshToken
     */

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public OAuthTokenResponseDto scope(String scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Get scope
     *
     * @return scope
     */
    @NotNull
    @JsonProperty("scope")
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public OAuthTokenResponseDto tokenType(TokenTypeEnum tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    /**
     * Get tokenType
     *
     * @return tokenType
     */
    @NotNull
    @JsonProperty("token_type")
    public TokenTypeEnum getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenTypeEnum tokenType) {
        this.tokenType = tokenType;
    }

    public OAuthTokenResponseDto expiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    /**
     * Get expiresIn
     *
     * @return expiresIn
     */

    @JsonProperty("expires_in")
    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OAuthTokenResponseDto oauthTokenResponse = (OAuthTokenResponseDto) o;
        return Objects.equals(this.accessToken, oauthTokenResponse.accessToken) &&
                Objects.equals(this.refreshToken, oauthTokenResponse.refreshToken) &&
                Objects.equals(this.scope, oauthTokenResponse.scope) &&
                Objects.equals(this.tokenType, oauthTokenResponse.tokenType) &&
                Objects.equals(this.expiresIn, oauthTokenResponse.expiresIn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken, scope, tokenType, expiresIn);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OAuthTokenResponseDto {\n");
        sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
        sb.append("    refreshToken: ").append(toIndentedString(refreshToken)).append("\n");
        sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
        sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
        sb.append("    expiresIn: ").append(toIndentedString(expiresIn)).append("\n");
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

