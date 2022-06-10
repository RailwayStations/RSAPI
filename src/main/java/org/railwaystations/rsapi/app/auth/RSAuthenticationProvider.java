package org.railwaystations.rsapi.app.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
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

        // try to verify user defined password
        if (passwordEncoder.matches(token.getCredentials().toString(), user.getUser().getKey())) {
            log.info("User verified by password '{}'", user.getUsername());
            userDetailsService.updateEmailVerification(user.getUser());

            return new UsernamePasswordAuthenticationToken(user, token.getCredentials(), user.getAuthorities());
        }

        log.info("Password failed and UploadToken doesn't fit to user '{}'", token.getPrincipal());
        return null;
    }

}
