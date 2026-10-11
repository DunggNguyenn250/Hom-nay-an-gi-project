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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests(request -> request
                        // 1. Swagger UI (Public)
                        .requestMatchers(SWAGGER_ENDPOINTS).permitAll()

                        // 2. Public Endpoints (Chỉ cho phép POST đăng ký / đăng nhập)
                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()

                        // 3. BỔ SUNG: Cho phép User đã đăng nhập xem thông tin cá nhân của mình
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()

                        // 4. Tags: Cho phép User xem danh sách tag hệ thống
                        .requestMatchers(HttpMethod.GET, "/api/v1/tags", "/api/v1/tags/{id}").authenticated()

                        // 5. Tags: Cho phép User quản lý tag cá nhân (/me)
                        .requestMatchers("/api/v1/tags/user/me/**").authenticated()

                        // 6. Swipes: Cho phép User thực hiện quẹt, xem lịch sử và xem kết quả
                        .requestMatchers("/api/v1/swipes/**").authenticated()

                        // 7. Friendships: Cho phép User gửi/chấp nhận/hủy lời mời kết bạn & xem danh sách bạn bè
                        .requestMatchers("/api/v1/friendships/**").authenticated()

                        // 8. Rooms: Cho phép User tạo phòng, tham gia phòng, quản lý phòng
                        .requestMatchers("/api/v1/rooms/**").authenticated()

                        // 9. Tất cả các endpoint /api/v1/** còn lại (Admin quản lý danh sách user, tags, places, v.v.)
                        .requestMatchers("/api/v1/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                );

        // BỔ SUNG CẤU HÌNH JWT RESOURCE SERVER (ĐÃ BỊ THIẾU)
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        // Bắt lỗi 401 (Chưa đăng nhập / Token không hợp lệ)
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        // Bắt lỗi 403 (Đã đăng nhập nhưng không đủ quyền)
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
        );

        // Tắt CSRF
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
