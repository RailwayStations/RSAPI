package org.railwaystations.rsapi.app.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import jakarta.servlet.http.HttpServletRequest
import org.railwaystations.rsapi.utils.JwtUtil.generateRsaKey
import org.railwaystations.rsapi.utils.JwtUtil.loadRsaKey
import org.railwaystations.rsapi.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RegexRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfiguration


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class WebSecurityConfig(
    private val authenticationProvider: RSAuthenticationProvider,
    private val userDetailsService: UserDetailsService,
) {

    private val log by Logger()

    @Bean
    @Order(1)
    @Profile("!mockMvcTest")
    fun oauthFilterChain(http: HttpSecurity): SecurityFilterChain {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)
        http
            .securityMatcher("/oauth2/**")
            .cors { corsConfigurer ->
                corsConfigurer.configurationSource {
                    val cors = CorsConfiguration()
                    cors.setAllowedOriginPatterns(listOf("*"))
                    cors.allowedMethods = listOf("GET", "POST", "OPTIONS")
                    cors.allowedHeaders = listOf("*")
                    cors.allowCredentials = true
                    cors
                }
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        LoginUrlAuthenticationEntryPoint("/login")
                    )
            }
            .oauth2ResourceServer { serverConfigurer -> serverConfigurer.jwt(withDefaults()) }
            .getConfigurer(OAuth2AuthorizationServerConfigurer::class.java)
            .oidc(withDefaults())
            .authorizationEndpoint { authorizationEndpoint -> authorizationEndpoint.consentPage("/oauth2/consent") }

        return http.build()
    }

    @Bean
    @Order(2)
    @Profile("!mockMvcTest")
    fun loginFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/login**")
            authorizeHttpRequests {
                authorize("/login", permitAll)
                authorize("/loginResetPassword", permitAll)
                authorize("/loginRegister", permitAll)
                authorize(anyRequest, authenticated)
            } // Form login handles the redirect to the login page from the
            // authorization server filter chain
            formLogin {
                loginPage = "/login"
                permitAll()
            }
        }

        return http.build()
    }

    @Bean
    @Order(3)
    fun apiFilterChain(
        http: HttpSecurity,
        @Autowired(required = false) authorizationService: OAuth2AuthorizationService?
    ): SecurityFilterChain {
        val authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder::class.java)
            .authenticationProvider(authenticationProvider)
        authenticationManagerBuilder.userDetailsService(userDetailsService)
        val myAuthenticationManager = authenticationManagerBuilder.build()

        http {
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            csrf {
                requireCsrfProtectionMatcher = createCsrfRequestMatcher()
            }
            headers {
                frameOptions { disable() }
            }
            authorizeHttpRequests {
                authorize("/**", permitAll)
                authorize("/userInbox", authenticated)
                authorize("/photoUpload", authenticated)
                authorize("/photoUploadMultipartFormdata", authenticated)
                authorize("/resendEmailVerification", authenticated)
                authorize("/reportProblem", authenticated)
                authorize("/changePassword", authenticated)
                authorize("/myProfile", authenticated)
                authorize("/adminInbox", authenticated)
                authorize("/adminInboxCount", authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(
                uploadTokenAuthenticationFilter(myAuthenticationManager, authorizationService)
            )
            authenticationManager = myAuthenticationManager
        }

        return http.build()
    }

    private fun uploadTokenAuthenticationFilter(
        authenticationManager: AuthenticationManager,
        authorizationService: OAuth2AuthorizationService?
    ): RSAuthenticationFilter {
        return RSAuthenticationFilter(authenticationManager, authorizationService)
    }

    @Bean
    @Profile("!mockMvcTest")
    fun registeredClientRepository(jdbcTemplate: JdbcTemplate): RegisteredClientRepository {
        return JdbcRegisteredClientRepository(jdbcTemplate)
    }

    @Bean
    @Profile("!mockMvcTest")
    fun authorizationService(
        jdbcTemplate: JdbcTemplate,
        registeredClientRepository: RegisteredClientRepository
    ): OAuth2AuthorizationService {
        val service = JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository)
        val rowMapper = OAuth2AuthorizationRowMapper(registeredClientRepository)

        val objectMapper = ObjectMapper()
        val classLoader = JdbcOAuth2AuthorizationService::class.java.classLoader
        val securityModules = SecurityJackson2Modules.getModules(classLoader)
        objectMapper.registerModules(securityModules)
        objectMapper.registerModule(OAuth2AuthorizationServerJackson2Module())
        // You will need to write the Mixin for your class so Jackson can marshall it.
        objectMapper.addMixIn(AuthUser::class.java, AuthUserMixin::class.java)
        rowMapper.setObjectMapper(objectMapper)
        service.setAuthorizationRowMapper(rowMapper)
        return service
    }

    @Bean
    @Profile("!mockMvcTest")
    fun authorizationConsentService(
        jdbcTemplate: JdbcTemplate?,
        registeredClientRepository: RegisteredClientRepository?
    ): OAuth2AuthorizationConsentService {
        return JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository)
    }

    @Bean
    fun jwkSource(@Value("\${jwkSourceKeyFile}") jwkSourceKeyFile: String?): JWKSource<SecurityContext> {
        var rsaKey: RSAKey? = null
        if (jwkSourceKeyFile != null) {
            try {
                rsaKey = loadRsaKey(jwkSourceKeyFile)
            } catch (e: Exception) {
                log.error("Error loading jwkSourceKeyFile from $jwkSourceKeyFile", e)
            }
        }

        if (rsaKey == null) {
            log.warn("No jwkSourceKeyFile provided, generating new RSAKey for jwkSource")
            rsaKey = generateRsaKey()
        }
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet(jwkSet)
    }

    @Bean
    fun jwtDecoder(jwkSource: JWKSource<SecurityContext?>?): JwtDecoder {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)
    }

    @Bean
    @Profile("!mockMvcTest")
    fun authorizationServerSettings(): AuthorizationServerSettings {
        return AuthorizationServerSettings.builder()
            .issuer("https://railway-stations.org")
            .build()
    }

    private fun createCsrfRequestMatcher(): RequestMatcher {
        return object : RequestMatcher {
            private val requestMatcher = RegexRequestMatcher("/login*", null)

            override fun matches(request: HttpServletRequest): Boolean {
                return requestMatcher.matches(request)
            }
        }
    }
}
