package org.railwaystations.rsapi.app.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RSAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private LazySodiumPasswordEncoder passwordEncoder;

    @Autowired
    private RSUserDetailsService userDetailsService;

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        var token = (UsernamePasswordAuthenticationToken) authentication;

        var user = userDetailsService.loadUserByUsername(String.valueOf(token.getPrincipal()));
        if (user == null) {
            log.info("User with email or name '{}' not found", token.getName());
            return null;
        }

        if (token.getCredentials() instanceof OAuth2Authorization oAuth2Authorization) {
            // try to verify jwt
            log.info("User verified by jwt '{}'", oAuth2Authorization.getPrincipalName());
            userDetailsService.updateEmailVerification(user.getUser());

            return new UsernamePasswordAuthenticationToken(user, token.getCredentials(), user.getAuthorities());
        } else if (token.getCredentials() instanceof String password) {
            // try to verify user defined password
            if (passwordEncoder.matches(password, user.getUser().getKey())) {
                log.info("User verified by password '{}'", user.getUsername());
                userDetailsService.updateEmailVerification(user.getUser());

                return new UsernamePasswordAuthenticationToken(user, token.getCredentials(), user.getAuthorities());
            }
        }

        log.info("Authentication by JWT, Password or UploadToken failed for user '{}'", token.getPrincipal());
        return null;
    }

}
