package org.railwaystations.rsapi.app.auth

import jakarta.validation.constraints.NotNull
import org.apache.commons.lang3.StringUtils
import org.railwaystations.rsapi.core.model.User
import org.springframework.security.core.GrantedAuthority

class AuthUser(val user: @NotNull User, authorities: Collection<GrantedAuthority>) :
    org.springframework.security.core.userdetails.User(
        user.name, StringUtils.defaultString(user.key), authorities
    )
