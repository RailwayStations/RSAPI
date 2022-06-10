package org.railwaystations.rsapi.app.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class RSAuthenticationFilter extends OncePerRequestFilter {

    private final BasicAuthenticationConverter authenticationConverter = new BasicAuthenticationConverter();

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        UsernamePasswordAuthenticationToken token = null;
        if (StringUtils.isNotBlank(authorization)) {
            token = authenticationConverter.convert(request);
        }

        if (token == null) {
            var email = request.getHeader("Email");
            var uploadToken = request.getHeader("Upload-Token");
            if (StringUtils.isNotBlank(email) && StringUtils.isNotBlank(uploadToken)) {
                token = new UsernamePasswordAuthenticationToken(email, uploadToken);
            }

        }

        if (token != null) {
            SecurityContextHolder.getContext().setAuthentication(token);
        }

        chain.doFilter(request, response);
    }

}
