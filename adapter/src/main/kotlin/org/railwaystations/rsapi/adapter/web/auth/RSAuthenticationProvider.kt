package org.railwaystations.rsapi.app.auth

import org.railwaystations.rsapi.core.utils.Logger
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.stereotype.Component

@Component
class RSAuthenticationProvider(
    private val passwordEncoder: LazySodiumPasswordEncoder,
    private val userDetailsService: RSUserDetailsService,
) : AuthenticationProvider {

    private val log by Logger()

    override fun supports(authentication: Class<*>?): Boolean {
        return (authentication?.let { UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(it) }) ?: false
    }

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication? {
        val token = authentication as UsernamePasswordAuthenticationToken

        val user = try {
            userDetailsService.loadUserByUsername(token.principal.toString())
        } catch (ex: UsernameNotFoundException) {
            null
        }
        if (user == null) {
            log.info("User with email or name '{}' not found", token.name)
            return null
        }

        if (token.credentials is OAuth2Authorization) {
            // try to verify jwt
            log.info("User verified by jwt '{}'", (token.credentials as OAuth2Authorization).principalName)
            userDetailsService.updateEmailVerification(user.user)

            return UsernamePasswordAuthenticationToken(user, token.credentials, user.authorities)
        } else if (token.credentials is String && user.user.key != null) {
            // try to verify user defined password
            if (passwordEncoder.matches(token.credentials as String, user.user.key!!)) {
                log.info("User verified by password '{}'", user.username)
                userDetailsService.updateEmailVerification(user.user)

                return UsernamePasswordAuthenticationToken(user, token.credentials, user.authorities)
            }
        }

        log.info(
            "Authentication by JWT, Password or UploadToken failed for user '{}'",
            token.principal
        )
        return null
    }
}
