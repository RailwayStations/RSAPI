package org.railwaystations.rsapi.adapter.web

import jakarta.servlet.http.HttpServletRequest
import org.railwaystations.rsapi.adapter.web.auth.AuthUser
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class RequestUtil {
    val authUser: AuthUser
        get() = SecurityContextHolder.getContext().authentication.principal as AuthUser

    val userAgent: String
        get() = request.getHeader(HttpHeaders.USER_AGENT)

    val request: HttpServletRequest
        get() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
}
