package org.railwaystations.rsapi.app.auth

import com.fasterxml.jackson.annotation.JsonProperty

internal abstract class AuthUserMixin {
    @Suppress("unused")
    @JsonProperty("long")
    var value: AuthUser? = null
}
