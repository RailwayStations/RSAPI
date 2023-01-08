package org.railwaystations.rsapi.app.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class RSAuthenticationFilter extends OncePerRequestFilter {

    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    private final AuthenticationManager authenticationManager;

    private RememberMeServices rememberMeServices = new NullRememberMeServices();

    private final BasicAuthenticationConverter authenticationConverter = new BasicAuthenticationConverter();

    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

    private final OAuth2AuthorizationService oAuth2AuthorizationService;

    public void setSecurityContextRepository(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

    public void setAuthenticationDetailsSource(
            AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
        this.authenticationConverter.setAuthenticationDetailsSource(authenticationDetailsSource);
    }

    public void setRememberMeServices(RememberMeServices rememberMeServices) {
        this.rememberMeServices = rememberMeServices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        var authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        UsernamePasswordAuthenticationToken authentication = null;
        if (StringUtils.isNotBlank(authorizationHeader)) {
            if (authorizationHeader.startsWith("Bearer ")) {
                authentication = getJwtAuthentication(authorizationHeader);
            } else if (authorizationHeader.startsWith("Basic ")) {
                authentication = authenticationConverter.convert(request);
            }
        }

        if (authentication == null) {
            var email = request.getHeader("Email");
            var uploadToken = request.getHeader("Upload-Token");
            if (StringUtils.isNotBlank(email) && StringUtils.isNotBlank(uploadToken)) {
                authentication = new UsernamePasswordAuthenticationToken(email, uploadToken);
                authentication.setDetails(authenticationConverter.getAuthenticationDetailsSource().buildDetails(request));
            }

        }

        if (authentication != null) {
            try {
                var authResult = this.authenticationManager.authenticate(authentication);
                var context = this.securityContextHolderStrategy.createEmptyContext();
                context.setAuthentication(authResult);
                this.securityContextHolderStrategy.setContext(context);
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug(LogMessage.format("Set SecurityContextHolder to %s", authResult));
                }
                this.rememberMeServices.loginSuccess(request, response, authResult);
                this.securityContextRepository.saveContext(context, request, response);
            } catch (AuthenticationException ex) {
                this.securityContextHolderStrategy.clearContext();
                this.logger.debug("Failed to process authentication request", ex);
                this.rememberMeServices.loginFail(request, response);
                chain.doFilter(request, response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getJwtAuthentication(String authorizationHeader) {
        try {
            var bearerToken = authorizationHeader.substring(7);
            var authorization = oAuth2AuthorizationService != null ? oAuth2AuthorizationService.findByToken(bearerToken, OAuth2TokenType.ACCESS_TOKEN) : null;
            var accessToken = authorization != null ? authorization.getAccessToken() : null;
            if (accessToken != null && accessToken.isActive()) {
                var user = authorization.getPrincipalName();
                return new UsernamePasswordAuthenticationToken(user, authorization);
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to parse JWT Bearer token", e);
        }
        return null;
    }

}
