package org.example.homnayangi.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/users",
            "/api/v1/auth/token",
            "/api/v1/auth/introspect",
            "/api/v1/rooms/**",
            "/api/v1/swipes/**"
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 1. Cấu hình phân quyền Request
                .authorizeHttpRequests(request ->
                                // Cho phép truy cập Swagger UI không cần login
                                request.requestMatchers(SWAGGER_ENDPOINTS).permitAll()

                                        // Cho phép truy cập công khai API public (POST)
                                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()

                                        // Các đường dẫn còn lại cho ADMIN
                                        .requestMatchers("/api/v1/**").hasRole("ADMIN")

                                        .anyRequest().authenticated()

                        // LƯU Ý: Nếu trong Token của bạn lưu dạng "ROLE_ADMIN" và
                        // jwtAuthenticationConverter có authorityPrefix là ""
                        // thì dùng .hasAuthority("ROLE_ADMIN") hoặc .hasRole("ADMIN") đều được.
                );

        // 2. Cấu hình xác thực JWT Resource Server
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
// Bắt lỗi 401 (Chưa đăng nhập / Token sai)
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        // Bắt lỗi 403 (Đã đăng nhập nhưng không đủ quyền Admin)
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
        );

        // 3. Tắt CSRF
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
