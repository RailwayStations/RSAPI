package org.railwaystations.rsapi.app.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

abstract class AuthUserMixin {
    @SuppressWarnings("unused")
    @JsonProperty("long")
    AuthUser value;
}
