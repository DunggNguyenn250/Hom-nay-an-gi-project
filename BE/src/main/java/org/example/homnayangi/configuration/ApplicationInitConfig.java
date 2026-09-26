package org.example.homnayangi.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.Role;
import org.example.homnayangi.enums.UserStatus;
import org.example.homnayangi.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                // Tạo Set kiểu Enum Role (không phải String)
                Set<Role> roles = new HashSet<>();
                roles.add(Role.ROLE_ADMIN); // hoặc Role.ADMIN tùy theo cách bạn định nghĩa Enum Role

                User user = User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin")) // Sửa lại thành passwordHash
                        .status(UserStatus.ACTIVE)
                        .roles(roles)
                        .build();

                userRepository.save(user);
                log.warn("Admin user has been created with default password: 'admin', please change it!");
            }
        };
    }
}
