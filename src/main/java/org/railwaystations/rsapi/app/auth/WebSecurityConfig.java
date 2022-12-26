package org.railwaystations.rsapi.app.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Autowired
    private RSAuthenticationProvider authenticationProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    @Order(1)
    public SecurityFilterChain oauthFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http
                .securityMatcher("/oauth2/**")
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain loginFilterChain(HttpSecurity http)
            throws Exception {
        http
                .securityMatcher("/login**")
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                )
                // Form login handles the redirect to the login page from the
                // authorization server filter chain
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        var authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authenticationProvider);
        authenticationManagerBuilder.userDetailsService(userDetailsService);
        var authenticationManager = authenticationManagerBuilder.build();

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()
                .headers()
                .frameOptions().disable()
                .and()
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/**").permitAll()
                        .requestMatchers("/adminInbox", "/adminInboxCount", "/userInbox", "/photoUpload",
                                "/resendEmailVerification", "/reportProblem", "/changePassword", "/myProfile",
                                "/resendEmailVerification").authenticated())
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                .addFilterBefore(uploadTokenAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new BasicAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
                .authenticationManager(authenticationManager);

        return http.build();
    }

    public RSAuthenticationFilter uploadTokenAuthenticationFilter(AuthenticationManager authenticationManager) {
        return new RSAuthenticationFilter(authenticationManager);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        var registeredClient =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("testClientId")
                        .clientSecret("{noop}secret")
                        .clientAuthenticationMethod(
                                ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .redirectUri(
                                "http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
                        .redirectUri("http://127.0.0.1:8080/authorized")
                        .scope(OidcScopes.OPENID)
                        .scope(OidcScopes.PROFILE)
                        .scope("railwaystations")
                        .clientSettings(
                                ClientSettings.builder()
                                        .requireAuthorizationConsent(true).build())
                        .build();

        var registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);

        // Save registered client in db as if in-memory
        //registeredClientRepository.save(registeredClient);

        return registeredClientRepository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        var keyPair = generateRsaKey();
        var publicKey = (RSAPublicKey) keyPair.getPublic();
        var privateKey = (RSAPrivateKey) keyPair.getPrivate();
        var rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        var jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("https://railway-stations.org")
                .build();
    }

}
