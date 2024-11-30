package org.railwaystations.rsapi.adapter.web.auth

import com.fasterxml.jackson.annotation.JsonProperty

internal abstract class AuthUserMixin {
    @Suppress("unused")
    @JsonProperty("long")
    var value: AuthUser? = null
}
