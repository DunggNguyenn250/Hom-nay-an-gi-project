package org.example.homnayangi.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/users",
            "/api/v1/auth/token",
            "/api/v1/auth/introspect",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    };

    private final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private final String[] WS_ENDPOINTS = {
            "/ws/**"
    };

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");

        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5174"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
                        .requestMatchers(WS_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()

                        // THÊM ĐOẠN NÀY ĐỂ MỞ QUYỀN CHO PLACES (GET cho phép xem, POST/PUT/DELETE yêu cầu ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/places", "/api/v1/places/**").permitAll() // Hoặc .authenticated() nếu cần đăng nhập
                        .requestMatchers(HttpMethod.POST, "/api/v1/places/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/places/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/places/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags", "/api/v1/tags/{id}").authenticated()
                        .requestMatchers("/api/v1/tags/user/me/**").authenticated()
                        .requestMatchers("/api/v1/swipes/**").authenticated()
                        .requestMatchers("/api/v1/friendships/**").authenticated()
                        .requestMatchers("/api/v1/rooms/**").authenticated()
                        .requestMatchers("/api/v1/expenses/**").authenticated()
                        .requestMatchers("/api/v1/matchmaking/**").authenticated()

                        .requestMatchers("/api/v1/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
        );
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }


    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}
