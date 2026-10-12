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
            "/api/v1/auth/introspect"
    };

    private final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**"
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

    // =========================================================================
    // 1. CẤU HÌNH CORS CHO PHÉP FRONTEND REACT GỌI API
    // =========================================================================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Thêm các domain/port của Frontend cho phép truy cập:
        // - http://localhost:3000: Cổng mặc định của Create React App / Next.js
        // - http://localhost:5173: Cổng mặc định của Vite React
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));

        // Cho phép tất cả các HTTP Method cần thiết
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Cho phép tất cả các Headers (bao gồm Content-Type, Authorization, v.v.)
        configuration.setAllowedHeaders(List.of("*"));

        // Cho phép truyền Credential (Cookie, Bearer Token)
        configuration.setAllowCredentials(true);

        // Thời gian cache cấu hình CORS preflight request (3600 giây = 1 tiếng)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng cấu hình CORS cho tất cả các endpoint trong hệ thống
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // =========================================================================
    // 2. CẤU HÌNH SECURITY FILTER CHAIN
    // =========================================================================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // Bật CORS (Spring Security sẽ tự động nhặt Bean corsConfigurationSource ở trên)
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(request -> request
                        // 1. Swagger UI (Public)
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()

                        // 2. Public Endpoints (Chỉ cho phép POST đăng ký / đăng nhập)
                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()

                        // 3. User cá nhân
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()

                        // 4. Tags
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags", "/api/v1/tags/{id}").authenticated()
                        .requestMatchers("/api/v1/tags/user/me/**").authenticated()

                        // 5. Swipes
                        .requestMatchers("/api/v1/swipes/**").authenticated()

                        // 6. Friendships
                        .requestMatchers("/api/v1/friendships/**").authenticated()

                        // 7. Rooms
                        .requestMatchers("/api/v1/rooms/**").authenticated()

                        // 8. Expenses (Hóa đơn & Chia tiền)
                        .requestMatchers("/api/v1/expenses/**").authenticated()

                        // 9. Tất cả các endpoint /api/v1/** còn lại dành riêng cho ADMIN
                        .requestMatchers("/api/v1/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                );

        // Cấu hình OAuth2 Resource Server cho JWT
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
        );

        // Tắt CSRF vì ứng dụng sử dụng JWT Token (Stateless)
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }


    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Để chuỗi rỗng "" vì trong Token đã lưu sẵn "ROLE_ADMIN", "ROLE_USER"
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}
