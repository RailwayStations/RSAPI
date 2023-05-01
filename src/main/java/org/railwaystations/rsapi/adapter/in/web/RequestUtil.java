package org.railwaystations.rsapi.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.railwaystations.rsapi.app.auth.AuthUser;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtil {
    public static AuthUser getAuthUser() {
        return (AuthUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static String getUserAgent() {
        return getRequest().getHeader(HttpHeaders.USER_AGENT);
    }

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }
}
