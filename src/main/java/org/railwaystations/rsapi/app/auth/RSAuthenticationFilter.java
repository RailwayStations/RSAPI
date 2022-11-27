package org.railwaystations.rsapi.app.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class RSAuthenticationFilter extends OncePerRequestFilter {

    private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    private final AuthenticationManager authenticationManager;

    private RememberMeServices rememberMeServices = new NullRememberMeServices();

    private final BasicAuthenticationConverter authenticationConverter = new BasicAuthenticationConverter();

    private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

    public RSAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

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
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        UsernamePasswordAuthenticationToken authentication = null;
        if (StringUtils.isNotBlank(authorization)) {
            authentication = authenticationConverter.convert(request);
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

}
