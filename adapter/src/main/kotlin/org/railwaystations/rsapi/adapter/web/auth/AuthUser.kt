package org.railwaystations.rsapi.adapter.web.auth

import jakarta.validation.constraints.NotNull
import org.railwaystations.rsapi.core.model.User
import org.springframework.security.core.GrantedAuthority

class AuthUser(val user: @NotNull User, authorities: Collection<GrantedAuthority>) :
    org.springframework.security.core.userdetails.User(
        user.name, user.key ?: "", authorities
    )
