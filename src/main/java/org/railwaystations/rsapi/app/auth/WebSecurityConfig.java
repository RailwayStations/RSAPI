package org.railwaystations.rsapi.app.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.railwaystations.rsapi.utils.JwtUtil.generateRsaKey;
import static org.railwaystations.rsapi.utils.JwtUtil.loadRsaKey;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class WebSecurityConfig {

    public static final String ISSUER = "https://railway-stations.org";

    private static final String CUSTOM_CONSENT_PAGE_URI = "/oauth2/consent";

    @Autowired
    private RSAuthenticationProvider authenticationProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    @Order(1)
    @Profile("!mockMvcTest")
    public SecurityFilterChain oauthFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http
                .securityMatcher("/oauth2/**")
                .cors()
                .configurationSource(request -> {
                    var cors = new CorsConfiguration();
                    cors.setAllowedOriginPatterns(List.of("*"));
                    cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
                    cors.setAllowedHeaders(List.of("*"));
                    cors.setAllowCredentials(true);
                    return cors;
                }).and()
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults())
                .authorizationEndpoint(authorizationEndpoint ->
                        authorizationEndpoint.consentPage(CUSTOM_CONSENT_PAGE_URI));

        return http.build();
    }

    @Bean
    @Order(2)
    @Profile("!mockMvcTest")
    public SecurityFilterChain loginFilterChain(HttpSecurity http)
            throws Exception {
        http
                .securityMatcher("/login**")
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/loginResetPassword").permitAll()
                        .requestMatchers("/loginRegister").permitAll()
                        .anyRequest().authenticated()
                )
                // Form login handles the redirect to the login page from the
                // authorization server filter chain
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, @Autowired(required = false) OAuth2AuthorizationService authorizationService) throws Exception {
        var authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authenticationProvider);
        authenticationManagerBuilder.userDetailsService(userDetailsService);
        var authenticationManager = authenticationManagerBuilder.build();

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .requireCsrfProtectionMatcher(createCsrfRequestMatcher())
                .and()

                .headers()
                .frameOptions().disable()
                .and()

                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/**").permitAll()
                        .requestMatchers("/adminInbox", "/adminInboxCount", "/userInbox",
                                "/photoUpload", "/photoUploadMultipartFormdata",
                                "/resendEmailVerification", "/reportProblem", "/changePassword", "/myProfile",
                                "/resendEmailVerification").authenticated())
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                .addFilterBefore(uploadTokenAuthenticationFilter(authenticationManager, authorizationService), UsernamePasswordAuthenticationFilter.class)
                .authenticationManager(authenticationManager);

        return http.build();
    }

    private static RequestMatcher createCsrfRequestMatcher() {
        return new RequestMatcher() {

            private final RegexRequestMatcher requestMatcher =
                    new RegexRequestMatcher("/login*", null);

            @Override
            public boolean matches(HttpServletRequest request) {
                return requestMatcher.matches(request);
            }

        };
    }

    public RSAuthenticationFilter uploadTokenAuthenticationFilter(AuthenticationManager authenticationManager, OAuth2AuthorizationService authorizationService) {
        return new RSAuthenticationFilter(authenticationManager, authorizationService);
    }

    @Bean
    @Profile("!mockMvcTest")
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    @Profile("!mockMvcTest")
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        var service = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        var rowMapper = new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(registeredClientRepository);

        var objectMapper = new ObjectMapper();
        var classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        var securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        // You will need to write the Mixin for your class so Jackson can marshall it.
        objectMapper.addMixIn(AuthUser.class, AuthUserMixin.class);
        rowMapper.setObjectMapper(objectMapper);
        service.setAuthorizationRowMapper(rowMapper);
        return service;
    }

    @Bean
    @Profile("!mockMvcTest")
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(@Value("${jwkSourceKeyFile}") String jwkSourceKeyFile) {
        RSAKey rsaKey = null;
        if (jwkSourceKeyFile != null) {
            try {
                rsaKey = loadRsaKey(jwkSourceKeyFile);
            } catch (Exception e) {
                log.error("Error loading jwkSourceKeyFile from " + jwkSourceKeyFile, e);
            }
        }

        if (rsaKey == null) {
            log.warn("No jwkSourceKeyFile provided, generating new RSAKey for jwkSource");
            rsaKey = generateRsaKey();
        }
        var jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    @Profile("!mockMvcTest")
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(ISSUER)
                .build();
    }

}
