package org.railwaystations.rsapi.adapter.web.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.log.LogMessage
import org.springframework.http.HttpHeaders
import org.springframework.lang.NonNull
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.web.authentication.NullRememberMeServices
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

class RSAuthenticationFilter(
    private val authenticationManager: AuthenticationManager,
    private val oAuth2AuthorizationService: OAuth2AuthorizationService?,
) : OncePerRequestFilter() {

    private var securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy()

    private var rememberMeServices = NullRememberMeServices()

    private val authenticationConverter = BasicAuthenticationConverter()

    private var securityContextRepository = RequestAttributeSecurityContextRepository()

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        @NonNull response: HttpServletResponse,
        @NonNull chain: FilterChain
    ) {
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        var authentication: UsernamePasswordAuthenticationToken? = null
        if (!authorizationHeader.isNullOrBlank()) {
            if (authorizationHeader.startsWith("Bearer ")) {
                authentication = getJwtAuthentication(authorizationHeader)
            } else if (authorizationHeader.startsWith("Basic ")) {
                authentication = authenticationConverter.convert(request)
            }
        }

        if (authentication == null) {
            val email = request.getHeader("Email")
            val uploadToken = request.getHeader("Upload-Token")
            if (!email.isNullOrBlank() && !uploadToken.isNullOrBlank()) {
                authentication = UsernamePasswordAuthenticationToken(email, uploadToken)
                authentication.details = authenticationConverter.authenticationDetailsSource.buildDetails(request)
            }
        }

        if (authentication != null) {
            try {
                val authResult =
                    authenticationManager.authenticate(authentication)
                val context = securityContextHolderStrategy.createEmptyContext()
                context.authentication = authResult
                securityContextHolderStrategy.context = context
                if (logger.isDebugEnabled) {
                    logger.debug(LogMessage.format("Set SecurityContextHolder to %s", authResult))
                }
                rememberMeServices.loginSuccess(request, response, authResult)
                securityContextRepository.saveContext(context, request, response)
            } catch (ex: AuthenticationException) {
                securityContextHolderStrategy.clearContext()
                logger.debug("Failed to process authentication request", ex)
                rememberMeServices.loginFail(request, response)
                chain.doFilter(request, response)
                return
            }
        }

        chain.doFilter(request, response)
    }

    private fun getJwtAuthentication(authorizationHeader: String): UsernamePasswordAuthenticationToken? {
        try {
            val bearerToken = authorizationHeader.substring(7)
            val authorization = oAuth2AuthorizationService?.findByToken(bearerToken, OAuth2TokenType.ACCESS_TOKEN)
            val accessToken = authorization?.accessToken
            if (accessToken != null && accessToken.isActive) {
                val user = authorization.principalName
                return UsernamePasswordAuthenticationToken(user, authorization)
            }
            return null
        } catch (e: Exception) {
            logger.error("Failed to parse JWT Bearer token", e)
        }
        return null
    }
}
